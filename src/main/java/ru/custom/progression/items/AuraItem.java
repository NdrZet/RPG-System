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
import ru.custom.progression.skills.SkillEventHooks;

/**
 * Аура Защиты Жреца — союзники в радиусе 8 блоков получают Resistance I + Regen I на 15 сек.
 * Разблокируется нодой {@code p_aura}. КД 120 сек.
 */
public class AuraItem extends Item {

    private static final long COOLDOWN_MS = 120_000L;

    public AuraItem(Properties props) {
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
                    Component.literal("Аура перезаряжается... ещё " + remaining + " сек.")
                            .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        AABB box = sp.getBoundingBox().inflate(8.0);
        for (ServerPlayer ally : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 300, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0));
        }

        SkillEventHooks.registerItemCooldown(this, sp.getUUID(), now);
        sp.displayClientMessage(
                Component.literal("☩ Аура Защиты").withStyle(ChatFormatting.WHITE), false
        );
        return InteractionResult.SUCCESS;
    }
}
