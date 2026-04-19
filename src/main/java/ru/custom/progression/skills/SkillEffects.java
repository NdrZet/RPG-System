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
 * <p>
 * Все ноды, работающие не через атрибуты (крит, Берсерк, Охотник, Жажда крови,
 * Бастион, Минное поле и др.), обрабатываются в {@link SkillEventHooks}.
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

        // «Мудрец» — Малые пассивки дают на 25% больше эффекта (класс Маг).
        double minorMult = unlocked.contains("m_sage") ? 1.25 : 1.0;

        // Накатываем модификаторы по активным нодам
        for (String id : unlocked) {
            SkillNode n = tree.get(id);
            if (n == null) continue;
            applyNode(player, n, minorMult);
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

    /**
     * Эффект каждой конкретной ноды — по её id.
     * Параметр {@code minorMult} применяется к Малым пассивкам (флаг Мудреца).
     */
    private static void applyNode(ServerPlayer p, SkillNode n, double minorMult) {
        double m = (n.type() == SkillNode.Type.MINOR) ? minorMult : 1.0;

        switch (n.id()) {
            // ── Воин: Ярость
            case "w_fury_atk1", "w_fury_atk2", "w_fury_atk3" ->
                    set(p, Attributes.ATTACK_DAMAGE, n.id(), 0.03 * m,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "w_fury_crit" -> { /* event-хук: крит 5% */ }
            case "w_fury_berserk", "w_fury_bloodthirst" -> { /* event-хук */ }

            // ── Воин: Стойкость
            case "w_guard_hp1", "w_guard_hp2", "w_guard_hp3" ->
                    set(p, Attributes.MAX_HEALTH, n.id(), 10.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "w_guard_armor" ->
                    set(p, Attributes.ARMOR, n.id(), 2.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "w_guard_iron_skin" ->
                    set(p, Attributes.ARMOR, n.id(), 4.0,
                            AttributeModifier.Operation.ADD_VALUE);
            case "w_guard_bastion", "w_guard_indestructible" -> { /* event-хук */ }

            // ── Воин: Командир
            case "w_cmd_cd1", "w_cmd_cd2", "w_cmd_dur",
                 "w_cmd_warcry", "w_cmd_shield", "w_cmd_unshakable" -> {
                // КД/длительность клича и активный щит — в WarCryItem / ShieldItem.
            }

            // ── Воин: Твердыня
            case "w_fort_hp1", "w_fort_hp2", "w_fort_hp3" ->
                    set(p, Attributes.MAX_HEALTH, n.id(), 4.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "w_fort_res1", "w_fort_res2", "w_fort_flesh",
                 "w_fort_regen", "w_fort_eternal" -> { /* event-хук */ }

            // ── Воин: Доминирование
            case "w_dom_atk1", "w_dom_atk2" ->
                    set(p, Attributes.ATTACK_DAMAGE, n.id(), 0.05 * m,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "w_dom_hp" ->
                    set(p, Attributes.MAX_HEALTH, n.id(), 8.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "w_dom_critdmg", "w_dom_incarnate",
                 "w_dom_leap", "w_dom_absolute" -> { /* event-хук / актив */ }

            // ── Маг: Мудрость
            case "m_xp1", "m_xp2", "m_xp3", "m_xp4" -> { /* XP-множитель в хуке */ }
            case "m_sage", "m_omniscience" -> { /* event/логика */ }

            // ── Маг: Фортуна
            case "m_luck1", "m_luck2", "m_luck3" ->
                    set(p, Attributes.LUCK, n.id(), 5.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "m_quality" ->
                    set(p, Attributes.LUCK, n.id(), 5.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "m_golden_hands", "m_fate" -> { /* event-хук */ }

            // ── Маг: Скорость
            case "m_spd_cd1", "m_spd_cd2" -> { /* КД свитка — в LuckScrollItem */ }
            case "m_spd1", "m_spd2" ->
                    set(p, Attributes.MOVEMENT_SPEED, n.id(), 0.03 * m,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "m_blink", "m_teleport", "m_timeless" -> { /* event-хук / актив */ }

            // ── Маг: Алхимия
            case "m_alch_luck" ->
                    set(p, Attributes.LUCK, n.id(), 10.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "m_alch_potions", "m_alch_mine_xp", "m_grand_alchemist",
                 "m_alch_speed", "m_philosopher_stone" -> { /* event-хук */ }

            // ── Маг: Провидение
            case "m_prov_xp" -> { /* XP-множитель в хуке */ }
            case "m_prov_luck" ->
                    set(p, Attributes.LUCK, n.id(), 8.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "m_night_vision", "m_sensor", "m_prov_haste",
                 "m_fate_eye", "m_time_bubble", "m_master_chance" -> { /* event-хук / актив */ }

            // ── Следопыт: Акробат
            case "r_spd1", "r_spd2", "r_spd3" ->
                    set(p, Attributes.MOVEMENT_SPEED, n.id(), 0.03 * m,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "r_dodge", "r_wind", "r_elusive" -> { /* event-хук */ }
            case "r_atkspd" ->
                    set(p, Attributes.ATTACK_SPEED, n.id(), 0.10 * m,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            // ── Следопыт: Охота
            case "r_atk1", "r_atk2", "r_atk4" ->
                    set(p, Attributes.ATTACK_DAMAGE, n.id(), 0.03 * m,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "r_atk3" -> { /* скорость стрелы — в хуке на shoot */ }
            case "r_eagle", "r_hunter", "r_shadow" -> { /* event-хук */ }

            // ── Следопыт: Ловушки
            case "r_trap_cd1", "r_trap_cd2", "r_trap_dur",
                 "r_trap_web", "r_trap_smoke", "r_trap_mine" -> {
                // Модификация ловушки — в TrapItem / event-хуке.
            }

            // ── Следопыт: Выживание
            case "r_surv_hp" ->
                    set(p, Attributes.MAX_HEALTH, n.id(), 4.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "r_surv_night1", "r_surv_night2", "r_surv_fireimm",
                 "r_forest_ghost", "r_beast_hunt" -> { /* event-хук */ }

            // ── Следопыт: Арсенал
            case "r_ars_atkspd" ->
                    set(p, Attributes.ATTACK_SPEED, n.id(), 0.03 * m,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "r_ars_atk", "r_ars_poison", "r_weapon_master",
                 "r_fan_arrows", "r_unstoppable", "r_ars_smokecd" -> { /* event-хук / актив */ }

            // ── Жрец: Исцеление
            case "p_heal1", "p_heal2", "p_staff_cd1", "p_staff_cd2",
                 "p_great_heal", "p_resurrection" -> {
                // Всё читается в HealingStaffItem / event-хуке.
            }

            // ── Жрец: Защита
            case "p_hp1", "p_hp2", "p_hp3" ->
                    set(p, Attributes.MAX_HEALTH, n.id(), 6.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "p_poison_imm", "p_faith_shield", "p_stalwart" -> { /* event-хук */ }

            // ── Жрец: Святость
            case "p_regen", "p_regen2", "p_cleanse", "p_aura", "p_devoted" -> {
                // Реген/аура — в ProgressionMod.registerPriestRegen / item.
            }
            case "p_luck1" ->
                    set(p, Attributes.LUCK, n.id(), 3.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);

            // ── Жрец: Благодать
            case "p_grace_hp" ->
                    set(p, Attributes.MAX_HEALTH, n.id(), 3.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "p_grace_atk1", "p_grace_atk2", "p_grace_undead",
                 "p_grace_allyregen", "p_grace_weakness",
                 "p_holy_wrath", "p_guardian_angel" -> { /* event-хук */ }

            // ── Жрец: Мученичество
            case "p_mart_hp" ->
                    set(p, Attributes.MAX_HEALTH, n.id(), 10.0 * m,
                            AttributeModifier.Operation.ADD_VALUE);
            case "p_mart_regen", "p_mart_sacrifice", "p_last_word",
                 "p_sacrifice_light", "p_martyr",
                 "p_mart_resist", "p_mart_heal_bonus" -> { /* event-хук / актив */ }

            default -> {}
        }
    }
}
