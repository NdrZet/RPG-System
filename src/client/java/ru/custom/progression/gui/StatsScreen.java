package ru.custom.progression.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ru.custom.progression.api.ClientStatsCache;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.network.ClientNetworkHandler;
import ru.custom.progression.skills.SkillNode;
import ru.custom.progression.skills.SkillTree;
import ru.custom.progression.skills.SkillTreeDefinitions;

import java.util.Set;

/**
 * Экран прогрессии в стиле Sodium: тёмный фон, боковая панель, верхние табы.
 * Табы: Характеристики, Навыки (древо).
 */
@Environment(EnvType.CLIENT)
public class StatsScreen extends Screen {

    // ── Палитра ──────────────────────────────────────────────────────────
    private static final int COL_BG        = 0xE0000000;
    private static final int COL_SIDEBAR   = 0xFF0A0A0A;
    private static final int COL_DIVIDER   = 0xFF2A2A2A;
    private static final int COL_ACCENT    = 0xFFFFAA00;
    private static final int COL_TEXT      = 0xFFE0E0E0;
    private static final int COL_TEXT_DIM  = 0xFF888888;
    private static final int COL_HEADER    = 0xFFFFD700;

    // Состояния нод
    private static final int COL_NODE_LOCKED      = 0xFF3A3A3A;
    private static final int COL_NODE_AVAILABLE   = 0xFFFFE066; // доступна к покупке
    private static final int COL_NODE_UNLOCKED    = 0xFF00D060; // взята
    private static final int COL_LINK_INACTIVE    = 0xFF3A3A3A;
    private static final int COL_LINK_ACTIVE      = 0xFF00D060;

    private static final int MARGIN     = 16;
    private static final int SIDEBAR_W  = 160;
    private static final int TAB_H      = 22;
    private static final int ROW_H      = 22;
    private static final int ROW_GAP    = 4;

    enum Tab { STATS, SKILLS }

    private static Tab currentTab = Tab.STATS;

    private Button btnStr, btnAgi, btnVit, btnInt;
    private Button btnClass;
    private Button btnClose;
    private Button tabStats, tabSkills;

    // Для хит-теста клика по нодам: сохраняем последние координаты полотна
    private int treeOriginX;
    private int treeOriginY;

    public StatsScreen() {
        super(Component.literal("Прогрессия"));
    }

    @Override
    protected void init() {
        PlayerStats stats = ClientStatsCache.get();
        boolean hp = stats.getSkillPoints() > 0;

        int contentX = MARGIN + SIDEBAR_W + MARGIN;
        int contentR = this.width - MARGIN;
        int contentTop = MARGIN + TAB_H + 8;

        int tabW = 140;
        tabStats = Button.builder(
                Component.literal("Характеристики"),
                b -> { currentTab = Tab.STATS; this.rebuildForTab(); }
        ).bounds(contentX, MARGIN, tabW, TAB_H).build();
        tabSkills = Button.builder(
                Component.literal("Навыки"),
                b -> { currentTab = Tab.SKILLS; this.rebuildForTab(); }
        ).bounds(contentX + tabW + 4, MARGIN, tabW, TAB_H).build();
        this.addRenderableWidget(tabStats);
        this.addRenderableWidget(tabSkills);
        updateTabButtons();

        if (currentTab == Tab.STATS) {
            int btnW = 16;
            int btnX = contentR - btnW - 8;
            int rowY = contentTop + 40;

            btnStr = addPlusButton(btnX, rowY + (ROW_H + ROW_GAP) * 0 + 3, "strength", hp);
            btnAgi = addPlusButton(btnX, rowY + (ROW_H + ROW_GAP) * 1 + 3, "agility", hp);
            btnVit = addPlusButton(btnX, rowY + (ROW_H + ROW_GAP) * 2 + 3, "vitality", hp);
            btnInt = addPlusButton(btnX, rowY + (ROW_H + ROW_GAP) * 3 + 3, "intelligence", hp);
        }

        if (stats.getLevel() >= 5 && "Странник".equals(stats.getPlayerClass())) {
            btnClass = Button.builder(
                    Component.literal("Выбрать путь"),
                    b -> this.minecraft.setScreen(new ClassSelectionScreen(this))
            ).bounds(MARGIN + 8, this.height - MARGIN - 40, SIDEBAR_W - 16, 16).build();
            this.addRenderableWidget(btnClass);
        }

        btnClose = Button.builder(
                Component.literal("Готово"),
                b -> this.onClose()
        ).bounds(this.width - MARGIN - 80, this.height - MARGIN - 20, 80, 16).build();
        this.addRenderableWidget(btnClose);
    }

    private void rebuildForTab() {
        this.clearWidgets();
        this.init();
    }

