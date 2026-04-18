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
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.skills.SkillEventHooks;
import ru.custom.progression.storage.DataManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Боевой Клич Воина — активная способность: Сила I на 10 сек.
 * Ноды дерева: −% КД, +сек длительности, Haste I (Военный клич).
 */
public class WarCryItem extends Item {

    private static final long BASE_COOLDOWN_MS = 60_000L;
    private static final int  BASE_DURATION_TICKS = 200;
    private static final Map<UUID, Long> lastUsed = new HashMap<>();

    public WarCryItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        PlayerStats stats = DataManager.getPlayer(sp.getUUID());
        long cdReduce = stats == null ? 0 : SkillEventHooks.warCryCooldownReductionMs(stats);
        int durBonus  = stats == null ? 0 : SkillEventHooks.warCryDurationBonusTicks(stats);
        boolean haste = stats != null && SkillEventHooks.hasWarCryHaste(stats);

        long cooldown = Math.max(5_000L, BASE_COOLDOWN_MS - cdReduce);
        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed.getOrDefault(sp.getUUID(), 0L);

        if (elapsed < cooldown) {
            long remaining = (cooldown - elapsed) / 1000 + 1;
            sp.displayClientMessage(
                Component.literal("Боевой клич перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        int duration = BASE_DURATION_TICKS + durBonus;
        sp.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 0));
        if (haste) {
            sp.addEffect(new MobEffectInstance(MobEffects.HASTE, duration, 0));
        }

        lastUsed.put(sp.getUUID(), now);
        sp.displayClientMessage(
            Component.literal("⚔ Боевой клич!").withStyle(ChatFormatting.RED), false
        );
        return InteractionResult.SUCCESS;
    }
}
