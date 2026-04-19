package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Прыжок Воина — подбрасывает игрока вперёд и вверх, наносит 4 HP урона
 * всем врагам в радиусе 3 при приземлении (оформлено как AoE в момент активации).
 * Разблокируется нодой {@code w_dom_leap}. КД 15 сек.
 */
public class LeapItem extends Item {

    private static final long COOLDOWN_MS = 15_000L;
    private static final Map<UUID, Long> lastUsed = new HashMap<>();

    public LeapItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed.getOrDefault(sp.getUUID(), 0L);
        if (elapsed < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
            sp.displayClientMessage(
                    Component.literal("Прыжок перезаряжается... ещё " + remaining + " сек.")
                            .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        Vec3 view = sp.getViewVector(1.0f);
        sp.setDeltaMovement(view.x * 1.4, 0.7, view.z * 1.4);
        sp.hurtMarked = true;
        sp.fallDistance = 0f;

        AABB box = new AABB(
                sp.getX() - 3.0, sp.getY() - 1, sp.getZ() - 3.0,
                sp.getX() + 3.0, sp.getY() + 2, sp.getZ() + 3.0
        );
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == sp) continue;
            le.hurtServer(sl, sl.damageSources().playerAttack(sp), 4.0f);
        }

        lastUsed.put(sp.getUUID(), now);
        sp.displayClientMessage(
                Component.literal("⚔ Прыжок Воина!").withStyle(ChatFormatting.GOLD), false
        );
        return InteractionResult.SUCCESS;
    }
}
