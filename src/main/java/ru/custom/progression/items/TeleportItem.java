package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.custom.progression.skills.SkillEventHooks;

/**
 * Телепорт Мага — переносит игрока к точке прицела (до 32 блоков).
 * Разблокируется нодой {@code m_teleport}. КД 30 сек.
 */
public class TeleportItem extends Item {

    private static final long COOLDOWN_MS = 30_000L;
    private static final double MAX_RANGE = 32.0;

    public TeleportItem(Properties props) {
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
                    Component.literal("Телепорт перезаряжается... ещё " + remaining + " сек.")
                            .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        Vec3 eye = sp.getEyePosition();
        Vec3 view = sp.getViewVector(1.0f);
        Vec3 target = eye.add(view.scale(MAX_RANGE));

        HitResult hit = sl.clip(new ClipContext(eye, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 dst = hit.getLocation();
        // Уменьшаем немного, чтобы не оказаться внутри блока
        if (hit.getType() == HitResult.Type.BLOCK) {
            dst = dst.subtract(view.scale(0.5));
        }

        sp.teleportTo(dst.x, dst.y, dst.z);
        sp.fallDistance = 0f;
        SkillEventHooks.registerItemCooldown(this, sp.getUUID(), now);

        sp.displayClientMessage(
                Component.literal("✦ Телепорт").withStyle(ChatFormatting.AQUA), false
        );
        return InteractionResult.SUCCESS;
    }
}
