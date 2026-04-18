package ru.custom.progression.skills;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Древо навыков одного класса: набор нод по id + id «стартовой» ноды.
 * Порядок LinkedHashMap фиксирован — используется для рендеринга.
 */
public final class SkillTree {

    private final String startNodeId;
    private final Map<String, SkillNode> nodes = new LinkedHashMap<>();

    public SkillTree(String startNodeId) {
        this.startNodeId = startNodeId;
    }

    public SkillTree add(SkillNode node) {
        nodes.put(node.id(), node);
        return this;
    }

    public SkillNode get(String id)   { return nodes.get(id); }
    public boolean   has(String id)   { return nodes.containsKey(id); }
    public String    startNodeId()    { return startNodeId; }
    public Map<String, SkillNode> nodes() { return Collections.unmodifiableMap(nodes); }
}
