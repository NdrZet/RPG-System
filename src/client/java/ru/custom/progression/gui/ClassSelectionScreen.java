package ru.custom.progression.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.custom.progression.network.ClientNetworkHandler;

/**
 * Экран выбора класса персонажа.
 * Открывается при нажатии кнопки «ВЫБРАТЬ ПУТЬ» (доступно с 5-го уровня).
 * <p>
 * Доступные классы:
 * <ul>
 *   <li><b>Воин</b>  — бонус к STR и VIT</li>
 *   <li><b>Маг</b>   — бонус к INT</li>
 *   <li><b>Следопыт</b> — бонус к AGI</li>
 *   <li><b>Жрец</b>  — бонус к VIT и INT</li>
 * </ul>
 * После выбора отправляется {@link ru.custom.progression.network.ChooseClassPayload}
 * на сервер, и экран закрывается.
 */
@Environment(EnvType.CLIENT)
public class ClassSelectionScreen extends Screen {

    /** Экран, из которого был открыт этот (инвентарь). */
    private final Screen parentScreen;

    /** Ширина и высота диалогового окна. */
    private static final int DIALOG_W = 160;
    private static final int DIALOG_H = 130;

    public ClassSelectionScreen(Screen parentScreen) {
        super(Component.literal("Выбор класса"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        // Центр экрана
        int cx = (this.width  - DIALOG_W) / 2;
        int cy = (this.height - DIALOG_H) / 2;

        int btnW = 120;
        int btnH = 16;
        int startY = cy + 30;
        int gap    = 20;

        // ── Кнопки выбора классов ─────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
                Component.literal("⚔  Воин  (STR / VIT)"),
                btn -> selectClass("Воин")
        ).bounds(cx + 20, startY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("✦  Маг  (INT)"),
                btn -> selectClass("Маг")
        ).bounds(cx + 20, startY + gap, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("🏹  Следопыт  (AGI)"),
                btn -> selectClass("Следопыт")
        ).bounds(cx + 20, startY + gap * 2, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("☩  Жрец  (VIT / INT)"),
                btn -> selectClass("Жрец")
        ).bounds(cx + 20, startY + gap * 3, btnW, btnH).build());

        // ── Отмена ────────────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
                Component.literal("Назад"),
                btn -> this.onClose()
        ).bounds(cx + 40, startY + gap * 4, 80, btnH).build());
    }

    /**
     * Переопределяем renderBackground чтобы не вызывать blur-эффект повторно.
     * Blur уже применён инвентарём; второй вызов в тот же кадр бросает
     * IllegalStateException ("Can only blur once per frame") в MC 1.21.11.
     */
    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xAA000000);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        // Затемнение фона (через переопределённый renderBackground — без блюра)
        this.renderBackground(gfx, mouseX, mouseY, delta);

        int cx = (this.width  - DIALOG_W) / 2;
        int cy = (this.height - DIALOG_H) / 2;

        // Фон диалога
        gfx.fill(cx,     cy,     cx + DIALOG_W, cy + DIALOG_H, 0xDD1A1A2E);
        // Рамка
        gfx.fill(cx,     cy,     cx + DIALOG_W, cy + 1,             0xFF5050FF);
        gfx.fill(cx,     cy + DIALOG_H - 1, cx + DIALOG_W, cy + DIALOG_H, 0xFF5050FF);
        gfx.fill(cx,     cy, cx + 1,  cy + DIALOG_H, 0xFF5050FF);
        gfx.fill(cx + DIALOG_W - 1, cy, cx + DIALOG_W, cy + DIALOG_H, 0xFF5050FF);

        // Заголовок
        gfx.drawCenteredString(this.font,
                Component.literal("✦ ВЫБРАТЬ ПУТЬ ✦"),
                this.width / 2, cy + 8, 0xFFD700);

        // Подзаголовок
        gfx.drawCenteredString(this.font,
                Component.literal("Выбор необратим!"),
                this.width / 2, cy + 18, 0xFF6666);

        super.render(gfx, mouseX, mouseY, delta);
    }

    /** Отправляет выбранный класс на сервер и закрывает экран. */
    private void selectClass(String className) {
        ClientNetworkHandler.sendChooseClass(className);
        this.onClose();
    }

    @Override
    public void onClose() {
        // Возвращаемся в инвентарь
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
