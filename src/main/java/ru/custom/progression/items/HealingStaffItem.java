package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
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
 * Посох Жреца — лечит 4 HP, КД 30 сек.
 * Ноды: +% лечения, −% КД, «Великое исцеление» (аура союзников), «Очищение».
 */
public class HealingStaffItem extends Item {

    private static final long BASE_COOLDOWN_MS = 30_000L;
    private static final float BASE_HEAL = 4.0f;

    public HealingStaffItem(Properties props) {
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

        long cooldown = Math.max(3_000L, BASE_COOLDOWN_MS - cdReduce);
        long now = System.currentTimeMillis();
        long elapsed = now - SkillEventHooks.getItemLastUsed(this, sp.getUUID());

        if (elapsed < cooldown) {
            long remaining = (cooldown - elapsed) / 1000 + 1;
            sp.displayClientMessage(
                Component.literal("Посох перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        float heal = BASE_HEAL * mult;
        sp.heal(heal);

        // «Великое исцеление»: союзники в радиусе 3 лечатся на 50%
        if (great) {
            AABB box = sp.getBoundingBox().inflate(3.0);
            for (ServerPlayer ally : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
                if (ally == sp) continue;
                ally.heal(heal * 0.5f);
            }
        }

        // «Очищение»: снимаем все негативные эффекты
        if (cleanse) {
            List<net.minecraft.core.Holder<MobEffect>> toRemove = new ArrayList<>();
            for (MobEffectInstance eff : sp.getActiveEffects()) {
                if (!eff.getEffect().value().isBeneficial()) toRemove.add(eff.getEffect());
            }
            for (var h : toRemove) sp.removeEffect(h);
        }

        SkillEventHooks.registerItemCooldown(this, sp.getUUID(), now);
        sp.displayClientMessage(
            Component.literal("✦ Исцеление").withStyle(ChatFormatting.GREEN), false
        );
        return InteractionResult.SUCCESS;
    }
}
