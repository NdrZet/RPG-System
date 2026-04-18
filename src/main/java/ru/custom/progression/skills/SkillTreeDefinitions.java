package ru.custom.progression.skills;

import java.util.HashMap;
import java.util.Map;

import static ru.custom.progression.skills.SkillNode.Type.*;

/**
 * Статически определённые древа навыков для всех классов.
 * Координаты (x, y) в пикселях относительно левого-верхнего угла полотна дерева.
 * <p>
 * Полное дерево для каждого класса: 3 ветки (каждая с Малыми пассивками,
 * Notable, Keystone и активным навыком). Стартовая нода — бесплатная.
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
    // 3 ветки: Ярость (влево), Стойкость (вправо), Командир (вниз)

    private static SkillTree buildWarrior() {
        SkillTree t = new SkillTree("warrior_start");
        t.add(new SkillNode("warrior_start", "Воин", "Начало пути Воина.",
                START, 0, 400, 250));

        // Ветка «Ярость» (влево)
        t.add(new SkillNode("w_fury_atk1", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 340, 250, "warrior_start"));
        t.add(new SkillNode("w_fury_atk2", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 280, 250, "w_fury_atk1"));
        t.add(new SkillNode("w_fury_crit", "+5% крит.шанс", "Шанс двойного урона 5%.",
                MINOR, 1, 220, 250, "w_fury_atk2"));
        t.add(new SkillNode("w_fury_berserk", "Берсерк",
                "При HP < 50%: +20% урона.",
                NOTABLE, 1, 160, 250, "w_fury_crit"));
        t.add(new SkillNode("w_fury_atk3", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 100, 250, "w_fury_berserk"));
        t.add(new SkillNode("w_fury_bloodthirst", "Жажда крови",
                "При убийстве: +15% макс.HP. Откл. естественную регенерацию.",
                KEYSTONE, 2, 40, 250, "w_fury_atk3"));

        // Ветка «Стойкость» (вправо)
        t.add(new SkillNode("w_guard_hp1", "+10 HP", "MAX_HEALTH +10.",
                MINOR, 1, 460, 250, "warrior_start"));
        t.add(new SkillNode("w_guard_hp2", "+10 HP", "MAX_HEALTH +10.",
                MINOR, 1, 520, 250, "w_guard_hp1"));
        t.add(new SkillNode("w_guard_armor", "+2 брони", "ARMOR +2.",
                MINOR, 1, 580, 250, "w_guard_hp2"));
        t.add(new SkillNode("w_guard_bastion", "Бастион",
                "Брони × 1.5 при стоянии на месте.",
                NOTABLE, 1, 640, 250, "w_guard_armor"));
        t.add(new SkillNode("w_guard_iron_skin", "Железная кожа",
                "Пассивно: +4 брони.",
                NOTABLE, 1, 580, 190, "w_guard_armor"));
        t.add(new SkillNode("w_guard_hp3", "+10 HP", "MAX_HEALTH +10.",
                MINOR, 1, 700, 250, "w_guard_bastion"));
        t.add(new SkillNode("w_guard_indestructible", "Несокрушимый",
                "Переживает смертельный удар (КД 5 мин).",
                KEYSTONE, 2, 760, 250, "w_guard_hp3"));

        // Ветка «Командир» (вниз)
        t.add(new SkillNode("w_cmd_cd1", "−5% КД клича",
                "Боевой клич перезаряжается быстрее.",
                MINOR, 1, 400, 310, "warrior_start"));
        t.add(new SkillNode("w_cmd_cd2", "−5% КД клича",
                "Боевой клич перезаряжается быстрее.",
                MINOR, 1, 400, 370, "w_cmd_cd1"));
        t.add(new SkillNode("w_cmd_dur", "+3 сек клича",
                "Увеличивает длительность Боевого клича.",
                MINOR, 1, 400, 430, "w_cmd_cd2"));
        t.add(new SkillNode("w_cmd_warcry", "Военный клич",
                "Боевой клич также накладывает Haste I.",
                NOTABLE, 1, 340, 430, "w_cmd_dur"));
        t.add(new SkillNode("w_cmd_shield", "Щит Воина",
                "Выдаёт предмет: Absorption IV на 8 сек, КД 120 сек.",
                ACTIVE, 2, 340, 490, "w_cmd_warcry"));
        t.add(new SkillNode("w_cmd_unshakable", "Непоколебимый",
                "Во время клича: иммунитет к замедлению и отбросу.",
                KEYSTONE, 2, 460, 430, "w_cmd_dur"));

        return t;
    }

    // ── МАГ ──────────────────────────────────────────────────────────────

    private static SkillTree buildMage() {
        SkillTree t = new SkillTree("mage_start");
        t.add(new SkillNode("mage_start", "Маг", "Начало пути Мага.",
                START, 0, 400, 250));

        // Ветка «Мудрость» (влево) — XP и очки
        t.add(new SkillNode("m_xp1", "+5% опыта", "XP +5%.",
                MINOR, 1, 340, 250, "mage_start"));
        t.add(new SkillNode("m_xp2", "+5% опыта", "XP +5%.",
                MINOR, 1, 280, 250, "m_xp1"));
        t.add(new SkillNode("m_xp3", "+15% опыта", "XP +15%.",
                MINOR, 1, 220, 250, "m_xp2"));
        t.add(new SkillNode("m_sage", "Мудрец",
                "Каждые 10 уровней: +2 очка характеристик.",
                NOTABLE, 1, 160, 250, "m_xp3"));
        t.add(new SkillNode("m_xp4", "+10% опыта", "XP +10%.",
                MINOR, 1, 100, 250, "m_sage"));
        t.add(new SkillNode("m_omniscience", "Всезнание",
                "Видишь HP мобов над их головой.",
                KEYSTONE, 2, 40, 250, "m_xp4"));

        // Ветка «Фортуна» (вправо) — LUCK и лут
        t.add(new SkillNode("m_luck1", "+5 удачи", "LUCK +5.",
                MINOR, 1, 460, 250, "mage_start"));
        t.add(new SkillNode("m_luck2", "+5 удачи", "LUCK +5.",
                MINOR, 1, 520, 250, "m_luck1"));
        t.add(new SkillNode("m_quality", "+5% качество лута",
                "LUCK +5 и шанс лучшего лута из облаков.",
                MINOR, 1, 580, 250, "m_luck2"));
        t.add(new SkillNode("m_golden_hands", "Золотые руки",
                "20% шанс двойного дропа при добыче блоков.",
                NOTABLE, 1, 640, 250, "m_quality"));
        t.add(new SkillNode("m_luck3", "+5 удачи", "LUCK +5.",
                MINOR, 1, 700, 250, "m_golden_hands"));
        t.add(new SkillNode("m_fate", "Рука судьбы",
                "Раз в 1 ч — Luck IV на 5 мин.",
                KEYSTONE, 2, 760, 250, "m_luck3"));

        // Ветка «Скорость» (вниз) — КД и мобильность
        t.add(new SkillNode("m_spd_cd1", "−5% КД свитка",
                "Свиток Удачи перезаряжается быстрее.",
                MINOR, 1, 400, 310, "mage_start"));
        t.add(new SkillNode("m_spd_cd2", "−5% КД свитка",
                "Свиток Удачи перезаряжается быстрее.",
                MINOR, 1, 400, 370, "m_spd_cd1"));
        t.add(new SkillNode("m_spd1", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, 400, 430, "m_spd_cd2"));
        t.add(new SkillNode("m_spd2", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, 340, 430, "m_spd1"));
        t.add(new SkillNode("m_blink", "Мерцание",
                "Раз в 90 сек: мгновенная телепортация на 10 блоков вперёд.",
                NOTABLE, 1, 280, 430, "m_spd2"));
        t.add(new SkillNode("m_teleport", "Телепорт",
                "Выдаёт предмет: телепортация к прицелу, КД 30 сек.",
                ACTIVE, 2, 280, 490, "m_blink"));
        t.add(new SkillNode("m_timeless", "Вне времени",
                "Переживает смертельный удар (КД 10 мин).",
                KEYSTONE, 2, 460, 430, "m_spd1"));

        return t;
    }

    // ── СЛЕДОПЫТ ─────────────────────────────────────────────────────────

    private static SkillTree buildRanger() {
        SkillTree t = new SkillTree("ranger_start");
        t.add(new SkillNode("ranger_start", "Следопыт", "Начало пути Следопыта.",
                START, 0, 400, 250));

        // Ветка «Акробат» (влево) — скорость
        t.add(new SkillNode("r_spd1", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, 340, 250, "ranger_start"));
        t.add(new SkillNode("r_spd2", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, 280, 250, "r_spd1"));
        t.add(new SkillNode("r_dodge", "+5% уклонение",
                "Шанс 5% полностью избежать урона.",
                MINOR, 1, 220, 250, "r_spd2"));
        t.add(new SkillNode("r_wind", "Призрак ветра",
                "При спринте: снаряды не попадают.",
                NOTABLE, 1, 160, 250, "r_dodge"));
        t.add(new SkillNode("r_spd3", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, 100, 250, "r_wind"));
        t.add(new SkillNode("r_elusive", "Неуловимый",
                "При получении урона: 1 сек невидимости.",
                KEYSTONE, 2, 40, 250, "r_spd3"));
        t.add(new SkillNode("r_atkspd", "+10% скор.атаки",
                "ATTACK_SPEED +10%.",
                MINOR, 1, 220, 190, "r_dodge"));

        // Ветка «Охота» (вправо) — урон
        t.add(new SkillNode("r_atk1", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 460, 250, "ranger_start"));
        t.add(new SkillNode("r_atk2", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 520, 250, "r_atk1"));
        t.add(new SkillNode("r_atk3", "+10% скор.стрелы",
                "Стрелы летят быстрее (не влияет на урон).",
                MINOR, 1, 580, 250, "r_atk2"));
        t.add(new SkillNode("r_eagle", "Острый глаз",
                "Полностью заряженный выстрел — крит.",
                NOTABLE, 1, 640, 250, "r_atk3"));
        t.add(new SkillNode("r_hunter", "Охотник",
                "Первый удар по новой цели: +50% урона.",
                NOTABLE, 1, 640, 190, "r_atk3"));
        t.add(new SkillNode("r_atk4", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, 700, 250, "r_eagle"));
        t.add(new SkillNode("r_shadow", "Охотник в тени",
                "Первый удар после 3 сек неподвижности: ×3 урона.",
                KEYSTONE, 2, 760, 250, "r_atk4"));

        // Ветка «Ловушки» (вниз)
        t.add(new SkillNode("r_trap_cd1", "−5% КД ловушки",
                "Ловушка перезаряжается быстрее.",
                MINOR, 1, 400, 310, "ranger_start"));
        t.add(new SkillNode("r_trap_cd2", "−5% КД ловушки",
                "Ловушка перезаряжается быстрее.",
                MINOR, 1, 400, 370, "r_trap_cd1"));
        t.add(new SkillNode("r_trap_dur", "+2 сек ловушки",
                "Увеличивает длительность облака ловушки.",
                MINOR, 1, 400, 430, "r_trap_cd2"));
        t.add(new SkillNode("r_trap_web", "Паутина",
                "Ловушка также обездвиживает на 1 сек.",
                NOTABLE, 1, 340, 430, "r_trap_dur"));
        t.add(new SkillNode("r_trap_smoke", "Дымовая завеса",
                "Выдаёт предмет: Blindness II врагам в радиусе 5, КД 60 сек.",
                ACTIVE, 2, 340, 490, "r_trap_web"));
        t.add(new SkillNode("r_trap_mine", "Минное поле",
                "Ловушка: +4 мгновенного урона при входе.",
                KEYSTONE, 2, 460, 430, "r_trap_dur"));

        return t;
    }

    // ── ЖРЕЦ ─────────────────────────────────────────────────────────────

    private static SkillTree buildPriest() {
        SkillTree t = new SkillTree("priest_start");
        t.add(new SkillNode("priest_start", "Жрец", "Начало пути Жреца.",
                START, 0, 400, 250));

        // Ветка «Исцеление» (влево) — посох
        t.add(new SkillNode("p_heal1", "+10% лечения",
                "Посох лечит на 10% больше.",
                MINOR, 1, 340, 250, "priest_start"));
        t.add(new SkillNode("p_heal2", "+10% лечения",
                "Посох лечит на 10% больше.",
                MINOR, 1, 280, 250, "p_heal1"));
        t.add(new SkillNode("p_staff_cd1", "−5% КД посоха",
                "Посох перезаряжается быстрее.",
                MINOR, 1, 220, 250, "p_heal2"));
        t.add(new SkillNode("p_great_heal", "Великое исцеление",
                "Посох лечит союзников в радиусе 3 на 50% эффекта.",
                NOTABLE, 1, 160, 250, "p_staff_cd1"));
        t.add(new SkillNode("p_staff_cd2", "−5% КД посоха",
                "Посох перезаряжается быстрее.",
                MINOR, 1, 100, 250, "p_great_heal"));
        t.add(new SkillNode("p_resurrection", "Воскрешение",
                "Переживает смертельный удар (КД 15 мин).",
                KEYSTONE, 2, 40, 250, "p_staff_cd2"));

        // Ветка «Защита» (вправо) — HP и иммунитеты
        t.add(new SkillNode("p_hp1", "+6 HP", "MAX_HEALTH +6.",
                MINOR, 1, 460, 250, "priest_start"));
        t.add(new SkillNode("p_hp2", "+6 HP", "MAX_HEALTH +6.",
                MINOR, 1, 520, 250, "p_hp1"));
        t.add(new SkillNode("p_poison_imm", "Иммун. яд",
                "Полный иммунитет к яду.",
                MINOR, 1, 580, 250, "p_hp2"));
        t.add(new SkillNode("p_faith_shield", "Щит Веры",
                "При HP < 30%: автоматически Absorption IV.",
                NOTABLE, 1, 640, 250, "p_poison_imm"));
        t.add(new SkillNode("p_hp3", "+6 HP", "MAX_HEALTH +6.",
                MINOR, 1, 700, 250, "p_faith_shield"));
        t.add(new SkillNode("p_stalwart", "Непоколебимый",
                "Иммунитет к огню, утоплению и Wither.",
                KEYSTONE, 2, 760, 250, "p_hp3"));

        // Ветка «Святость» (вниз) — аура и союзники
        t.add(new SkillNode("p_regen", "+50% регенерации",
                "Пассивная регенерация Жреца +50%.",
                MINOR, 1, 400, 310, "priest_start"));
        t.add(new SkillNode("p_regen2", "+1 HP / 5с",
                "Дополнительная пассивная регенерация.",
                MINOR, 1, 400, 370, "p_regen"));
        t.add(new SkillNode("p_luck1", "+3 удачи", "LUCK +3.",
                MINOR, 1, 400, 430, "p_regen2"));
        t.add(new SkillNode("p_cleanse", "Очищение",
                "Посох снимает все негативные эффекты.",
                NOTABLE, 1, 340, 430, "p_luck1"));
        t.add(new SkillNode("p_aura", "Аура Защиты",
                "Выдаёт предмет: Resistance I + Regen I союзникам, КД 120 сек.",
                ACTIVE, 2, 340, 490, "p_cleanse"));
        t.add(new SkillNode("p_devoted", "Посвящённый",
                "Пассивно: союзники в радиусе 15 получают +1 HP / 10с.",
                KEYSTONE, 2, 460, 430, "p_luck1"));

        return t;
    }
}
