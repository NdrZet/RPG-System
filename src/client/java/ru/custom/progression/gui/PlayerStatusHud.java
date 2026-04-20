package ru.custom.progression.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * Кастомный RPG HUD: HP / броня / сытость / ванильный XP.
 * Рисуется в левом верхнем углу; ванильные бары снимаются через HudElementRegistry.
 */
@Environment(EnvType.CLIENT)
public final class PlayerStatusHud {

    // ── Геометрия ────────────────────────────────────────────────────────────
    /** X бара. Иконка рисуется в (X − 10), нужно место под неё. */
    private static final int X = 16;
    private static final int Y = 6;
    private static final int ICON_OFFSET = 10;
    private static final int BAR_WIDTH = 110;
    private static final int BAR_HEIGHT = 8;
    private static final int ROW_GAP = 12;
    private static final int XP_HEIGHT = 4;

    // ── Анимация (lerp) ──────────────────────────────────────────────────────
    /** Скорость сглаживания: доля пути, которую бар проходит за тик (20 tps). */
    private static final float LERP_SPEED = 0.25f;

    private static float displayedHp = -1f;
    private static float displayedArmor = -1f;
    private static float displayedFood = -1f;
    private static float displayedAbsorb = -1f;

    // ── Цвета ────────────────────────────────────────────────────────────────
    private static final int COLOR_BG = 0xCC000000;
    private static final int COLOR_EMPTY = 0xFF2A2A2A;
    private static final int COLOR_BORDER = 0xFF101010;

    private static final int COLOR_HP_HIGH = 0xFF33DD55;
    private static final int COLOR_HP_MID = 0xFFE5B800;
    private static final int COLOR_HP_LOW = 0xFFE53030;
    private static final int COLOR_ABSORB = 0xFFFFC235;

    private static final int COLOR_ARMOR = 0xFF6FA8FF;
    private static final int COLOR_FOOD = 0xFFC9772A;
    private static final int COLOR_XP = 0xFF6CE64B;

    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private PlayerStatusHud() {}

    public static void register() {
        // Отключаем ванильные элементы HUD
        HudElementRegistry.removeElement(VanillaHudElements.HEALTH_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.FOOD_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.ARMOR_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.AIR_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.INFO_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.EXPERIENCE_LEVEL);

        // Добавляем свой слой
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("spa-rpg", "player_status"),
                PlayerStatusHud::render);

