package ru.custom.progression.skills;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Set;

/**
 * Применяет AttributeModifier'ы к игроку на основе набора активированных нод.
 * Вызывается из {@link ru.custom.progression.StatEffects#apply} после базовых бонусов.
 */
public final class SkillEffects {

    private SkillEffects() {}

    /** Идентификатор модификатора по id ноды — стабильный и уникальный. */
    private static Identifier modId(String nodeId) {
        return Identifier.fromNamespaceAndPath("progression", "skill_" + nodeId);
    }

    /**
     * Снимает все модификаторы нод (по списку известных id) и накатывает
     * модификаторы от активированных нод.
     */
    public static void apply(ServerPlayer player, String playerClass, Set<String> unlocked) {
        SkillTree tree = SkillTreeDefinitions.forClass(playerClass);
        if (tree == null) return;

        // Сначала снимаем все возможные модификаторы дерева — чтобы при сбросе
        // старые бонусы не оставались висеть.
        for (String id : tree.nodes().keySet()) {
            removeAll(player, modId(id));
        }

        // Накатываем модификаторы по активным нодам
        for (String id : unlocked) {
            SkillNode n = tree.get(id);
            if (n == null) continue;
            applyNode(player, n);
        }
    }

    /** Список всех атрибутов, на которые потенциально вешаем модификаторы. */
    private static final Holder<Attribute>[] ALL_ATTRS = new Holder[] {
            Attributes.ATTACK_DAMAGE,
            Attributes.MAX_HEALTH,
            Attributes.MOVEMENT_SPEED,
            Attributes.ATTACK_SPEED,
            Attributes.ARMOR,
            Attributes.LUCK,
    };

    private static void removeAll(ServerPlayer player, Identifier id) {
        for (Holder<Attribute> a : ALL_ATTRS) {
            AttributeInstance inst = player.getAttribute(a);
            if (inst != null) inst.removeModifier(id);
        }
    }

    private static void set(ServerPlayer p, Holder<Attribute> attr, String nodeId,
                             double value, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        Identifier id = modId(nodeId);
        inst.removeModifier(id);
        if (value != 0.0) {
            inst.addPermanentModifier(new AttributeModifier(id, value, op));
        }
    }

    /** Эффект каждой конкретной ноды — по её id. */
    private static void applyNode(ServerPlayer p, SkillNode n) {
        switch (n.id()) {
            // ── Воин: Ярость
            case "w_fury_atk1", "w_fury_atk2" ->
                    set(p, Attributes.ATTACK_DAMAGE, n.id(), 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "w_fury_crit" -> {
                // Крит обрабатывается event-хуком — атрибута не вешаем, но регистрируем через «пустой» модификатор,
                // чтобы apply/remove-логика была симметрична.
            }
            case "w_fury_berserk", "w_fury_bloodthirst" -> {
                // Обрабатываются в event-хуках (LivingEntity damage / kill).
            }

            // ── Воин: Стойкость
            case "w_guard_hp1", "w_guard_hp2" ->
                    set(p, Attributes.MAX_HEALTH, n.id(), 10.0, AttributeModifier.Operation.ADD_VALUE);
            case "w_guard_armor" ->
                    set(p, Attributes.ARMOR, n.id(), 2.0, AttributeModifier.Operation.ADD_VALUE);
            case "w_guard_bastion", "w_guard_indestructible" -> {
                // Event-хуки.
            }

            // ── Маг
            case "m_xp1", "m_xp2" -> {
                // XP-множитель — в registerMobKillXp (проверяем unlocked в ProgressionMod).
            }
            case "m_luck" ->
                    set(p, Attributes.LUCK, n.id(), 5.0, AttributeModifier.Operation.ADD_VALUE);
            case "m_sage" -> {
                // Усиление — повторный apply с множителем; упрощённо: игнорируем.
            }
            case "m_spd1", "m_spd2" ->
                    set(p, Attributes.MOVEMENT_SPEED, n.id(), 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "m_timeless" -> { /* event */ }

            // ── Следопыт
            case "r_spd1", "r_spd2" ->
                    set(p, Attributes.MOVEMENT_SPEED, n.id(), 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "r_atkspd" ->
                    set(p, Attributes.ATTACK_SPEED, n.id(), 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "r_atk1", "r_atk2" ->
                    set(p, Attributes.ATTACK_DAMAGE, n.id(), 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "r_hunter" -> { /* event */ }

            // ── Жрец
            case "p_hp1", "p_hp2" ->
                    set(p, Attributes.MAX_HEALTH, n.id(), 6.0, AttributeModifier.Operation.ADD_VALUE);
            case "p_regen" -> {
                // Усиление регенерации — флаг для ProgressionMod.registerPriestRegen.
            }
            case "p_luck1", "p_luck2" ->
                    set(p, Attributes.LUCK, n.id(), 3.0, AttributeModifier.Operation.ADD_VALUE);
            case "p_resurrection" -> { /* event */ }

            default -> {}
        }
    }
}
