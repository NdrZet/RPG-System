package ru.custom.progression.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import ru.custom.progression.api.ClientStatsCache;

/**
 * HUD-элемент ноды «Всезнание» ({@code m_omniscience}): показывает HP моба,
 * на которого смотрит игрок (в пределах 32 блоков). Рисуется в центре экрана,
 * чуть ниже прицела. Активен только если нода разблокирована.
 */
@Environment(EnvType.CLIENT)
public final class OmniscienceHud {

    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 4;

    private OmniscienceHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(OmniscienceHud::render);
    }

    private static void render(GuiGraphics gfx, net.minecraft.client.DeltaTracker delta) {
        if (!ClientStatsCache.get().isNodeUnlocked("m_omniscience")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof EntityHitResult ehr)) return;
        Entity target = ehr.getEntity();
        if (!(target instanceof LivingEntity le) || target instanceof Player) return;

        float hp = le.getHealth();
        float maxHp = le.getMaxHealth();
        if (maxHp <= 0f) return;
        float ratio = Math.max(0f, Math.min(1f, hp / maxHp));

        int screenW = gfx.guiWidth();
        int screenH = gfx.guiHeight();
        int x = (screenW - BAR_WIDTH) / 2;
        int y = screenH / 2 + 12;

        // Фон (тёмный)
        gfx.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xAA000000);
        // Пустая часть (серая)
        gfx.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF444444);
        // Заполненная часть (от красного к зелёному)
        int color = ratio < 0.33f ? 0xFFFF3333
                   : ratio < 0.66f ? 0xFFFFAA00
                                   : 0xFF33DD33;
        gfx.fill(x, y, x + (int)(BAR_WIDTH * ratio), y + BAR_HEIGHT, color);

        // Текст "имя — HP/maxHP"
        String label = le.getType().getDescription().getString()
                + " — " + (int)Math.ceil(hp) + "/" + (int)Math.ceil(maxHp);
        Component comp = Component.literal(label).withStyle(ChatFormatting.WHITE);
        int tw = mc.font.width(comp);
        gfx.drawString(mc.font, comp, (screenW - tw) / 2, y + BAR_HEIGHT + 2, 0xFFFFFFFF, true);
    }
}
