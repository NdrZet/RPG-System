package ru.custom.progression.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.custom.progression.api.ClientStatsCache;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.network.ClientNetworkHandler;

/**
 * Рисует RPG-панель СПРАВА от инвентаря через addRenderableOnly,
 * чтобы текст и фон рисовались в одном батч-контексте (не в renderBg-фазе).
 */
@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    protected InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Unique private Button btnStr;
    @Unique private Button btnAgi;
    @Unique private Button btnVit;
    @Unique private Button btnInt;
    @Unique private Button btnClass;

    // ── init: регистрируем рендерабл-панель и кнопки ─────────────────────────

    @Inject(method = "method_25426", at = @At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        rebuildButtons();
    }

    @Unique
    private void rebuildButtons() {
        PlayerStats stats = ClientStatsCache.get();
        boolean hp = stats.getSkillPoints() > 0;

        // Панель: leftPos+178 … leftPos+288 (110 px), topPos+7 … topPos+97
        int px   = this.leftPos + 178;
        int py   = this.topPos  + 7;
        int btnX = px + 118;  // правый край кнопки (панель 130 px)
        // Смещение кнопок: строка «Опыт» добавила 10 px

        // Рендерабл фона/текста добавляем ПЕРВЫМ — чтобы рисовался ДО кнопок
        this.addRenderableOnly(this::drawPanel);

        btnStr = Button.builder(Component.literal("+"),
                b -> ClientNetworkHandler.sendStatUpgrade("strength"))
                .bounds(btnX, py + 53, 10, 10).build();
        btnStr.active = hp;

        btnAgi = Button.builder(Component.literal("+"),
                b -> ClientNetworkHandler.sendStatUpgrade("agility"))
                .bounds(btnX, py + 63, 10, 10).build();
        btnAgi.active = hp;

        btnVit = Button.builder(Component.literal("+"),
                b -> ClientNetworkHandler.sendStatUpgrade("vitality"))
                .bounds(btnX, py + 73, 10, 10).build();
        btnVit.active = hp;

        btnInt = Button.builder(Component.literal("+"),
                b -> ClientNetworkHandler.sendStatUpgrade("intelligence"))
                .bounds(btnX, py + 83, 10, 10).build();
        btnInt.active = hp;

        this.addRenderableWidget(btnStr);
        this.addRenderableWidget(btnAgi);
        this.addRenderableWidget(btnVit);
        this.addRenderableWidget(btnInt);

        if (stats.getLevel() >= 5 && "Странник".equals(stats.getPlayerClass())) {
            btnClass = Button.builder(
                    Component.literal("ВЫБРАТЬ ПУТЬ"),
                    b -> openClassSelectionDialog()
            ).bounds(px, py + 92, 110, 12).build();
            this.addRenderableWidget(btnClass);
        }
    }

    // ── рендер панели (вызывается из Renderable каждый кадр) ─────────────────

    @Unique
    private void drawPanel(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        PlayerStats stats = ClientStatsCache.get();

        if (ClientStatsCache.consumeReinitFlag()) {
            boolean hp = stats.getSkillPoints() > 0;
            if (btnStr != null) btnStr.active = hp;
            if (btnAgi != null) btnAgi.active = hp;
            if (btnVit != null) btnVit.active = hp;
            if (btnInt != null) btnInt.active = hp;
        }

        int px = this.leftPos + 178;
        int py = this.topPos  + 7;
        int pr = px + 130;
        int pb = py + 103;

        // Фон + рамка
        gfx.fill(px,     py,     pr,     pb,     0xFF1A1A1A);
        gfx.fill(px,     py,     pr,     py + 1, 0xFF555555); // верх
        gfx.fill(px,     pb - 1, pr,     pb,     0xFF555555); // низ
        gfx.fill(px,     py,     px + 1, pb,     0xFF555555); // лево
        gfx.fill(pr - 1, py,     pr,     pb,     0xFF555555); // право

        int tx = px + 4;
        gfx.drawString(this.font, "Класс: "   + stats.getPlayerClass(), tx, py +  2, 0xFFFFD700, false);
        gfx.drawString(this.font, "Уровень: " + stats.getLevel(),       tx, py + 12, 0xFFFFFFFF, false);

        int xpMax   = stats.getLevel() * 100;
        int xpColor = stats.getExperience() >= xpMax * 3 / 4 ? 0xFF00FF00 : 0xFFAAAAAA;
        gfx.drawString(this.font, "Опыт: " + stats.getExperience() + " / " + xpMax,
                       tx, py + 22, xpColor, false);

        // Прогресс-бар XP (3px высотой, между строками Опыт и Ранг)
        int barX1  = px + 2;
        int barX2  = pr - 2;
        int barW   = barX2 - barX1;
        int filled = (int)((long) barW * Math.min(stats.getExperience(), xpMax) / xpMax);
        gfx.fill(barX1,          py + 29, barX2,          py + 32, 0xFF333333);           // фон
        if (filled > 0) {
            int fillColor = stats.getExperience() >= xpMax * 3 / 4 ? 0xFF00CC00 : 0xFF227722;
            gfx.fill(barX1,      py + 29, barX1 + filled, py + 32, fillColor);            // заполнение
        }
        gfx.fill(barX1,          py + 29, barX2,          py + 30, 0xFF555555);           // рамка верх
        gfx.fill(barX1,          py + 31, barX2,          py + 32, 0xFF555555);           // рамка низ

        gfx.drawString(this.font, "Ранг: "    + stats.getRank(),        tx, py + 32, 0xFFADD8E6, false);

        int spColor = stats.getSkillPoints() > 0 ? 0xFF00FF00 : 0xFF888888;
        gfx.drawString(this.font, "Очки: "    + stats.getSkillPoints(), tx, py + 42, spColor,    false);

        gfx.fill(px + 2, py + 52, pr - 2, py + 53, 0xFF444444); // разделитель

        gfx.drawString(this.font, "Сила: "         + stats.getStrength(),     tx, py + 56, 0xFFFF6347, false);
        gfx.drawString(this.font, "Ловкость: "     + stats.getAgility(),      tx, py + 66, 0xFF90EE90, false);
        gfx.drawString(this.font, "Выносливость: " + stats.getVitality(),     tx, py + 76, 0xFFFF69B4, false);
        gfx.drawString(this.font, "Интеллект: "    + stats.getIntelligence(), tx, py + 86, 0xFF87CEEB, false);
    }

    @Unique
    private void openClassSelectionDialog() {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new ru.custom.progression.gui.ClassSelectionScreen(
                        (InventoryScreen)(Object)this));
    }
}
