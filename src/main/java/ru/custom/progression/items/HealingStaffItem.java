package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        long now = System.currentTimeMillis();
        long last = lastUsed.getOrDefault(player.getUUID(), 0L);
        long elapsed = now - last;

        if (elapsed < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
            player.sendSystemMessage(
                Component.literal("Посох перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED)
            );
            return InteractionResultHolder.fail(stack);
        }

        player.heal(4.0f);
        lastUsed.put(player.getUUID(), now);
        player.sendSystemMessage(
            Component.literal("✦ Исцеление: +2 сердца").withStyle(ChatFormatting.GREEN)
        );
        return InteractionResultHolder.success(stack);
    }
}
