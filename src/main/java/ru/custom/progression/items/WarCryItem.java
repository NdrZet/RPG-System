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
 * Боевой Клич Воина — активная способность: даёт Сила I на 10 секунд.
 * Кулдаун: 60 секунд.
 */
public class WarCryItem extends Item {

    private static final long COOLDOWN_MS = 60_000L;
    private static final Map<UUID, Long> lastUsed = new HashMap<>();

    public WarCryItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel)) return InteractionResult.PASS;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed.getOrDefault(player.getUUID(), 0L);

        if (elapsed < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
            player.displayClientMessage(
                Component.literal("Боевой клич перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        // Сила I на 10 секунд (200 тиков)
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0));
        lastUsed.put(player.getUUID(), now);
        player.displayClientMessage(
            Component.literal("⚔ Боевой клич! Сила усилена на 10 сек.").withStyle(ChatFormatting.RED), false
        );
        return InteractionResult.SUCCESS;
    }
}
