package ru.custom.progression.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Жертвенное сияние Жреца — снимает 20% HP с Жреца, союзники в р.10
 * получают +6 HP и Resistance II на 15 сек. Разблокируется нодой
 * {@code p_sacrifice_light}. КД 90 сек.
 */
public class SacrificeRelicItem extends Item {

    private static final long COOLDOWN_MS = 90_000L;
    private static final Map<UUID, Long> lastUsed = new HashMap<>();

    public SacrificeRelicItem(Properties props) {
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
                    Component.literal("Сияние перезаряжается... ещё " + remaining + " сек.")
                            .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        // Нельзя применить, если HP слишком мало
        float cost = sp.getMaxHealth() * 0.20f;
        if (sp.getHealth() <= cost + 1.0f) {
            sp.displayClientMessage(
                    Component.literal("Недостаточно HP для сияния.")
                            .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }
        sp.hurtServer(sl, sl.damageSources().magic(), cost);

        AABB box = sp.getBoundingBox().inflate(10.0);
        for (ServerPlayer ally : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (ally == sp) continue;
            if (ally.getHealth() < ally.getMaxHealth()) ally.heal(6.0f);
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 300, 1));
        }

        lastUsed.put(sp.getUUID(), now);
        sp.displayClientMessage(
                Component.literal("☩ Жертвенное сияние").withStyle(ChatFormatting.WHITE), false
        );
        return InteractionResult.SUCCESS;
    }
}
