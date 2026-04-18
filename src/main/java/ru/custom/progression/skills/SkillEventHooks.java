package ru.custom.progression.skills;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.storage.DataManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Event-хуки, реализующие поведение нод, которые нельзя выразить одним
 * {@link net.minecraft.world.entity.ai.attributes.AttributeModifier}.
 * <p>
 * Регистрируется из {@link ru.custom.progression.ProgressionMod#onInitialize()}.
 */
public final class SkillEventHooks {

    private SkillEventHooks() {}

    private static final Random RNG = new Random();

    /** Защита от рекурсии при повторной атаке через {@code entity.hurt()}. */
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> false);

    // ── Кулдауны death-saver'ов и прочих разовых эффектов ────────────────
    private static final Map<UUID, Long> lastIndestructibleTick = new HashMap<>();
    private static final Map<UUID, Long> lastTimelessTick       = new HashMap<>();
    private static final Map<UUID, Long> lastResurrectionTick   = new HashMap<>();
    private static final Map<UUID, Long> lastBlinkTick          = new HashMap<>();
    private static final Map<UUID, Long> lastFateTick           = new HashMap<>();

    /** Игрок → UUID последней цели, которую он ударил (для Охотника — первый удар по новой цели). */
    private static final Map<UUID, UUID> lastHitTarget = new HashMap<>();

    /** Игрок → позиция/тик последнего движения (Охотник в тени). */
    private static final Map<UUID, Vec3>  lastPosition   = new HashMap<>();
    private static final Map<UUID, Long>  lastMoveTick   = new HashMap<>();

    /** Идентификатор временного бонуса брони от «Бастион». */
    private static final Identifier BASTION_ID =
            Identifier.fromNamespaceAndPath("progression", "skill_w_guard_bastion_tick");

    public static void register() {
        registerDamageBoosts();
        registerDeathSavers();
        registerBastionTick();
        registerGoldenHands();
        registerDodge();
        registerImmunities();
        registerFateAndFaithShield();
        registerMovementTracker();
    }

    // ── Модификация исходящего урона (крит / Берсерк / Охотник / Охотник в тени) ──────────

    private static void registerDamageBoosts() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (REENTRY.get()) return;
            if (blocked || damageTaken <= 0) return;
            if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
            if (attacker == entity) return;

            PlayerStats stats = DataManager.getPlayer(attacker.getUUID());
            if (stats == null) return;

            float bonus = 0f;

            // +5% крит шанс — двойной урон с вероятностью 5%
            if (stats.isNodeUnlocked("w_fury_crit") && RNG.nextFloat() < 0.05f) {
                bonus += damageTaken;
                attacker.sendSystemMessage(
                        Component.literal("✦ Критический удар!").withStyle(ChatFormatting.YELLOW),
                        true
                );
            }

            // Берсерк: +20% урона при HP < 50%
            if (stats.isNodeUnlocked("w_fury_berserk")
                    && attacker.getHealth() < attacker.getMaxHealth() * 0.5f) {
                bonus += damageTaken * 0.20f;
            }

            // Охотник Следопыта: первый удар по новой цели — +50% урона
            if (stats.isNodeUnlocked("r_hunter")) {
                UUID targetId = entity.getUUID();
                UUID prev = lastHitTarget.get(attacker.getUUID());
                if (prev == null || !prev.equals(targetId)) {
                    bonus += damageTaken * 0.50f;
                }
                lastHitTarget.put(attacker.getUUID(), targetId);
            }

            // Охотник в тени: первый удар после 3 сек неподвижности — ×3 урона
            if (stats.isNodeUnlocked("r_shadow")) {
                long now = attacker.level().getGameTime();
                Long lastMove = lastMoveTick.get(attacker.getUUID());
                if (lastMove != null && now - lastMove >= 60L) {
                    bonus += damageTaken * 2.0f; // ×3 итого = +200% сверху
                    attacker.sendSystemMessage(
                            Component.literal("☾ Охотник в тени: ×3 урона!")
                                    .withStyle(ChatFormatting.DARK_PURPLE),
                            true
                    );
                    // Сбиваем — нужен заново период неподвижности
                    lastMoveTick.put(attacker.getUUID(), now);
                }
            }

            if (bonus <= 0f) return;

            REENTRY.set(true);
            try {
                if (attacker.level() instanceof ServerLevel sl) {
                    entity.hurtServer(sl, source, bonus);
                }
            } finally {
                REENTRY.set(false);
            }
        });

        // Жажда крови: при убийстве восстанавливает 15% макс HP
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed, source) -> {
            if (!(killer instanceof ServerPlayer player)) return;
            PlayerStats stats = DataManager.getPlayer(player.getUUID());
            if (stats == null) return;
            if (stats.isNodeUnlocked("w_fury_bloodthirst")) {
                float heal = player.getMaxHealth() * 0.15f;
                if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(heal);
                }
            }
        });
    }

    // ── Защита от смерти (Несокрушимый / Вне времени / Воскрешение) ──────

    private static void registerDeathSavers() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) return true;
            PlayerStats stats = DataManager.getPlayer(player.getUUID());
            if (stats == null) return true;

            long now = player.level().getGameTime();

            // Несокрушимый Воина: 1 раз в 5 минут (6000 тиков)
            if (stats.isNodeUnlocked("w_guard_indestructible")
                    && tickCooldownOk(lastIndestructibleTick, player.getUUID(), now, 6000L)) {
                saveFromDeath(player, 1.0f);
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 1));
                lastIndestructibleTick.put(player.getUUID(), now);
                player.sendSystemMessage(
                        Component.literal("⛨ Несокрушимый: вы пережили удар!")
                                .withStyle(ChatFormatting.GOLD)
                );
                return false;
            }

            // Вне времени Мага: 1 раз в 10 минут (12000 тиков)
            if (stats.isNodeUnlocked("m_timeless")
                    && tickCooldownOk(lastTimelessTick, player.getUUID(), now, 12000L)) {
                saveFromDeath(player, player.getMaxHealth() * 0.3f);
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 3));
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
                lastTimelessTick.put(player.getUUID(), now);
                player.sendSystemMessage(
                        Component.literal("✦ Вне времени: время замерло...")
                                .withStyle(ChatFormatting.AQUA)
                );
                return false;
            }

            // Воскрешение Жреца: 1 раз в 15 минут (18000 тиков), 20% HP
            if (stats.isNodeUnlocked("p_resurrection")
                    && tickCooldownOk(lastResurrectionTick, player.getUUID(), now, 18000L)) {
                saveFromDeath(player, player.getMaxHealth() * 0.20f);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                lastResurrectionTick.put(player.getUUID(), now);
                player.sendSystemMessage(
                        Component.literal("☩ Воскрешение: свет хранит вас...")
                                .withStyle(ChatFormatting.WHITE)
                );
                return false;
            }

            // Неуловимый Следопыта: не спасает от смерти, обрабатывается в registerImmunities
            return true;
        });
    }

    // ── Бастион: +50% ARMOR при стоянии на месте ─────────────────────────

    private static void registerBastionTick() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 != 0) return; // раз в 0.5 сек
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerStats stats = DataManager.getPlayer(player.getUUID());
                if (stats == null) continue;

                AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
                if (armor == null) continue;

                boolean shouldBuff = stats.isNodeUnlocked("w_guard_bastion")
                        && isStandingStill(player);

                boolean hasBuff = armor.getModifier(BASTION_ID) != null;

                if (shouldBuff && !hasBuff) {
                    armor.addPermanentModifier(new AttributeModifier(
                            BASTION_ID, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                } else if (!shouldBuff && hasBuff) {
                    armor.removeModifier(BASTION_ID);
                }
            }
        });
    }

    private static boolean isStandingStill(ServerPlayer p) {
        Vec3 v = p.getDeltaMovement();
        return v.lengthSqr() < 0.001;
    }

    // ── Золотые руки: 20% шанс двойного дропа ────────────────────────────

    private static void registerGoldenHands() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer sp)) return;
            if (!(level instanceof ServerLevel sl)) return;
            PlayerStats stats = DataManager.getPlayer(sp.getUUID());
            if (stats == null || !stats.isNodeUnlocked("m_golden_hands")) return;
            if (RNG.nextFloat() >= 0.20f) return;

            // Дублируем дроп: рассчитываем vanilla-drop и спавним рядом
            List<ItemStack> drops = Block.getDrops(state, sl, pos, blockEntity, sp,
                    sp.getMainHandItem());
            for (ItemStack s : drops) {
                if (!s.isEmpty()) {
                    Block.popResource(sl, pos, s.copy());
                }
            }
        });
    }

    // ── Уклонение Следопыта + Неуловимый + Щит Веры ──────────────────────

    private static void registerDodge() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) return true;
            PlayerStats stats = DataManager.getPlayer(player.getUUID());
            if (stats == null) return true;

            // Уклонение: 5% шанс избежать урона
            if (stats.isNodeUnlocked("r_dodge") && RNG.nextFloat() < 0.05f) {
                player.sendSystemMessage(
                        Component.literal("⟳ Уклонение!").withStyle(ChatFormatting.GREEN),
                        true
                );
                return false;
            }

            // Призрак ветра: при спринте снаряды не попадают
            if (stats.isNodeUnlocked("r_wind") && player.isSprinting()
                    && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
                return false;
            }

            return true;
        });

        // Неуловимый: при получении урона — 1 сек невидимости
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            PlayerStats stats = DataManager.getPlayer(player.getUUID());
            if (stats == null) return;
            if (stats.isNodeUnlocked("r_elusive") && damageTaken > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false));
            }
        });
    }

    // ── Иммунитеты Жреца (яд/огонь/утопление/Wither) ──────────────────────

    private static void registerImmunities() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return; // раз в секунду
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerStats stats = DataManager.getPlayer(player.getUUID());
                if (stats == null) continue;

                // Иммунитет к яду
                if (stats.isNodeUnlocked("p_poison_imm") && player.hasEffect(MobEffects.POISON)) {
                    player.removeEffect(MobEffects.POISON);
                }
                // Жажда крови: отключаем естественную регенерацию (понижаем сытость до 17,
                // при которой vanilla-регенерация не запускается)
                if (stats.isNodeUnlocked("w_fury_bloodthirst") && player.getFoodData().getFoodLevel() > 17) {
                    player.getFoodData().setFoodLevel(17);
                }
                // Непоколебимый Жреца: огонь, Wither, утопление
                if (stats.isNodeUnlocked("p_stalwart")) {
                    if (player.isOnFire()) player.clearFire();
                    if (player.hasEffect(MobEffects.WITHER)) player.removeEffect(MobEffects.WITHER);
                    if (player.getAirSupply() < player.getMaxAirSupply()) {
                        player.setAirSupply(player.getMaxAirSupply());
                    }
                }
            }
        });
    }

    // ── Рука судьбы + Щит Веры ───────────────────────────────────────────

    private static void registerFateAndFaithShield() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 40 != 0) return; // раз в 2 секунды
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerStats stats = DataManager.getPlayer(player.getUUID());
                if (stats == null) continue;

                long now = player.level().getGameTime();

                // Рука судьбы: раз в час — Luck IV на 5 минут
                if (stats.isNodeUnlocked("m_fate")
                        && tickCooldownOk(lastFateTick, player.getUUID(), now, 72000L)) {
                    player.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 3));
                    lastFateTick.put(player.getUUID(), now);
                    player.sendSystemMessage(
                            Component.literal("✦ Рука Судьбы: удача на вашей стороне!")
                                    .withStyle(ChatFormatting.GOLD)
                    );
                }

                // Щит Веры: при HP < 30% — Absorption IV (если ещё нет)
                if (stats.isNodeUnlocked("p_faith_shield")
                        && player.getHealth() < player.getMaxHealth() * 0.30f
                        && !player.hasEffect(MobEffects.ABSORPTION)) {
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 3));
                    player.sendSystemMessage(
                            Component.literal("☩ Щит Веры защищает вас")
                                    .withStyle(ChatFormatting.WHITE),
                            true
                    );
                }

                // Посвящённый Жреца: аура +1 HP / 10с союзникам в радиусе 15
                if (stats.isNodeUnlocked("p_devoted") && server.getTickCount() % 200 == 0) {
                    AABB box = player.getBoundingBox().inflate(15.0);
                    for (ServerPlayer ally : player.level().getEntitiesOfClass(ServerPlayer.class, box)) {
                        if (ally == player) continue;
                        if (ally.getHealth() < ally.getMaxHealth()) ally.heal(1.0f);
                    }
                }
            }
        });
    }

    // ── Трекер движения (для «Охотника в тени» и Мерцания) ────────────────

    private static void registerMovementTracker() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Vec3 prev = lastPosition.get(player.getUUID());
                Vec3 cur  = player.position();
                long now  = player.level().getGameTime();
                if (prev == null || prev.distanceToSqr(cur) > 0.01) {
                    lastPosition.put(player.getUUID(), cur);
                    lastMoveTick.put(player.getUUID(), now);
                }
            }
        });
    }

    // ── Вспомогательные ───────────────────────────────────────────────────

    private static boolean tickCooldownOk(Map<UUID, Long> map, UUID id, long now, long cdTicks) {
        Long last = map.get(id);
        return last == null || (now - last) >= cdTicks;
    }

    private static void saveFromDeath(ServerPlayer player, float restoreHp) {
        player.setHealth(Math.max(1.0f, restoreHp));
    }

    // ── API, используемое из ProgressionMod / items ───────────────────────

    /** Множитель XP от нод Мага («Мудрость»). */
    public static double xpMultiplierFromNodes(PlayerStats stats) {
        double mult = 1.0;
        if (stats.isNodeUnlocked("m_xp1")) mult *= 1.05;
        if (stats.isNodeUnlocked("m_xp2")) mult *= 1.05;
        if (stats.isNodeUnlocked("m_xp3")) mult *= 1.15;
        if (stats.isNodeUnlocked("m_xp4")) mult *= 1.10;
        return mult;
    }

    /** Бонус к регенерации Жреца (+50% от ноды {@code p_regen}, +1 HP от {@code p_regen2}). */
    public static float priestRegenBonus(PlayerStats stats, float base) {
        float h = stats.isNodeUnlocked("p_regen") ? base * 1.5f : base;
        if (stats.isNodeUnlocked("p_regen2")) h += 1.0f;
        return h;
    }

    /** Множитель длительности Боевого клича (от нод Воина). */
    public static int warCryDurationBonusTicks(PlayerStats stats) {
        return stats.isNodeUnlocked("w_cmd_dur") ? 60 : 0; // +3 сек
    }

    /** Снижение КД Боевого клича (в мс) — от двух нод. */
    public static long warCryCooldownReductionMs(PlayerStats stats) {
        long r = 0;
        if (stats.isNodeUnlocked("w_cmd_cd1")) r += 3000L; // 5% от 60с
        if (stats.isNodeUnlocked("w_cmd_cd2")) r += 3000L;
        return r;
    }

    /** Боевой клич Воина также даёт Haste I (нода «Военный клич»). */
    public static boolean hasWarCryHaste(PlayerStats stats) {
        return stats.isNodeUnlocked("w_cmd_warcry");
    }

    /** Снижение КД Свитка Удачи (мс). */
    public static long luckScrollCooldownReductionMs(PlayerStats stats) {
        long r = 0;
        if (stats.isNodeUnlocked("m_spd_cd1")) r += 4500L; // 5% от 90с
        if (stats.isNodeUnlocked("m_spd_cd2")) r += 4500L;
        return r;
    }

    /** Снижение КД Ловушки (мс). */
    public static long trapCooldownReductionMs(PlayerStats stats) {
        long r = 0;
        if (stats.isNodeUnlocked("r_trap_cd1")) r += 2250L; // 5% от 45с
        if (stats.isNodeUnlocked("r_trap_cd2")) r += 2250L;
        return r;
    }

    /** Длительность облака ловушки (тики). База 100 + 40 от ноды. */
    public static int trapDurationTicks(PlayerStats stats) {
        return 100 + (stats.isNodeUnlocked("r_trap_dur") ? 40 : 0);
    }

    /** Ловушка также обездвиживает (Slowness 255) на 20 тиков — нода «Паутина». */
    public static boolean trapHasWeb(PlayerStats stats) {
        return stats.isNodeUnlocked("r_trap_web");
    }

    /** Ловушка наносит 4 HP мгновенного урона — «Минное поле». */
    public static boolean trapHasMine(PlayerStats stats) {
        return stats.isNodeUnlocked("r_trap_mine");
    }

    /** Множитель силы лечения посоха (ноды «+10% лечения»). */
    public static float healingStaffMultiplier(PlayerStats stats) {
        float m = 1.0f;
        if (stats.isNodeUnlocked("p_heal1")) m += 0.10f;
        if (stats.isNodeUnlocked("p_heal2")) m += 0.10f;
        return m;
    }

    /** Снижение КД Посоха (мс). */
    public static long healingStaffCooldownReductionMs(PlayerStats stats) {
        long r = 0;
        if (stats.isNodeUnlocked("p_staff_cd1")) r += 1500L; // 5% от 30с
        if (stats.isNodeUnlocked("p_staff_cd2")) r += 1500L;
        return r;
    }

    /** Посох также лечит союзников в радиусе 3 на 50% — «Великое исцеление». */
    public static boolean staffHasGreatHeal(PlayerStats stats) {
        return stats.isNodeUnlocked("p_great_heal");
    }

    /** Посох снимает все дебаффы с игрока — «Очищение». */
    public static boolean staffHasCleanse(PlayerStats stats) {
        return stats.isNodeUnlocked("p_cleanse");
    }

    /** Проверка ноды «Всезнание» — видимость HP мобов (клиентский флаг). */
    public static boolean hasOmniscience(PlayerStats stats) {
        return stats.isNodeUnlocked("m_omniscience");
    }

    /** КД мерцания (в тиках), если нода активна; иначе -1. */
    public static long blinkCooldownTicks() { return 1800L; }

    /** Время последнего мерцания. */
    public static boolean blinkReady(UUID id, long now) {
        return tickCooldownOk(lastBlinkTick, id, now, blinkCooldownTicks());
    }

    public static void markBlinkUsed(UUID id, long now) {
        lastBlinkTick.put(id, now);
    }
}
