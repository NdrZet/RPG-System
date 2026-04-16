package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Посох Жреца — активная способность: исцеляет 4 HP (2 сердца).
 * Кулдаун: 30 секунд.
 */
public class HealingStaffItem extends Item {

    private static final long COOLDOWN_MS = 30_000L;
    private static final Map<UUID, Long> lastUsed = new HashMap<>();

    public HealingStaffItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel)) return InteractionResult.PASS;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed.getOrDefault(player.getUUID(), 0L);

        if (elapsed < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
            player.displayClientMessage(
                Component.literal("Посох перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        player.heal(4.0f);
        lastUsed.put(player.getUUID(), now);
        player.displayClientMessage(
            Component.literal("✦ Исцеление: +2 сердца").withStyle(ChatFormatting.GREEN), false
        );
        return InteractionResult.SUCCESS;
    }
}
