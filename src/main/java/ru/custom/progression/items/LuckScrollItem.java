package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel)) return InteractionResult.PASS;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed.getOrDefault(player.getUUID(), 0L);

        if (elapsed < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
            player.displayClientMessage(
                Component.literal("Свиток перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        // Удача I на 60 секунд (1200 тиков)
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 0));
        lastUsed.put(player.getUUID(), now);
        player.displayClientMessage(
            Component.literal("✦ Удача активирована на 60 сек! Богатый лут ждёт.")
                     .withStyle(ChatFormatting.GOLD), false
        );
        return InteractionResult.SUCCESS;
    }
}
