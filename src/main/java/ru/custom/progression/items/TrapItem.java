package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.skills.SkillEventHooks;
import ru.custom.progression.storage.DataManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ловушка Следопыта — создаёт облако Slowness III + Weakness I.
 * Ноды: −% КД, +длительность, «Паутина» (Slowness 255 на 1 сек),
 * «Минное поле» (+4 мгн. урона на вход).
 */
public class TrapItem extends Item {

    private static final long BASE_COOLDOWN_MS = 45_000L;
    private static final Map<UUID, Long> lastUsed = new HashMap<>();

    public TrapItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        PlayerStats stats = DataManager.getPlayer(sp.getUUID());
        long cdReduce = stats == null ? 0 : SkillEventHooks.trapCooldownReductionMs(stats);
        int duration  = stats == null ? 100 : SkillEventHooks.trapDurationTicks(stats);
        boolean web   = stats != null && SkillEventHooks.trapHasWeb(stats);
        boolean mine  = stats != null && SkillEventHooks.trapHasMine(stats);

        long cooldown = Math.max(10_000L, BASE_COOLDOWN_MS - cdReduce);
        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed.getOrDefault(sp.getUUID(), 0L);

        if (elapsed < cooldown) {
            long remaining = (cooldown - elapsed) / 1000 + 1;
            sp.displayClientMessage(
                Component.literal("Ловушка перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        // «Минное поле»: мгновенный урон всем врагам в радиусе 2.5
        if (mine) {
            AABB box = new AABB(
                    sp.getX() - 2.5, sp.getY() - 1, sp.getZ() - 2.5,
                    sp.getX() + 2.5, sp.getY() + 2, sp.getZ() + 2.5
            );
            for (LivingEntity le : serverLevel.getEntitiesOfClass(LivingEntity.class, box)) {
                if (le == sp) continue;
                le.hurtServer(serverLevel, serverLevel.damageSources().magic(), 4.0f);
            }
        }

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                sp.getX(), sp.getY(), sp.getZ());
        cloud.setOwner(sp);
        cloud.setRadius(2.5f);
        cloud.setRadiusOnUse(-0.5f);
        cloud.setWaitTime(0);
        cloud.setDuration(duration);
        cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 2));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
        if (web) {
            // Полное обездвиживание на 20 тиков (1 сек) — высокий уровень замедления
            cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 6));
        }
        serverLevel.addFreshEntity(cloud);

        lastUsed.put(sp.getUUID(), now);
        sp.displayClientMessage(
            Component.literal("🪤 Ловушка расставлена!").withStyle(ChatFormatting.GREEN), false
        );
        return InteractionResult.SUCCESS;
    }
}
