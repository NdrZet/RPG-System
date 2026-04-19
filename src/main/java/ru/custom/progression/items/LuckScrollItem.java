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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.skills.SkillEventHooks;
import ru.custom.progression.storage.DataManager;


/**
 * Свиток Удачи Мага — Удача I на 60 сек, КД 90 сек.
 * Ноды: −% КД свитка.
 */
public class LuckScrollItem extends Item {

    private static final long BASE_COOLDOWN_MS = 90_000L;

    public LuckScrollItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        PlayerStats stats = DataManager.getPlayer(sp.getUUID());

        // Мерцание: если нода m_blink разблокирована и игрок приседает → телепорт 10 блоков
        if (stats != null && stats.isNodeUnlocked("m_blink") && sp.isShiftKeyDown()) {
            long nowTicks = sl.getGameTime();
            if (!SkillEventHooks.blinkReady(sp.getUUID(), nowTicks)) {
                long remaining = (SkillEventHooks.blinkCooldownTicks()
                        - (nowTicks - SkillEventHooks.lastBlinkTick(sp.getUUID()))) / 20L + 1L;
                sp.displayClientMessage(
                    Component.literal("Мерцание перезаряжается... ещё " + remaining + " сек.")
                             .withStyle(ChatFormatting.RED), false
                );
                return InteractionResult.FAIL;
            }
            Vec3 eye = sp.getEyePosition();
            Vec3 view = sp.getViewVector(1.0f);
            Vec3 target = eye.add(view.scale(10.0));
            HitResult hit = sl.clip(new ClipContext(eye, target,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
            Vec3 dst = hit.getLocation();
            if (hit.getType() == HitResult.Type.BLOCK) {
                dst = dst.subtract(view.scale(0.5));
            }
            sp.teleportTo(dst.x, dst.y, dst.z);
            sp.fallDistance = 0f;
            SkillEventHooks.markBlinkUsed(sp.getUUID(), nowTicks);
            sp.displayClientMessage(
                Component.literal("✦ Мерцание").withStyle(ChatFormatting.AQUA), false
            );
            return InteractionResult.SUCCESS;
        }

        long cdReduce = stats == null ? 0 : SkillEventHooks.luckScrollCooldownReductionMs(stats);
        long cooldown = Math.max(10_000L, BASE_COOLDOWN_MS - cdReduce);

        long now = System.currentTimeMillis();
        long elapsed = now - SkillEventHooks.getItemLastUsed(this, sp.getUUID());

        if (elapsed < cooldown) {
            long remaining = (cooldown - elapsed) / 1000 + 1;
            sp.displayClientMessage(
                Component.literal("Свиток перезаряжается... ещё " + remaining + " сек.")
                         .withStyle(ChatFormatting.RED), false
            );
            return InteractionResult.FAIL;
        }

        sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 0));
        SkillEventHooks.registerItemCooldown(this, sp.getUUID(), now);
        sp.displayClientMessage(
            Component.literal("✦ Удача активирована на 60 сек! Богатый лут ждёт.")
                     .withStyle(ChatFormatting.GOLD), false
        );
        return InteractionResult.SUCCESS;
    }
}
