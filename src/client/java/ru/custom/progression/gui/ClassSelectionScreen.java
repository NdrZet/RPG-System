package ru.custom.progression.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.custom.progression.network.ClientNetworkHandler;

/**
 * Экран выбора класса персонажа в стиле Sodium.
 * Открывается кнопкой «Выбрать путь» (доступна с 5-го уровня) из {@link StatsScreen}.
 */
@Environment(EnvType.CLIENT)
public class ClassSelectionScreen extends Screen {

    private static final int COL_BG       = 0xE0000000;
    private static final int COL_PANEL    = 0xFF0A0A0A;
    private static final int COL_DIVIDER  = 0xFF2A2A2A;
    private static final int COL_ACCENT   = 0xFFFFAA00;
    private static final int COL_TEXT     = 0xFFE0E0E0;
    private static final int COL_TEXT_DIM = 0xFF888888;
    private static final int COL_WARNING  = 0xFFFF6347;

    private static final int MARGIN      = 16;
    private static final int SIDEBAR_W   = 160;

    private final Screen parentScreen;

    public ClassSelectionScreen(Screen parentScreen) {
        super(Component.literal("Выбор класса"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int cx = MARGIN + SIDEBAR_W + MARGIN;
        int cr = this.width - MARGIN;
        int contentW = cr - cx;

        int btnW = Math.min(320, contentW - 16);
        int btnH = 24;
        int btnX = cx + (contentW - btnW) / 2;
        int startY = MARGIN + 80;
        int gap = btnH + 8;

        this.addRenderableWidget(Button.builder(
                Component.literal("Воин — ближний бой, танк (STR / VIT)"),
                b -> selectClass("Воин")
        ).bounds(btnX, startY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Маг — магия, опыт, удача (INT)"),
                b -> selectClass("Маг")
        ).bounds(btnX, startY + gap, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Следопыт — скорость, дальний бой (AGI)"),
                b -> selectClass("Следопыт")
        ).bounds(btnX, startY + gap * 2, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Жрец — поддержка, лечение (VIT / INT)"),
                b -> selectClass("Жрец")
        ).bounds(btnX, startY + gap * 3, btnW, btnH).build());

        // «Назад» в правом нижнем углу (как «Готово» в StatsScreen)
        this.addRenderableWidget(Button.builder(
                Component.literal("Назад"),
                b -> this.onClose()
        ).bounds(this.width - MARGIN - 80, this.height - MARGIN - 20, 80, 16).build());
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, COL_BG);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        this.renderBackground(gfx, mouseX, mouseY, delta);

        drawSidebar(gfx);
        drawContent(gfx);

        super.render(gfx, mouseX, mouseY, delta);
    }

    private void drawSidebar(GuiGraphics gfx) {
        int sx = MARGIN;
        int sy = MARGIN;
        int sr = sx + SIDEBAR_W;
        int sb = this.height - MARGIN;

        gfx.fill(sx, sy, sr, sb, COL_PANEL);

        gfx.drawString(this.font, "SPA Progression", sx + 8, sy + 8, COL_ACCENT, false);
        gfx.drawString(this.font, "v1.0", sx + 8, sy + 18, COL_TEXT_DIM, false);
        gfx.fill(sx + 8, sy + 32, sr - 8, sy + 33, COL_DIVIDER);

        gfx.drawString(this.font, "Выбор пути",  sx + 8, sy + 40, 0xFFFFD700, false);
        gfx.drawString(this.font, "необратим!",  sx + 8, sy + 52, COL_WARNING, false);

        // Короткие подсказки
        int ty = sy + 72;
        gfx.drawString(this.font, "• Определяет",        sx + 8, ty,        COL_TEXT_DIM, false);
        gfx.drawString(this.font, "  бонусы класса",     sx + 8, ty + 10,   COL_TEXT_DIM, false);
        gfx.drawString(this.font, "• Открывает древо",   sx + 8, ty + 24,   COL_TEXT_DIM, false);
        gfx.drawString(this.font, "  навыков",           sx + 8, ty + 34,   COL_TEXT_DIM, false);
        gfx.drawString(this.font, "• Выдаёт классовый",  sx + 8, ty + 48,   COL_TEXT_DIM, false);
        gfx.drawString(this.font, "  предмет",           sx + 8, ty + 58,   COL_TEXT_DIM, false);
    }

    private void drawContent(GuiGraphics gfx) {
        int cx = MARGIN + SIDEBAR_W + MARGIN;
        int cr = this.width - MARGIN;

        gfx.drawString(this.font, "ВЫБЕРИТЕ КЛАСС",
                cx, MARGIN + 8, COL_ACCENT, false);
        gfx.fill(cx, MARGIN + 20, cr, MARGIN + 21, COL_DIVIDER);

        gfx.drawString(this.font,
                "Каждый класс имеет уникальное древо навыков и стартовые бонусы.",
                cx, MARGIN + 32, COL_TEXT, false);
        gfx.drawString(this.font,
                "После выбора класс нельзя изменить.",
                cx, MARGIN + 44, COL_WARNING, false);
    }

    private void selectClass(String className) {
        ClientNetworkHandler.sendChooseClass(className);
        this.onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
