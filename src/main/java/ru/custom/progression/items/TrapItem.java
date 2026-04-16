package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ловушка Следопыта — ПКМ создаёт облако с Slowness III + Weakness I на 5 сек.
 * Выдаётся Следопыту автоматически при достижении 20-го уровня.
 * Кулдаун: 45 секунд.
 */
public class TrapItem extends Item {

    private static final long COOLDOWN_MS = 45_000L;
    private static final Map<UUID, Long> lastUsed = new HashMap<>();

    public TrapItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed.getOrDefault(player.getUUID(), 0L);

        if (elapsed < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
            player.displayClientMessage(
                Component.literal("Ловушка перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        // Создаём Area Effect Cloud под ногами игрока
        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                player.getX(), player.getY(), player.getZ());
        cloud.setOwner(player);
        cloud.setRadius(2.5f);
        cloud.setRadiusOnUse(-0.5f);
        cloud.setWaitTime(0);
        cloud.setDuration(100); // 5 секунд
        cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2)); // Slowness III
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));          // Weakness I
        serverLevel.addFreshEntity(cloud);

        lastUsed.put(player.getUUID(), now);
        player.displayClientMessage(
            Component.literal("🪤 Ловушка расставлена!").withStyle(ChatFormatting.GREEN), false
        );
        return InteractionResult.SUCCESS;
    }
}
