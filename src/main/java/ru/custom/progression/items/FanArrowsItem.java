package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.custom.progression.skills.SkillEventHooks;

/**
 * Веер стрел Следопыта — выпускает 5 стрел веером с 60% базового урона.
 * Разблокируется нодой {@code r_fan_arrows}. КД 25 сек.
 */
public class FanArrowsItem extends Item {

    private static final long COOLDOWN_MS = 25_000L;

    public FanArrowsItem(Properties props) {
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
                    Component.literal("Веер перезаряжается... ещё " + remaining + " сек.")
                            .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        Vec3 eye = sp.getEyePosition();
        Vec3 view = sp.getViewVector(1.0f);
        // 5 стрел с разбросом ±30° в горизонтальной плоскости
        double[] yawOffsets = { -0.5, -0.25, 0.0, 0.25, 0.5 };
        for (double yawOff : yawOffsets) {
            double cos = Math.cos(yawOff);
            double sin = Math.sin(yawOff);
            Vec3 dir = new Vec3(
                    view.x * cos - view.z * sin,
                    view.y,
                    view.x * sin + view.z * cos
            ).normalize();

            Arrow arrow = new Arrow(sl, sp, new net.minecraft.world.item.ItemStack(
                    net.minecraft.world.item.Items.ARROW), null);
            arrow.setPos(eye.x, eye.y - 0.1, eye.z);
            arrow.setBaseDamage(1.2);
            arrow.shoot(dir.x, dir.y, dir.z, 2.5f, 1.0f);
            sl.addFreshEntity(arrow);
        }

        SkillEventHooks.registerItemCooldown(this, sp.getUUID(), now);
        sp.displayClientMessage(
                Component.literal("🏹 Веер стрел!").withStyle(ChatFormatting.GREEN), false
        );
        return InteractionResult.SUCCESS;
    }
}