    private void updateTabButtons() {
        if (tabStats != null) tabStats.active = currentTab != Tab.STATS;
        if (tabSkills != null) tabSkills.active = currentTab != Tab.SKILLS;
    }

    private Button addPlusButton(int x, int y, String stat, boolean active) {
        Button b = Button.builder(Component.literal("+"),
                btn -> ClientNetworkHandler.sendStatUpgrade(stat))
                .bounds(x, y, 16, 16).build();
        b.active = active;
        this.addRenderableWidget(b);
        return b;
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, COL_BG);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        this.renderBackground(gfx, mouseX, mouseY, delta);

        PlayerStats stats = ClientStatsCache.get();

        if (currentTab == Tab.STATS && ClientStatsCache.consumeReinitFlag()) {
            boolean hp = stats.getSkillPoints() > 0;
            if (btnStr != null) btnStr.active = hp;
            if (btnAgi != null) btnAgi.active = hp;
            if (btnVit != null) btnVit.active = hp;
            if (btnInt != null) btnInt.active = hp;
        }

        drawSidebar(gfx, stats);

        switch (currentTab) {
            case STATS  -> drawStatsTab(gfx, stats);
            case SKILLS -> drawSkillsTab(gfx, stats, mouseX, mouseY);
        }

        super.render(gfx, mouseX, mouseY, delta);
    }

    // ── Боковая панель ──────────────────────────────────────────────────

    private void drawSidebar(GuiGraphics gfx, PlayerStats stats) {
        int sx = MARGIN;
        int sy = MARGIN;
        int sr = sx + SIDEBAR_W;
        int sb = this.height - MARGIN;

        gfx.fill(sx, sy, sr, sb, COL_SIDEBAR);

        gfx.drawString(this.font, "SPA Progression", sx + 8, sy + 8, COL_ACCENT, false);
        gfx.drawString(this.font, "v1.0", sx + 8, sy + 18, COL_TEXT_DIM, false);
        gfx.fill(sx + 8, sy + 32, sr - 8, sy + 33, COL_DIVIDER);

        int ty = sy + 40;
        gfx.drawString(this.font, stats.getPlayerClass(), sx + 8, ty, COL_HEADER, false);
        ty += 12;
        gfx.drawString(this.font, "Уровень " + stats.getLevel(), sx + 8, ty, COL_TEXT, false);
        ty += 10;
        gfx.drawString(this.font, "Ранг: " + stats.getRank(), sx + 8, ty, 0xFFADD8E6, false);

        ty += 16;
        int spColor = stats.getSkillPoints() > 0 ? 0xFF00FF7F : COL_TEXT_DIM;
        gfx.drawString(this.font, "Очки навыков", sx + 8, ty, COL_TEXT_DIM, false);
        ty += 10;
        gfx.drawString(this.font, String.valueOf(stats.getSkillPoints()), sx + 8, ty, spColor, false);
    }

    // ── Таб: Характеристики ─────────────────────────────────────────────

    private void drawStatsTab(GuiGraphics gfx, PlayerStats stats) {
        int cx = MARGIN + SIDEBAR_W + MARGIN;
        int cr = this.width - MARGIN;
        int top = MARGIN + TAB_H + 8;

        gfx.drawString(this.font, "ХАРАКТЕРИСТИКИ", cx, top, COL_ACCENT, false);
        gfx.fill(cx, top + 12, cr, top + 13, COL_DIVIDER);

        int xpMax = stats.getLevel() * 100;
        int xpY = top + 20;
        gfx.drawString(this.font,
                "Опыт: " + stats.getExperience() + " / " + xpMax,
                cx, xpY, COL_TEXT, false);

        int barY = xpY + 12;
        int barH = 4;
        int barW = cr - cx;
        int filled = (int) ((long) barW * Math.min(stats.getExperience(), xpMax) / Math.max(1, xpMax));
        gfx.fill(cx, barY, cr, barY + barH, 0xFF1A1A1A);
        if (filled > 0) gfx.fill(cx, barY, cx + filled, barY + barH, COL_ACCENT);

        int rowY = top + 40;
        drawStatRow(gfx, "Сила",         "STR", stats.getStrength(),     cx, rowY + (ROW_H + ROW_GAP) * 0, cr, 0xFFFF6347);
        drawStatRow(gfx, "Ловкость",     "AGI", stats.getAgility(),      cx, rowY + (ROW_H + ROW_GAP) * 1, cr, 0xFF90EE90);
        drawStatRow(gfx, "Выносливость", "VIT", stats.getVitality(),     cx, rowY + (ROW_H + ROW_GAP) * 2, cr, 0xFFFF69B4);
        drawStatRow(gfx, "Интеллект",    "INT", stats.getIntelligence(), cx, rowY + (ROW_H + ROW_GAP) * 3, cr, 0xFF87CEEB);
    }

    private void drawStatRow(GuiGraphics gfx, String name, String code, int value,
                             int x, int y, int r, int nameColor) {
        gfx.fill(x, y, r, y + ROW_H, 0xFF0A0A0A);
        gfx.fill(x, y, x + 2, y + ROW_H, nameColor);
        int textY = y + (ROW_H - 8) / 2;
        gfx.drawString(this.font, code, x + 8, textY, COL_TEXT_DIM, false);
        gfx.drawString(this.font, name, x + 40, textY, nameColor, false);
        String val = String.valueOf(value);
        int valX = r - 8 - 16 - 8 - this.font.width(val);
        gfx.drawString(this.font, val, valX, textY, 0xFFFFFFFF, false);
    }

    // ── Таб: Навыки (древо) ─────────────────────────────────────────────

    private void drawSkillsTab(GuiGraphics gfx, PlayerStats stats, int mouseX, int mouseY) {
        int cx = MARGIN + SIDEBAR_W + MARGIN;
        int cr = this.width - MARGIN;
        int top = MARGIN + TAB_H + 8;

        String cls = stats.getPlayerClass();
        gfx.drawString(this.font, "ДРЕВО НАВЫКОВ — " + cls.toUpperCase(), cx, top, COL_ACCENT, false);
        gfx.fill(cx, top + 12, cr, top + 13, COL_DIVIDER);

        SkillTree tree = SkillTreeDefinitions.forClass(cls);
        if (tree == null) {
            gfx.drawString(this.font,
                    "Выберите класс, чтобы открыть древо навыков.",
                    cx, top + 24, COL_TEXT_DIM, false);
            return;
        }

        // Полотно
        int treeL = cx;
        int treeT = top + 24;
        int treeR = cr;
        int treeB = this.height - MARGIN - 32;
        gfx.fill(treeL, treeT, treeR, treeB, 0xFF050505);

        // Центрируем дерево в полотне: ноды имеют локальные координаты,
        // оригин — так чтобы стартовая нода оказалась в центре по X
        SkillNode startNode = tree.get(tree.startNodeId());
        int centerX = (treeL + treeR) / 2;
        int centerY = (treeT + treeB) / 2;
        this.treeOriginX = centerX - startNode.x();
        this.treeOriginY = centerY - startNode.y();

        Set<String> unlocked = stats.getUnlockedNodes();

        // 1) Связи
        for (SkillNode n : tree.nodes().values()) {
            for (String neighborId : n.neighbors()) {
                SkillNode m = tree.get(neighborId);
                if (m == null) continue;
                if (n.id().compareTo(m.id()) > 0) continue; // рисуем каждое ребро один раз
                boolean active = unlocked.contains(n.id()) && unlocked.contains(m.id());
                drawLink(gfx,
                        treeOriginX + n.x(), treeOriginY + n.y(),
                        treeOriginX + m.x(), treeOriginY + m.y(),
                        active ? COL_LINK_ACTIVE : COL_LINK_INACTIVE);
            }
        }

        // 2) Ноды + hover-детект
        SkillNode hovered = null;
        for (SkillNode n : tree.nodes().values()) {
            int nx = treeOriginX + n.x();
            int ny = treeOriginY + n.y();
            NodeState state = stateOf(n, unlocked, tree);
            drawNode(gfx, nx, ny, n, state);
            if (isHovered(mouseX, mouseY, nx, ny, n.type())) hovered = n;
        }

        // 3) Тултип
        if (hovered != null) {
            drawTooltip(gfx, hovered, stateOf(hovered, unlocked, tree), mouseX, mouseY, cr);
        }
    }

    private enum NodeState { UNLOCKED, AVAILABLE, LOCKED }

    private NodeState stateOf(SkillNode n, Set<String> unlocked, SkillTree tree) {
        if (unlocked.contains(n.id())) return NodeState.UNLOCKED;
        if (n.id().equals(tree.startNodeId())) return NodeState.AVAILABLE;
        for (String neighborId : n.neighbors()) {
            if (unlocked.contains(neighborId)) return NodeState.AVAILABLE;
        }
        // Также доступна, если у неё есть сосед, который её считает соседом и уже взят
        for (SkillNode other : tree.nodes().values()) {
            if (unlocked.contains(other.id()) && other.neighbors().contains(n.id())) {
                return NodeState.AVAILABLE;
            }
        }
        return NodeState.LOCKED;
    }

    private int nodeRadius(SkillNode.Type t) {
        return switch (t) {
            case START    -> 8;
            case MINOR    -> 5;
            case NOTABLE  -> 9;
            case KEYSTONE -> 11;
        };
    }

    private int nodeColor(NodeState state) {
        return switch (state) {
            case UNLOCKED  -> COL_NODE_UNLOCKED;
            case AVAILABLE -> COL_NODE_AVAILABLE;
            case LOCKED    -> COL_NODE_LOCKED;
        };
    }

    private void drawNode(GuiGraphics gfx, int x, int y, SkillNode n, NodeState state) {
        int r = nodeRadius(n.type());
        int col = nodeColor(state);
        // Тёмная рамка
        gfx.fill(x - r - 1, y - r - 1, x + r + 1, y + r + 1, 0xFF000000);
        // Заливка состояния
        gfx.fill(x - r, y - r, x + r, y + r, col);
        // Внутренняя точка: тёмная для locked/available, насыщенная для unlocked
        if (state == NodeState.UNLOCKED) {
            gfx.fill(x - r + 2, y - r + 2, x + r - 2, y + r - 2, 0xFF004020);
        } else {
            gfx.fill(x - r + 2, y - r + 2, x + r - 2, y + r - 2, 0xFF1A1A1A);
        }

        // Подпись для крупных нод
        if (n.type() != SkillNode.Type.MINOR) {
            int tw = this.font.width(n.title());
            gfx.drawString(this.font, n.title(),
                    x - tw / 2, y + r + 4,
                    state == NodeState.UNLOCKED ? COL_TEXT : COL_TEXT_DIM, false);
        }
    }

    private boolean isHovered(int mx, int my, int x, int y, SkillNode.Type t) {
        int r = nodeRadius(t) + 2;
        return mx >= x - r && mx <= x + r && my >= y - r && my <= y + r;
    }

    private void drawLink(GuiGraphics gfx, int x1, int y1, int x2, int y2, int color) {
        if (y1 == y2) {
            gfx.fill(Math.min(x1, x2), y1 - 1, Math.max(x1, x2), y1 + 1, color);
        } else if (x1 == x2) {
            gfx.fill(x1 - 1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2), color);
        } else {
            // Диагональ — рисуем L-образную линию (горизонталь + вертикаль)
            gfx.fill(Math.min(x1, x2), y1 - 1, Math.max(x1, x2), y1 + 1, color);
            gfx.fill(x2 - 1, Math.min(y1, y2), x2 + 1, Math.max(y1, y2), color);
        }
    }

    private void drawTooltip(GuiGraphics gfx, SkillNode n, NodeState state,
                             int mouseX, int mouseY, int screenR) {
        String title = n.title() + "  [" + typeLabel(n.type()) + "]";
        String cost  = state == NodeState.UNLOCKED ? "Активирована" : ("Стоимость: " + n.cost() + " очк.");
        String desc  = n.description();

        int w = Math.max(this.font.width(title), Math.max(this.font.width(cost), this.font.width(desc))) + 12;
        int h = 12 * 3 + 6;

        int tx = Math.min(mouseX + 10, screenR - w);
        int ty = mouseY - h - 4;
        if (ty < 4) ty = mouseY + 12;

        gfx.fill(tx, ty, tx + w, ty + h, 0xF0000000);
        gfx.fill(tx, ty, tx + w, ty + 1, COL_ACCENT);
        gfx.fill(tx, ty + h - 1, tx + w, ty + h, COL_ACCENT);

        gfx.drawString(this.font, title, tx + 6, ty + 4, COL_HEADER, false);
        int costColor = state == NodeState.UNLOCKED ? 0xFF00FF7F
                      : state == NodeState.AVAILABLE ? COL_TEXT : COL_TEXT_DIM;
        gfx.drawString(this.font, cost, tx + 6, ty + 16, costColor, false);
        gfx.drawString(this.font, desc, tx + 6, ty + 28, COL_TEXT, false);
    }

    private String typeLabel(SkillNode.Type t) {
        return switch (t) {
            case START    -> "Старт";
            case MINOR    -> "Малая";
            case NOTABLE  -> "Notable";
            case KEYSTONE -> "Keystone";
        };
    }

    // ── Клики ────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (currentTab == Tab.SKILLS && event.button() == 0) {
            PlayerStats stats = ClientStatsCache.get();
            SkillTree tree = SkillTreeDefinitions.forClass(stats.getPlayerClass());
            if (tree != null) {
                Set<String> unlocked = stats.getUnlockedNodes();
                int mx = (int) event.x();
                int my = (int) event.y();
                for (SkillNode n : tree.nodes().values()) {
                    int nx = treeOriginX + n.x();
                    int ny = treeOriginY + n.y();
                    if (isHovered(mx, my, nx, ny, n.type())) {
                        NodeState s = stateOf(n, unlocked, tree);
                        if (s == NodeState.AVAILABLE) {
                            ClientNetworkHandler.sendUnlockNode(n.id());
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
