package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Свиток Удачи Мага — активная способность: даёт Удача I на 60 секунд.
 * Улучшает лут из сундуков, рыбалки и дропа мобов.
 * Кулдаун: 90 секунд.
 */
public class LuckScrollItem extends Item {

    private static final long COOLDOWN_MS = 90_000L;
    private static final Map<UUID, Long> lastUsed = new HashMap<>();

    public LuckScrollItem() {
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
                Component.literal("Свиток перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED)
            );
            return InteractionResultHolder.fail(stack);
        }

        // Удача I на 60 секунд (1200 тиков)
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 0));
        lastUsed.put(player.getUUID(), now);
        player.sendSystemMessage(
            Component.literal("✦ Удача активирована на 60 сек! Богатый лут ждёт.")
                     .withStyle(ChatFormatting.GOLD)
        );
        return InteractionResultHolder.success(stack);
    }
}
