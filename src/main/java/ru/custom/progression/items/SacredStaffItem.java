package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.skills.SkillEventHooks;
import ru.custom.progression.storage.DataManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Священный Посох — оружие Жреца T2. Разблокируется нодой {@code p_great_heal}.
 * Лечит 6 HP, КД 20 сек. Поддерживает те же ноды, что {@link HealingStaffItem}.
 */
public class SacredStaffItem extends Item {

    private static final long BASE_COOLDOWN_MS = 20_000L;
    private static final float BASE_HEAL = 6.0f;

    public SacredStaffItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        PlayerStats stats = DataManager.getPlayer(sp.getUUID());
        long cdReduce = stats == null ? 0 : SkillEventHooks.healingStaffCooldownReductionMs(stats);
        float mult    = stats == null ? 1.0f : SkillEventHooks.healingStaffMultiplier(stats);
        boolean great = stats != null && SkillEventHooks.staffHasGreatHeal(stats);
        boolean cleanse = stats != null && SkillEventHooks.staffHasCleanse(stats);
        boolean holyWrath = stats != null && SkillEventHooks.staffHasHolyWrath(stats);
        boolean martyrSac = stats != null && SkillEventHooks.staffHasMartyrSacrifice(stats);

        long cooldown = Math.max(2_000L, BASE_COOLDOWN_MS - cdReduce);
        long now = System.currentTimeMillis();
        long elapsed = now - SkillEventHooks.getItemLastUsed(this, sp.getUUID());

        if (elapsed < cooldown) {
            long remaining = (cooldown - elapsed) / 1000 + 1;
            sp.displayClientMessage(
                Component.literal("Священный посох перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        float heal = BASE_HEAL * mult;
        sp.heal(heal);

        if (great) {
            AABB box = sp.getBoundingBox().inflate(4.0);
            for (ServerPlayer ally : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
                if (ally == sp) continue;
                ally.heal(heal * 0.6f);
            }
        }

        if (cleanse) {
            List<net.minecraft.core.Holder<MobEffect>> toRemove = new ArrayList<>();
            for (MobEffectInstance eff : sp.getActiveEffects()) {
                if (!eff.getEffect().value().isBeneficial()) toRemove.add(eff.getEffect());
            }
            for (var h : toRemove) sp.removeEffect(h);
        }

        if (holyWrath) {
            AABB box = sp.getBoundingBox().inflate(6.0);
            LivingEntity nearest = null;
            double nearestDistSqr = Double.MAX_VALUE;
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                if (le == sp || le instanceof Player) continue;
                double d = le.distanceToSqr(sp);
                if (d < nearestDistSqr) { nearestDistSqr = d; nearest = le; }
            }
            if (nearest != null) {
                nearest.hurtServer(sl, sl.damageSources().magic(), 3.0f);
                nearest.addEffect(new MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 1));
            }
        }

        if (martyrSac) {
            AABB box = sp.getBoundingBox().inflate(3.0);
            boolean anyAlly = false;
            for (ServerPlayer ally : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
                if (ally == sp) continue;
                if (ally.getHealth() < ally.getMaxHealth()) {
                    ally.heal(3.0f);
                    anyAlly = true;
                }
            }
            if (anyAlly && sp.getHealth() > 1.5f) {
                sp.setHealth(sp.getHealth() - 1.0f);
            }
        }

        SkillEventHooks.registerItemCooldown(this, sp.getUUID(), now);
        sp.displayClientMessage(
            Component.literal("☩ Священный свет").withStyle(ChatFormatting.WHITE), false
        );
        return InteractionResult.SUCCESS;
    }
}
