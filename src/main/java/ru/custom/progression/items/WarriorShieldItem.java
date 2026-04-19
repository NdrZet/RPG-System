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
import ru.custom.progression.skills.SkillEventHooks;

/**
 * Щит Воина — активный предмет, разблокируется нодой {@code w_cmd_shield}.
 * ПКМ: Absorption IV на 8 сек, КД 120 сек.
 */
public class WarriorShieldItem extends Item {

    private static final long COOLDOWN_MS = 120_000L;

    public WarriorShieldItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        long now = System.currentTimeMillis();
        long elapsed = now - SkillEventHooks.getItemLastUsed(this, sp.getUUID());

        if (elapsed < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
            sp.displayClientMessage(
                    Component.literal("Щит перезаряжается... ещё " + remaining + " сек.")
                            .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 3));
        SkillEventHooks.registerItemCooldown(this, sp.getUUID(), now);
        sp.displayClientMessage(
                Component.literal("⛨ Щит Воина!").withStyle(ChatFormatting.GOLD), false
        );
        return InteractionResult.SUCCESS;
    }
}
