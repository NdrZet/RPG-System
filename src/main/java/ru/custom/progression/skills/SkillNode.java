package ru.custom.progression.skills;

import java.util.Arrays;
import java.util.List;

/**
 * Узел древа навыков. Содержит стоимость, визуальные параметры, связи
 * и эффект, применяемый при активации. Эффекты применяются в {@link SkillEffects}.
 */
public final class SkillNode {

    public enum Type { START, MINOR, NOTABLE, KEYSTONE }

    private final String id;
    private final String title;
    private final String description;
    private final Type   type;
    private final int    cost;
    /** Соседи по графу (для проверки доступности). */
    private final List<String> neighbors;
    /** GUI-координаты (относительно полотна дерева). */
    private final int x;
    private final int y;

    public SkillNode(String id, String title, String description,
                     Type type, int cost, int x, int y, String... neighbors) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.cost = cost;
        this.x = x;
        this.y = y;
        this.neighbors = Arrays.asList(neighbors);
    }

    public String id()          { return id; }
    public String title()       { return title; }
    public String description() { return description; }
    public Type   type()        { return type; }
    public int    cost()        { return cost; }
    public List<String> neighbors() { return neighbors; }
    public int x() { return x; }
    public int y() { return y; }
}
