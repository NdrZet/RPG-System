package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import ru.custom.progression.skills.SkillEventHooks;

/**
 * Дымовая Завеса Следопыта — Blindness II врагам в радиусе 5 блоков на 5 сек.
 * Разблокируется нодой {@code r_trap_smoke}. КД 60 сек.
 */
public class SmokeCloudItem extends Item {

    private static final long COOLDOWN_MS = 60_000L;

    public SmokeCloudItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        long now = System.currentTimeMillis();
        long elapsed = now - SkillEventHooks.getItemLastUsed(this, sp.getUUID());
        if (elapsed < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
            sp.displayClientMessage(
                    Component.literal("Завеса перезаряжается... ещё " + remaining + " сек.")
                            .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        AABB box = sp.getBoundingBox().inflate(5.0);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == sp) continue;
            if (le instanceof Player) continue;
            le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 1));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }

        SkillEventHooks.registerItemCooldown(this, sp.getUUID(), now);
        sp.displayClientMessage(
                Component.literal("☁ Дымовая завеса").withStyle(ChatFormatting.GRAY), false
        );
        return InteractionResult.SUCCESS;
    }
}
