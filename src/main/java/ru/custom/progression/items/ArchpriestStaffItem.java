package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Посох Архижреца — оружие Жреца T3. Разблокируется нодой {@code p_resurrection}.
 * Лечит 10 HP + Regen I 10 сек, КД 15 сек. Поддерживает те же ноды, что
 * {@link HealingStaffItem}.
 */
public class ArchpriestStaffItem extends Item {

    private static final long BASE_COOLDOWN_MS = 15_000L;
    private static final float BASE_HEAL = 10.0f;

    public ArchpriestStaffItem(Properties props) {
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
                Component.literal("Посох Архижреца перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        float heal = BASE_HEAL * mult;
        sp.heal(heal);
        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, true, true));

        if (great) {
            AABB box = sp.getBoundingBox().inflate(5.0);
            for (ServerPlayer ally : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
                if (ally == sp) continue;
                ally.heal(heal * 0.7f);
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, true));
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
            AABB box = sp.getBoundingBox().inflate(8.0);
            LivingEntity nearest = null;
            double nearestDistSqr = Double.MAX_VALUE;
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                if (le == sp || le instanceof Player) continue;
                double d = le.distanceToSqr(sp);
                if (d < nearestDistSqr) { nearestDistSqr = d; nearest = le; }
            }
            if (nearest != null) {
                nearest.hurtServer(sl, sl.damageSources().magic(), 5.0f);
                nearest.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2));
            }
        }

        if (martyrSac) {
            AABB box = sp.getBoundingBox().inflate(4.0);
            boolean anyAlly = false;
            for (ServerPlayer ally : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
                if (ally == sp) continue;
                if (ally.getHealth() < ally.getMaxHealth()) {
                    ally.heal(4.0f);
                    anyAlly = true;
                }
            }
            if (anyAlly && sp.getHealth() > 1.5f) {
                sp.setHealth(sp.getHealth() - 1.0f);
            }
        }

        SkillEventHooks.registerItemCooldown(this, sp.getUUID(), now);
        sp.displayClientMessage(
            Component.literal("☩ Длань архижреца").withStyle(ChatFormatting.GOLD), false
        );
        return InteractionResult.SUCCESS;
    }
}
