package ru.custom.progression.skills;

import java.util.HashMap;
import java.util.Map;

import static ru.custom.progression.skills.SkillNode.Type.*;

/**
 * Статически определённые древа навыков для всех классов.
 * Координаты (x, y) в пикселях относительно левого-верхнего угла полотна дерева.
 */
public final class SkillTreeDefinitions {

    private static final Map<String, SkillTree> TREES = new HashMap<>();

    static {
        TREES.put("Воин",    buildWarrior());
        TREES.put("Маг",     buildMage());
        TREES.put("Следопыт", buildRanger());
        TREES.put("Жрец",    buildPriest());
    }

    private SkillTreeDefinitions() {}

    public static SkillTree forClass(String playerClass) {
        return TREES.get(playerClass);
    }

    public static boolean hasTree(String playerClass) {
        return TREES.containsKey(playerClass);
    }

    // ── ВОИН ─────────────────────────────────────────────────────────────

    private static SkillTree buildWarrior() {
        SkillTree t = new SkillTree("warrior_start");

        // Стартовый узел в центре
        t.add(new SkillNode("warrior_start", "Воин", "Начало пути Воина.",
                START, 0, 250, 150));

        // Ветка «Ярость» (влево)
        t.add(new SkillNode("w_fury_atk1", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 190, 150, "warrior_start"));
        t.add(new SkillNode("w_fury_atk2", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 130, 150, "w_fury_atk1"));
        t.add(new SkillNode("w_fury_crit", "+5% крит.шанс", "Шанс двойного урона 5%.",
                MINOR, 1, 70, 150, "w_fury_atk2"));
        t.add(new SkillNode("w_fury_berserk", "Берсерк",
                "При HP < 50%: +20% урона.",
                NOTABLE, 1, 70, 90, "w_fury_crit"));
        t.add(new SkillNode("w_fury_bloodthirst", "Жажда крови",
                "При убийстве: +15% макс.HP. Откл. естественную регенерацию.",
                KEYSTONE, 2, 70, 30, "w_fury_berserk"));

        // Ветка «Стойкость» (вправо)
        t.add(new SkillNode("w_guard_hp1", "+10 HP", "MAX_HEALTH +10.",
                MINOR, 1, 310, 150, "warrior_start"));
        t.add(new SkillNode("w_guard_hp2", "+10 HP", "MAX_HEALTH +10.",
                MINOR, 1, 370, 150, "w_guard_hp1"));
        t.add(new SkillNode("w_guard_armor", "+2 брони", "ARMOR +2.",
                MINOR, 1, 430, 150, "w_guard_hp2"));
        t.add(new SkillNode("w_guard_bastion", "Бастион",
                "Брони × 1.5 при стоянии на месте.",
                NOTABLE, 1, 430, 90, "w_guard_armor"));
        t.add(new SkillNode("w_guard_indestructible", "Несокрушимый",
                "Переживает смертельный удар (КД 5 мин).",
                KEYSTONE, 2, 430, 30, "w_guard_bastion"));

        return t;
    }

    // ── МАГ ──────────────────────────────────────────────────────────────

    private static SkillTree buildMage() {
        SkillTree t = new SkillTree("mage_start");
        t.add(new SkillNode("mage_start", "Маг", "Начало пути Мага.",
                START, 0, 250, 150));

        t.add(new SkillNode("m_xp1", "+5% опыта", "XP +5%.",
                MINOR, 1, 190, 150, "mage_start"));
        t.add(new SkillNode("m_xp2", "+5% опыта", "XP +5%.",
                MINOR, 1, 130, 150, "m_xp1"));
        t.add(new SkillNode("m_luck", "+5 удачи", "LUCK +5.",
                MINOR, 1, 70, 150, "m_xp2"));
        t.add(new SkillNode("m_sage", "Мудрец", "Все Мал.пассивки дают +25% эффекта.",
                NOTABLE, 1, 70, 90, "m_luck"));

        t.add(new SkillNode("m_spd1", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, 310, 150, "mage_start"));
        t.add(new SkillNode("m_spd2", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, 370, 150, "m_spd1"));
        t.add(new SkillNode("m_timeless", "Вне времени",
                "Переживает смертельный удар (КД 10 мин).",
                KEYSTONE, 2, 370, 90, "m_spd2"));

        return t;
    }

    // ── СЛЕДОПЫТ ─────────────────────────────────────────────────────────

    private static SkillTree buildRanger() {
        SkillTree t = new SkillTree("ranger_start");
        t.add(new SkillNode("ranger_start", "Следопыт", "Начало пути Следопыта.",
                START, 0, 250, 150));

        t.add(new SkillNode("r_spd1", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, 190, 150, "ranger_start"));
        t.add(new SkillNode("r_spd2", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, 130, 150, "r_spd1"));
        t.add(new SkillNode("r_atkspd", "+10% скор.атаки", "ATTACK_SPEED +10%.",
                MINOR, 1, 70, 150, "r_spd2"));

        t.add(new SkillNode("r_atk1", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 310, 150, "ranger_start"));
        t.add(new SkillNode("r_atk2", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 370, 150, "r_atk1"));
        t.add(new SkillNode("r_hunter", "Охотник",
                "Первый удар по цели: +50% урона.",
                NOTABLE, 1, 370, 90, "r_atk2"));

        return t;
    }

    // ── ЖРЕЦ ─────────────────────────────────────────────────────────────

    private static SkillTree buildPriest() {
        SkillTree t = new SkillTree("priest_start");
        t.add(new SkillNode("priest_start", "Жрец", "Начало пути Жреца.",
                START, 0, 250, 150));

        t.add(new SkillNode("p_hp1", "+6 HP", "MAX_HEALTH +6.",
                MINOR, 1, 190, 150, "priest_start"));
        t.add(new SkillNode("p_hp2", "+6 HP", "MAX_HEALTH +6.",
                MINOR, 1, 130, 150, "p_hp1"));
        t.add(new SkillNode("p_regen", "+50% регенерации",
                "Пассивная регенерация Жреца усилена на 50%.",
                NOTABLE, 1, 70, 150, "p_hp2"));

        t.add(new SkillNode("p_luck1", "+3 удачи", "LUCK +3.",
                MINOR, 1, 310, 150, "priest_start"));
        t.add(new SkillNode("p_luck2", "+3 удачи", "LUCK +3.",
                MINOR, 1, 370, 150, "p_luck1"));
        t.add(new SkillNode("p_resurrection", "Воскрешение",
                "Переживает смертельный удар (КД 15 мин).",
                KEYSTONE, 2, 370, 90, "p_luck2"));

        return t;
    }
}
