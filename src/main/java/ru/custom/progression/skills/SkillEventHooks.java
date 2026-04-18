package ru.custom.progression.skills;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.storage.DataManager;

import java.util.HashMap;
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

    /** Игрок → tick-времени, когда последний раз сработала Жажда крови/Несокрушимый/Вне времени/Воскрешение (КД). */
    private static final Map<UUID, Long> lastIndestructibleTick = new HashMap<>();
    private static final Map<UUID, Long> lastTimelessTick = new HashMap<>();
    private static final Map<UUID, Long> lastResurrectionTick = new HashMap<>();

    /** Игрок → UUID последней цели, которую он ударил (для Охотника — первый удар по новой цели). */
    private static final Map<UUID, UUID> lastHitTarget = new HashMap<>();

    public static void register() {
        registerDamageBoosts();
        registerDeathSavers();
    }

    // ── Модификация исходящего урона (крит / Берсерк / Охотник) ──────────
    //
    // Подход: слушаем AFTER_DAMAGE у цели. Если атакующий — игрок с активными
    // нодами, наносим дополнительный урон через target.hurt() с флагом
    // REENTRY=true для защиты от рекурсии. Базовый урон мы НЕ меняем, поэтому
    // удар игрока выглядит обычным; дополнительный урон приходит отдельно
    // (так выглядит в PoE — как отдельный крит-эффект).

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
                bonus += damageTaken; // удвоение
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

            if (bonus <= 0f) return;

            // Наносим дополнительный урон с защитой от рекурсии
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

            // Несокрушимый: 1 раз в 5 минут (6000 тиков)
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

            // Вне времени: 1 раз в 10 минут (12000 тиков)
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

            // Воскрешение: 1 раз в 15 минут (18000 тиков), восстанавливает 20% HP
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

            return true;
        });
    }

    private static boolean tickCooldownOk(Map<UUID, Long> map, UUID id, long now, long cdTicks) {
        Long last = map.get(id);
        return last == null || (now - last) >= cdTicks;
    }

    private static void saveFromDeath(ServerPlayer player, float restoreHp) {
        player.setHealth(Math.max(1.0f, restoreHp));
    }

    // ── Утилиты, используемые из ProgressionMod ──────────────────────────

    /** Множитель XP от нод Мага (m_xp1 +5%, m_xp2 +5%). */
    public static double xpMultiplierFromNodes(PlayerStats stats) {
        double mult = 1.0;
        if (stats.isNodeUnlocked("m_xp1")) mult *= 1.05;
        if (stats.isNodeUnlocked("m_xp2")) mult *= 1.05;
        return mult;
    }

    /** Бонус к регенерации Жреца от ноды p_regen (+50%). */
    public static float priestRegenBonus(PlayerStats stats, float base) {
        return stats.isNodeUnlocked("p_regen") ? base * 1.5f : base;
    }
}