        // Сброс сглаживания при смене мира
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                displayedHp = displayedArmor = displayedFood = displayedAbsorb = -1f;
            }
        });
    }

    private static void render(GuiGraphics gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return; // creative/spectator

        Player p = mc.player;

        // Цели
        float hp = p.getHealth();
        float maxHp = p.getMaxHealth();
        float absorb = p.getAbsorptionAmount();
        float armor = p.getArmorValue();
        FoodData fd = p.getFoodData();
        float food = fd.getFoodLevel();

        // Инициализация при первом кадре
        if (displayedHp < 0f) {
            displayedHp = hp;
            displayedArmor = armor;
            displayedFood = food;
            displayedAbsorb = absorb;
        }

        // Плавная интерполяция (с учётом partial-tick)
        float t = LERP_SPEED;
        displayedHp = lerp(displayedHp, hp, t);
        displayedArmor = lerp(displayedArmor, armor, t);
        displayedFood = lerp(displayedFood, food, t);
        displayedAbsorb = lerp(displayedAbsorb, absorb, t);

        int y = Y;
        drawHealthBar(gfx, mc.font, X, y, displayedHp, maxHp, displayedAbsorb);
        y += ROW_GAP;
        drawArmorBar(gfx, mc.font, X, y, displayedArmor, armor);
        y += ROW_GAP;
        drawBar(gfx, mc.font, X, y, displayedFood, 20f, food, 20f,
                COLOR_FOOD, "\u25C9"); // ◉ — еда
        y += ROW_GAP;
        drawXpBar(gfx, mc.font, X, y, p);
    }

    // ── Отрисовка ────────────────────────────────────────────────────────────

    private static void drawHealthBar(GuiGraphics gfx, Font font, int x, int y,
                                      float shown, float max, float absorbShown) {
        float ratio = max <= 0f ? 0f : clamp(shown / max);
        int color = ratio < 0.33f ? COLOR_HP_LOW
                  : ratio < 0.66f ? COLOR_HP_MID
                                  : COLOR_HP_HIGH;

        drawBarFrame(gfx, x, y, BAR_WIDTH, BAR_HEIGHT);
        int fillW = (int) (BAR_WIDTH * ratio);
        gfx.fill(x, y, x + fillW, y + BAR_HEIGHT, color);

        // Абсорбция — золотая полоска поверх правого края
        if (absorbShown > 0.01f) {
            float absorbRatio = clamp(absorbShown / max);
            int absorbW = (int) (BAR_WIDTH * absorbRatio);
            gfx.fill(x + BAR_WIDTH - absorbW, y, x + BAR_WIDTH, y + 2, COLOR_ABSORB);
        }

        String label = ceilInt(shown) + "/" + ceilInt(max);
        if (absorbShown > 0.01f) label += " +" + ceilInt(absorbShown);
        gfx.drawString(font, label, x + BAR_WIDTH + 4, y, COLOR_TEXT, true);

        gfx.drawString(font, "\u2764", x - ICON_OFFSET, y, COLOR_HP_LOW, true); // ❤
    }

    /**
     * Бар брони без верхнего лимита. Заполнение считается по скользящей шкале:
     * каждая следующая «двадцатка» укладывается во всё более сжатую долю бара,
     * поэтому 20/40/80/∞ всегда влезают, и число не расходится с визуалом.
     */
    private static void drawArmorBar(GuiGraphics gfx, Font font, int x, int y,
                                     float shown, float real) {
        drawBarFrame(gfx, x, y, BAR_WIDTH, BAR_HEIGHT);
        float ratio = 1f - (float) Math.pow(0.5d, Math.max(0f, shown) / 20d);
        int fillW = (int) (BAR_WIDTH * ratio);
        gfx.fill(x, y, x + fillW, y + BAR_HEIGHT, COLOR_ARMOR);

        String label = Integer.toString(ceilInt(real));
        gfx.drawString(font, label, x + BAR_WIDTH + 4, y, COLOR_TEXT, true);
        gfx.drawString(font, "\u2726", x - ICON_OFFSET, y, COLOR_ARMOR, true); // ✦
    }

    private static void drawBar(GuiGraphics gfx, Font font, int x, int y,
                                float shown, float max,
                                float realValue, float realMax,
                                int color, String icon) {
        float ratio = max <= 0f ? 0f : clamp(shown / max);
        drawBarFrame(gfx, x, y, BAR_WIDTH, BAR_HEIGHT);
        int fillW = (int) (BAR_WIDTH * ratio);
        gfx.fill(x, y, x + fillW, y + BAR_HEIGHT, color);

        String label = ceilInt(realValue) + "/" + ceilInt(realMax);
        gfx.drawString(font, label, x + BAR_WIDTH + 4, y, COLOR_TEXT, true);
        gfx.drawString(font, icon, x - ICON_OFFSET, y, color, true);
    }

    private static void drawXpBar(GuiGraphics gfx, Font font, int x, int y, Player p) {
        int level = p.experienceLevel;
        float progress = clamp(p.experienceProgress);

        drawBarFrame(gfx, x, y, BAR_WIDTH, XP_HEIGHT);
        int fillW = (int) (BAR_WIDTH * progress);
        gfx.fill(x, y, x + fillW, y + XP_HEIGHT, COLOR_XP);

        String label = "Lv " + level + "  " + (int) (progress * 100f) + "%";
        gfx.drawString(font, label, x + BAR_WIDTH + 4, y - 2, COLOR_XP, true);
    }

    private static void drawBarFrame(GuiGraphics gfx, int x, int y, int w, int h) {
        // внешняя рамка
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, COLOR_BORDER);
        // фон
        gfx.fill(x, y, x + w, y + h, COLOR_BG);
        // пустая «жёлоб» часть
        gfx.fill(x, y, x + w, y + h, COLOR_EMPTY);
    }

    // ── Утилиты ──────────────────────────────────────────────────────────────

    private static float lerp(float current, float target, float t) {
        if (Math.abs(current - target) < 0.01f) return target;
        return current + (target - current) * t;
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static int ceilInt(float v) {
        return (int) Math.ceil(v);
    }
}
