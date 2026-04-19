package ru.custom.progression.skills;

import java.util.HashMap;
import java.util.Map;

import static ru.custom.progression.skills.SkillNode.Type.*;

/**
 * Статически определённые древа навыков для всех классов.
 * <p>
 * Раскладка: стартовая нода в центре. От неё пять веток расходятся
 * под углами ~72°: NW (северо-запад), NE (северо-восток), E (восток),
 * W (запад), S (юг). Каждая ветка состоит из 5–6 нод и заканчивается
 * Keystone'ом или Активным навыком.
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

    // ── Геометрия ───────────────────────────────────────────────────────
    private static final int CX = 400;
    private static final int CY = 320;
    private static final int STEP_D = 38;   // шаг по диагонали (NW/NE)
    private static final int STEP_V = 42;   // шаг по вертикали (S)
    private static final int STEP_H = 52;   // шаг по горизонтали (E/W)

    // 5 базовых направлений
    private static int[] nw(int k) { return new int[] { CX - STEP_D * k, CY - STEP_D * k }; }
    private static int[] ne(int k) { return new int[] { CX + STEP_D * k, CY - STEP_D * k }; }
    private static int[] s (int k) { return new int[] { CX,              CY + STEP_V * k }; }
    private static int[] e (int k) { return new int[] { CX + STEP_H * k, CY               }; }
    private static int[] w (int k) { return new int[] { CX - STEP_H * k, CY               }; }

    private static int[] sSide(int k, int sideDx, int sideDy) {
        int[] p = s(k); return new int[] { p[0] + sideDx, p[1] + sideDy };
    }
    private static int[] eSide(int k, int dx, int dy) {
        int[] p = e(k); return new int[] { p[0] + dx, p[1] + dy };
    }
    private static int[] wSide(int k, int dx, int dy) {
        int[] p = w(k); return new int[] { p[0] + dx, p[1] + dy };
    }

    // ── ВОИН ─────────────────────────────────────────────────────────────

    private static SkillTree buildWarrior() {
        SkillTree t = new SkillTree("warrior_start");
        t.add(new SkillNode("warrior_start", "Воин", "Начало пути Воина.",
                START, 0, CX, CY));

        // Ветка 1 «Ярость» — NW
        int[] p; p = nw(1);
        t.add(new SkillNode("w_fury_atk1", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, p[0], p[1], "warrior_start"));
        p = nw(2);
        t.add(new SkillNode("w_fury_atk2", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, p[0], p[1], "w_fury_atk1"));
        p = nw(3);
        t.add(new SkillNode("w_fury_crit", "+5% крит.шанс", "Шанс двойного урона 5%.",
                MINOR, 1, p[0], p[1], "w_fury_atk2"));
        p = nw(4);
        t.add(new SkillNode("w_fury_berserk", "Берсерк",
                "При HP < 50%: +20% урона.",
                NOTABLE, 1, p[0], p[1], "w_fury_crit"));
        p = nw(5);
        t.add(new SkillNode("w_fury_atk3", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, p[0], p[1], "w_fury_berserk"));
        p = nw(6);
        t.add(new SkillNode("w_fury_bloodthirst", "Жажда крови",
                "При убийстве: +15% макс.HP. Откл. естественную регенерацию.",
                KEYSTONE, 2, p[0], p[1], "w_fury_atk3"));

        // Ветка 2 «Страж» — NE
        p = ne(1);
        t.add(new SkillNode("w_guard_hp1", "+5 HP", "MAX_HEALTH +5.",
                MINOR, 1, p[0], p[1], "warrior_start"));
        p = ne(2);
        t.add(new SkillNode("w_guard_hp2", "+5 HP", "MAX_HEALTH +5.",
                MINOR, 1, p[0], p[1], "w_guard_hp1"));
        p = ne(3);
        t.add(new SkillNode("w_guard_armor", "−3% урона", "Входящий урон ×0.97.",
                MINOR, 1, p[0], p[1], "w_guard_hp2"));
        p = ne(4);
        t.add(new SkillNode("w_guard_iron_skin", "Железная кожа",
                "ARMOR +4, TOUGHNESS +2.",
                NOTABLE, 1, p[0], p[1], "w_guard_armor"));
        p = ne(5);
        t.add(new SkillNode("w_guard_hp3", "+5 HP", "MAX_HEALTH +5.",
                MINOR, 1, p[0], p[1], "w_guard_iron_skin"));
        p = ne(6);
        t.add(new SkillNode("w_guard_indestructible", "Несокрушимый",
                "Переживает смертельный удар (КД 5 мин).",
                KEYSTONE, 2, p[0], p[1], "w_guard_hp3"));
        // Бастион — боковое ответвление от armor
        p = new int[] { ne(3)[0] + 60, ne(3)[1] - 10 };
        t.add(new SkillNode("w_guard_bastion", "Бастион",
                "Брони × 1.5 при стоянии на месте.",
                NOTABLE, 1, p[0], p[1], "w_guard_armor"));

        // Ветка 3 «Командир» — S
        p = s(1);
        t.add(new SkillNode("w_cmd_cd1", "−5% КД клича",
                "Боевой клич перезаряжается быстрее.",
                MINOR, 1, p[0], p[1], "warrior_start"));
        p = s(2);
        t.add(new SkillNode("w_cmd_cd2", "−5% КД клича",
                "Боевой клич перезаряжается быстрее.",
                MINOR, 1, p[0], p[1], "w_cmd_cd1"));
        p = s(3);
        t.add(new SkillNode("w_cmd_dur", "+3 сек клича",
                "Увеличивает длительность Боевого клича.",
                MINOR, 1, p[0], p[1], "w_cmd_cd2"));
        p = sSide(3, -60, 60);
        t.add(new SkillNode("w_cmd_warcry", "Военный клич",
                "Боевой клич также накладывает Haste I.",
                NOTABLE, 1, p[0], p[1], "w_cmd_dur"));
        p = sSide(3, -60, 120);
        t.add(new SkillNode("w_cmd_shield", "Щит Воина",
                "Актив: Absorption IV на 8 сек, КД 120 сек.",
                ACTIVE, 2, p[0], p[1], "w_cmd_warcry"));
        p = sSide(3, 60, 60);
        t.add(new SkillNode("w_cmd_unshakable", "Непоколебимый",
                "Во время клича: иммун. к замедлению, отбросу, слабости.",
                KEYSTONE, 2, p[0], p[1], "w_cmd_dur"));

        // Ветка 4 «Твердыня» — E
        p = e(1);
        t.add(new SkillNode("w_fort_hp1", "+4 HP", "MAX_HEALTH +4.",
                MINOR, 1, p[0], p[1], "warrior_start"));
        p = e(2);
        t.add(new SkillNode("w_fort_hp2", "+4 HP", "MAX_HEALTH +4.",
                MINOR, 1, p[0], p[1], "w_fort_hp1"));
        p = e(3);
        t.add(new SkillNode("w_fort_res1", "−2% урона", "Входящий урон ×0.98.",
                MINOR, 1, p[0], p[1], "w_fort_hp2"));
        p = e(4);
        t.add(new SkillNode("w_fort_flesh", "Стена из плоти",
                "Стоя: +2% HP в сек.",
                NOTABLE, 1, p[0], p[1], "w_fort_res1"));
        p = e(5);
        t.add(new SkillNode("w_fort_hp3", "+4 HP", "MAX_HEALTH +4.",
                MINOR, 1, p[0], p[1], "w_fort_flesh"));
        p = e(6);
        t.add(new SkillNode("w_fort_eternal", "Вечный страж",
                "Полная броня: ARMOR+6, KB-сопр. +50%, реген ×2.",
                KEYSTONE, 2, p[0], p[1], "w_fort_hp3"));
        // боковое — Пасс.реген и второй резист
        p = eSide(3, 0, 50);
        t.add(new SkillNode("w_fort_res2", "−2% урона", "Входящий урон ×0.98.",
                MINOR, 1, p[0], p[1], "w_fort_res1"));
        p = eSide(3, 0, 100);
        t.add(new SkillNode("w_fort_regen", "+1 HP / 3с",
                "Пассивная регенерация.",
                MINOR, 1, p[0], p[1], "w_fort_res2"));

        // Ветка 5 «Доминирование» — W
        p = w(1);
        t.add(new SkillNode("w_dom_atk1", "+5% урона", "ATTACK_DAMAGE +5%.",
                MINOR, 1, p[0], p[1], "warrior_start"));
        p = w(2);
        t.add(new SkillNode("w_dom_atk2", "+5% урона", "ATTACK_DAMAGE +5%.",
                MINOR, 1, p[0], p[1], "w_dom_atk1"));
        p = w(3);
        t.add(new SkillNode("w_dom_critdmg", "+5% крит.урона",
                "Критический удар ×2.1 вместо ×2.0.",
                MINOR, 1, p[0], p[1], "w_dom_atk2"));
        p = w(4);
        t.add(new SkillNode("w_dom_incarnate", "Воплощение войны",
                "Во время клича удары поджигают цель на 3 сек.",
                NOTABLE, 1, p[0], p[1], "w_dom_critdmg"));
        p = w(5);
        t.add(new SkillNode("w_dom_leap", "Прыжок Воина",
                "Актив: прыжок вперёд + AoE 4 HP, КД 15 сек.",
                ACTIVE, 2, p[0], p[1], "w_dom_incarnate"));
        p = w(6);
        t.add(new SkillNode("w_dom_absolute", "Абсолютный боец",
                "5 убийств без урона → Strength III на 10 сек.",
                KEYSTONE, 2, p[0], p[1], "w_dom_leap"));
        p = wSide(3, 0, 60);
        t.add(new SkillNode("w_dom_hp", "+8 HP", "MAX_HEALTH +8.",
                MINOR, 1, p[0], p[1], "w_dom_critdmg"));

        return t;
    }

    // ── МАГ ──────────────────────────────────────────────────────────────

    private static SkillTree buildMage() {
        SkillTree t = new SkillTree("mage_start");
        t.add(new SkillNode("mage_start", "Маг", "Начало пути Мага.",
                START, 0, CX, CY));

        // Ветка 1 «Мудрость» — NW
        int[] p;
        p = nw(1);
        t.add(new SkillNode("m_xp1", "+5% опыта", "XP +5%.",
                MINOR, 1, p[0], p[1], "mage_start"));
        p = nw(2);
        t.add(new SkillNode("m_xp2", "+5% опыта", "XP +5%.",
                MINOR, 1, p[0], p[1], "m_xp1"));
        p = nw(3);
        t.add(new SkillNode("m_xp3", "+15% опыта", "XP +15%.",
                MINOR, 1, p[0], p[1], "m_xp2"));
        p = nw(4);
        t.add(new SkillNode("m_sage", "Мудрец",
                "Каждые 10 уровней: +2 очка характеристик.",
                NOTABLE, 1, p[0], p[1], "m_xp3"));
        p = nw(5);
        t.add(new SkillNode("m_xp4", "+10% опыта", "XP +10%.",
                MINOR, 1, p[0], p[1], "m_sage"));
        p = nw(6);
        t.add(new SkillNode("m_omniscience", "Всезнание",
                "Видишь HP мобов над их головой.",
                KEYSTONE, 2, p[0], p[1], "m_xp4"));

        // Ветка 2 «Фортуна» — NE
        p = ne(1);
        t.add(new SkillNode("m_luck1", "+5 удачи", "LUCK +5.",
                MINOR, 1, p[0], p[1], "mage_start"));
        p = ne(2);
        t.add(new SkillNode("m_luck2", "+5 удачи", "LUCK +5.",
                MINOR, 1, p[0], p[1], "m_luck1"));
        p = ne(3);
        t.add(new SkillNode("m_quality", "+5% качество лута",
                "LUCK +5 и шанс лучшего лута.",
                MINOR, 1, p[0], p[1], "m_luck2"));
        p = ne(4);
        t.add(new SkillNode("m_golden_hands", "Золотые руки",
                "20% шанс двойного дропа при добыче блоков.",
                NOTABLE, 1, p[0], p[1], "m_quality"));
        p = ne(5);
        t.add(new SkillNode("m_luck3", "+5 удачи", "LUCK +5.",
                MINOR, 1, p[0], p[1], "m_golden_hands"));
        p = ne(6);
        t.add(new SkillNode("m_fate", "Рука судьбы",
                "Раз в 1 ч — Luck IV на 5 мин.",
                KEYSTONE, 2, p[0], p[1], "m_luck3"));

        // Ветка 3 «Скорость» — S
        p = s(1);
        t.add(new SkillNode("m_spd_cd1", "−5% КД свитка",
                "Свиток Удачи перезаряжается быстрее.",
                MINOR, 1, p[0], p[1], "mage_start"));
        p = s(2);
        t.add(new SkillNode("m_spd_cd2", "−5% КД свитка",
                "Свиток Удачи перезаряжается быстрее.",
                MINOR, 1, p[0], p[1], "m_spd_cd1"));
        p = s(3);
        t.add(new SkillNode("m_spd1", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, p[0], p[1], "m_spd_cd2"));
        p = sSide(3, -60, 60);
        t.add(new SkillNode("m_spd2", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, p[0], p[1], "m_spd1"));
        p = sSide(3, -60, 120);
        t.add(new SkillNode("m_blink", "Мерцание",
                "Раз в 90 сек: ПКМ свитком → телепорт на 10 блоков.",
                NOTABLE, 1, p[0], p[1], "m_spd2"));
        p = sSide(3, -120, 120);
        t.add(new SkillNode("m_teleport", "Телепорт",
                "Актив: телепорт к прицелу 30 блоков, КД 30 сек.",
                ACTIVE, 2, p[0], p[1], "m_blink"));
        p = sSide(3, 60, 60);
        t.add(new SkillNode("m_timeless", "Вне времени",
                "Переживает смертельный удар (КД 10 мин).",
                KEYSTONE, 2, p[0], p[1], "m_spd1"));

        // Ветка 4 «Алхимия» — E
        p = e(1);
        t.add(new SkillNode("m_alch_luck", "+10 удачи", "LUCK +10.",
                MINOR, 1, p[0], p[1], "mage_start"));
        p = e(2);
        t.add(new SkillNode("m_alch_potions", "+20% длит.зелий",
                "Эффекты зелий длятся на 20% дольше.",
                MINOR, 1, p[0], p[1], "m_alch_luck"));
        p = e(3);
        t.add(new SkillNode("m_alch_mine_xp", "+5% XP с добычи",
                "XP от добычи блоков ×1.05.",
                MINOR, 1, p[0], p[1], "m_alch_potions"));
        p = e(4);
        t.add(new SkillNode("m_grand_alchemist", "Великий алхимик",
                "Любое выпитое зелье даёт Luck I на 30 сек.",
                NOTABLE, 1, p[0], p[1], "m_alch_mine_xp"));
        p = e(5);
        t.add(new SkillNode("m_alch_speed", "+3% скорости при Luck",
                "Пока активен Luck: MOVEMENT_SPEED +3%.",
                MINOR, 1, p[0], p[1], "m_grand_alchemist"));
        p = e(6);
        t.add(new SkillNode("m_philosopher_stone", "Философский камень",
                "10% шанс превратить дроп руды в алмаз/изумруд (КД 15 мин).",
                KEYSTONE, 2, p[0], p[1], "m_alch_speed"));

        // Ветка 5 «Провидение» — W
        p = w(1);
        t.add(new SkillNode("m_prov_xp", "+15% XP", "Весь XP ×1.15.",
                MINOR, 1, p[0], p[1], "mage_start"));
        p = w(2);
        t.add(new SkillNode("m_prov_luck", "+8 удачи", "LUCK +8.",
                MINOR, 1, p[0], p[1], "m_prov_xp"));
        p = w(3);
        t.add(new SkillNode("m_night_vision", "Ночное зрение",
                "Постоянный Night Vision.",
                MINOR, 1, p[0], p[1], "m_prov_luck"));
        p = w(4);
        t.add(new SkillNode("m_sensor", "Сенсор мобов",
                "Мобы в р.10 подсвечены сквозь стены.",
                MINOR, 1, p[0], p[1], "m_night_vision"));
        p = w(5);
        t.add(new SkillNode("m_fate_eye", "Око судьбы",
                "Видеть сундуки/порталы в р.20 сквозь стены.",
                NOTABLE, 1, p[0], p[1], "m_sensor"));
        p = w(6);
        t.add(new SkillNode("m_time_bubble", "Временной пузырь",
                "Актив: мобы р.6 → Slowness V 4с, +Speed II Магу, КД 45 сек.",
                ACTIVE, 2, p[0], p[1], "m_fate_eye"));
        // боковое — Haste I постоянно
        p = wSide(3, 0, 60);
        t.add(new SkillNode("m_prov_haste", "+5% скор. добычи",
                "Постоянный Haste I.",
                MINOR, 1, p[0], p[1], "m_night_vision"));
        p = wSide(3, 0, 120);
        t.add(new SkillNode("m_master_chance", "Мастер случая",
                "Пока Рука судьбы на КД: +5 очков навыков раз в 30 мин.",
                KEYSTONE, 2, p[0], p[1], "m_prov_haste"));

        return t;
    }

    // ── СЛЕДОПЫТ ─────────────────────────────────────────────────────────

    private static SkillTree buildRanger() {
        SkillTree t = new SkillTree("ranger_start");
        t.add(new SkillNode("ranger_start", "Следопыт", "Начало пути Следопыта.",
                START, 0, CX, CY));

        // Ветка 1 «Акробат» — NW
        int[] p;
        p = nw(1);
        t.add(new SkillNode("r_spd1", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, p[0], p[1], "ranger_start"));
        p = nw(2);
        t.add(new SkillNode("r_spd2", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, p[0], p[1], "r_spd1"));
        p = nw(3);
        t.add(new SkillNode("r_dodge", "+5% уклонение",
                "Шанс 5% полностью избежать урона.",
                MINOR, 1, p[0], p[1], "r_spd2"));
        p = new int[] { nw(3)[0] - 60, nw(3)[1] + 40 };
        t.add(new SkillNode("r_atkspd", "+10% скор.атаки",
                "ATTACK_SPEED +10%.",
                MINOR, 1, p[0], p[1], "r_dodge"));
        p = nw(4);
        t.add(new SkillNode("r_wind", "Призрак ветра",
                "При спринте: снаряды не попадают.",
                NOTABLE, 1, p[0], p[1], "r_dodge"));
        p = nw(5);
        t.add(new SkillNode("r_spd3", "+3% скорости", "MOVEMENT_SPEED +3%.",
                MINOR, 1, p[0], p[1], "r_wind"));
        p = nw(6);
        t.add(new SkillNode("r_elusive", "Неуловимый",
                "При получении урона: 1 сек невидимости.",
                KEYSTONE, 2, p[0], p[1], "r_spd3"));

        // Ветка 2 «Охота» — NE
        p = ne(1);
        t.add(new SkillNode("r_atk1", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, p[0], p[1], "ranger_start"));
        p = ne(2);
        t.add(new SkillNode("r_atk2", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, p[0], p[1], "r_atk1"));
        p = ne(3);
        t.add(new SkillNode("r_atk3", "+10% скор.стрелы",
                "Стрелы летят быстрее.",
                MINOR, 1, p[0], p[1], "r_atk2"));
        p = ne(4);
        t.add(new SkillNode("r_eagle", "Острый глаз",
                "Полностью заряженный выстрел — крит.",
                NOTABLE, 1, p[0], p[1], "r_atk3"));
        p = new int[] { ne(4)[0] + 60, ne(4)[1] - 40 };
        t.add(new SkillNode("r_hunter", "Охотник",
                "Первый удар по новой цели: +50% урона.",
                NOTABLE, 1, p[0], p[1], "r_eagle"));
        p = ne(5);
        t.add(new SkillNode("r_atk4", "+3% урона", "ATTACK_DAMAGE +3%.",
                MINOR, 1, p[0], p[1], "r_eagle"));
        p = ne(6);
        t.add(new SkillNode("r_shadow", "Охотник в тени",
                "Первый удар после 3 сек неподвижности: ×3 урона.",
                KEYSTONE, 2, p[0], p[1], "r_atk4"));

        // Ветка 3 «Ловушки» — S
        p = s(1);
        t.add(new SkillNode("r_trap_cd1", "−5% КД ловушки",
                "Ловушка перезаряжается быстрее.",
                MINOR, 1, p[0], p[1], "ranger_start"));
        p = s(2);
        t.add(new SkillNode("r_trap_cd2", "−5% КД ловушки",
                "Ловушка перезаряжается быстрее.",
                MINOR, 1, p[0], p[1], "r_trap_cd1"));
        p = s(3);
        t.add(new SkillNode("r_trap_dur", "+2 сек ловушки",
                "Увеличивает длительность облака ловушки.",
                MINOR, 1, p[0], p[1], "r_trap_cd2"));
        p = sSide(3, -60, 60);
        t.add(new SkillNode("r_trap_web", "Паутина",
                "Ловушка также обездвиживает на 1 сек.",
                NOTABLE, 1, p[0], p[1], "r_trap_dur"));
        p = sSide(3, -60, 120);
        t.add(new SkillNode("r_trap_smoke", "Дымовая завеса",
                "Актив: Blindness II в радиусе 5, КД 60 сек.",
                ACTIVE, 2, p[0], p[1], "r_trap_web"));
        p = sSide(3, 60, 60);
        t.add(new SkillNode("r_trap_mine", "Минное поле",
                "Ловушка: +4 мгновенного урона при входе.",
                KEYSTONE, 2, p[0], p[1], "r_trap_dur"));

        // Ветка 4 «Выживание» — E
        p = e(1);
        t.add(new SkillNode("r_surv_night1", "+5% урона ночью",
                "Ночью ATK ×1.05.",
                MINOR, 1, p[0], p[1], "ranger_start"));
        p = e(2);
        t.add(new SkillNode("r_surv_night2", "+5% урона ночью",
                "Ночью ATK ×1.05 (суммируется).",
                MINOR, 1, p[0], p[1], "r_surv_night1"));
        p = e(3);
        t.add(new SkillNode("r_surv_fireimm", "Иммун. огонь",
                "Огонь тушится мгновенно, урон от огня обнуляется.",
                MINOR, 1, p[0], p[1], "r_surv_night2"));
        p = e(4);
        t.add(new SkillNode("r_forest_ghost", "Лесной призрак",
                "Ночью, стоя/присев — Invisibility.",
                NOTABLE, 1, p[0], p[1], "r_surv_fireimm"));
        p = e(5);
        t.add(new SkillNode("r_surv_hp", "+4 HP", "MAX_HEALTH +4.",
                MINOR, 1, p[0], p[1], "r_forest_ghost"));
        p = e(6);
        t.add(new SkillNode("r_beast_hunt", "Зверь охоты",
                "Ночью стрелы +50% урона; днём — Slowness II на 2с.",
                KEYSTONE, 2, p[0], p[1], "r_surv_hp"));

        // Ветка 5 «Арсенал» — W
        p = w(1);
        t.add(new SkillNode("r_ars_atk", "+7% урона луком",
                "Стрелы наносят на 7% больше урона.",
                MINOR, 1, p[0], p[1], "ranger_start"));
        p = w(2);
        t.add(new SkillNode("r_ars_atkspd", "+3% скор.атаки",
                "ATTACK_SPEED +3%.",
                MINOR, 1, p[0], p[1], "r_ars_atk"));
        p = w(3);
        t.add(new SkillNode("r_ars_poison", "Отравленные стрелы",
                "Стрелы накладывают Poison I на 3 сек.",
                MINOR, 1, p[0], p[1], "r_ars_atkspd"));
        p = w(4);
        t.add(new SkillNode("r_weapon_master", "Мастер оружия",
                "Смена на ближний бой → Haste I на 5с.",
                NOTABLE, 1, p[0], p[1], "r_ars_poison"));
        p = w(5);
        t.add(new SkillNode("r_fan_arrows", "Веер стрел",
                "Актив: 5 стрел веером 60% урона, КД 25 сек.",
                ACTIVE, 2, p[0], p[1], "r_weapon_master"));
        p = w(6);
        t.add(new SkillNode("r_unstoppable", "Неудержимый охотник",
                "Попадание стрелой по мобу → −1с КД всех активов.",
                KEYSTONE, 2, p[0], p[1], "r_fan_arrows"));
        p = wSide(3, 0, 60);
        t.add(new SkillNode("r_ars_smokecd", "−10% КД бомбы",
                "Дымовая Завеса перезаряжается быстрее.",
                MINOR, 1, p[0], p[1], "r_ars_poison"));

        return t;
    }

    // ── ЖРЕЦ ─────────────────────────────────────────────────────────────

    private static SkillTree buildPriest() {
        SkillTree t = new SkillTree("priest_start");
        t.add(new SkillNode("priest_start", "Жрец", "Начало пути Жреца.",
                START, 0, CX, CY));

        // Ветка 1 «Исцеление» — NW
        int[] p;
        p = nw(1);
        t.add(new SkillNode("p_heal1", "+10% лечения",
                "Посох лечит на 10% больше.",
                MINOR, 1, p[0], p[1], "priest_start"));
        p = nw(2);
        t.add(new SkillNode("p_heal2", "+10% лечения",
                "Посох лечит на 10% больше.",
                MINOR, 1, p[0], p[1], "p_heal1"));
        p = nw(3);
        t.add(new SkillNode("p_staff_cd1", "−5% КД посоха",
                "Посох перезаряжается быстрее.",
                MINOR, 1, p[0], p[1], "p_heal2"));
        p = nw(4);
        t.add(new SkillNode("p_great_heal", "Великое исцеление",
                "Посох лечит союзников в р.3 на 50%.",
                NOTABLE, 1, p[0], p[1], "p_staff_cd1"));
        p = nw(5);
        t.add(new SkillNode("p_staff_cd2", "−5% КД посоха",
                "Посох перезаряжается быстрее.",
                MINOR, 1, p[0], p[1], "p_great_heal"));
        p = nw(6);
        t.add(new SkillNode("p_resurrection", "Воскрешение",
                "Переживает смертельный удар (КД 15 мин).",
                KEYSTONE, 2, p[0], p[1], "p_staff_cd2"));

        // Ветка 2 «Защита» — NE
        p = ne(1);
        t.add(new SkillNode("p_hp1", "+5 HP", "MAX_HEALTH +5.",
                MINOR, 1, p[0], p[1], "priest_start"));
        p = ne(2);
        t.add(new SkillNode("p_hp2", "+5 HP", "MAX_HEALTH +5.",
                MINOR, 1, p[0], p[1], "p_hp1"));
        p = ne(3);
        t.add(new SkillNode("p_poison_imm", "Иммун. яд",
                "Полный иммунитет к яду.",
                MINOR, 1, p[0], p[1], "p_hp2"));
        p = ne(4);
        t.add(new SkillNode("p_faith_shield", "Щит Веры",
                "При HP < 30%: автоматически Absorption IV.",
                NOTABLE, 1, p[0], p[1], "p_poison_imm"));
        p = ne(5);
        t.add(new SkillNode("p_hp3", "+5 HP", "MAX_HEALTH +5.",
                MINOR, 1, p[0], p[1], "p_faith_shield"));
        p = ne(6);
        t.add(new SkillNode("p_stalwart", "Непоколебимый",
                "Иммун. к огню, утоплению и Wither.",
                KEYSTONE, 2, p[0], p[1], "p_hp3"));

        // Ветка 3 «Святость» — S
        p = s(1);
        t.add(new SkillNode("p_regen", "+50% регенерации",
                "Пассивная регенерация Жреца +50%.",
                MINOR, 1, p[0], p[1], "priest_start"));
        p = s(2);
        t.add(new SkillNode("p_regen2", "+1 HP / 5с",
                "Дополнительная пассивная регенерация.",
                MINOR, 1, p[0], p[1], "p_regen"));
        p = s(3);
        t.add(new SkillNode("p_luck1", "+3 удачи", "LUCK +3.",
                MINOR, 1, p[0], p[1], "p_regen2"));
        p = sSide(3, -60, 60);
        t.add(new SkillNode("p_cleanse", "Очищение",
                "Посох снимает все негативные эффекты.",
                NOTABLE, 1, p[0], p[1], "p_luck1"));
        p = sSide(3, -60, 120);
        t.add(new SkillNode("p_aura", "Аура Защиты",
                "Актив: Resistance I + Regen I союзникам, КД 120 сек.",
                ACTIVE, 2, p[0], p[1], "p_cleanse"));
        p = sSide(3, 60, 60);
        t.add(new SkillNode("p_devoted", "Посвящённый",
                "Пассивно: союзники в р.15 получают +1 HP / 10с.",
                KEYSTONE, 2, p[0], p[1], "p_luck1"));

        // Ветка 4 «Благодать» — E
        p = e(1);
        t.add(new SkillNode("p_grace_atk1", "+5% ATK союзникам",
                "Союзники в р.10: +5% урона.",
                MINOR, 1, p[0], p[1], "priest_start"));
        p = e(2);
        t.add(new SkillNode("p_grace_atk2", "+5% ATK союзникам",
                "Союзники в р.10: +5% урона (суммируется).",
                MINOR, 1, p[0], p[1], "p_grace_atk1"));
        p = e(3);
        t.add(new SkillNode("p_grace_undead", "−10% урон от нежити",
                "Урон от Undead ×0.90.",
                MINOR, 1, p[0], p[1], "p_grace_atk2"));
        p = e(4);
        t.add(new SkillNode("p_holy_wrath", "Священный гнев",
                "Посох: ближайший враг в р.4 → −2 HP + Weakness II.",
                NOTABLE, 1, p[0], p[1], "p_grace_undead"));
        p = e(5);
        t.add(new SkillNode("p_grace_hp", "+3 HP", "MAX_HEALTH +3.",
                MINOR, 1, p[0], p[1], "p_holy_wrath"));
        p = e(6);
        t.add(new SkillNode("p_guardian_angel", "Ангел-хранитель",
                "30% урона союзника с мин. HP → Жрецу.",
                KEYSTONE, 2, p[0], p[1], "p_grace_hp"));
        p = eSide(3, 0, 50);
        t.add(new SkillNode("p_grace_allyregen", "+1 HP/5с союзникам",
                "Союзники в р.8 регенерируют +1 HP / 5с.",
                MINOR, 1, p[0], p[1], "p_grace_undead"));
        p = eSide(3, 0, 100);
        t.add(new SkillNode("p_grace_weakness", "Слабость врагов",
                "Мобы в р.5 от Жреца: пассивно Weakness I.",
                MINOR, 1, p[0], p[1], "p_grace_allyregen"));

        // Ветка 5 «Мученичество» — W
        p = w(1);
        t.add(new SkillNode("p_mart_hp", "+10 HP", "MAX_HEALTH +10.",
                MINOR, 1, p[0], p[1], "priest_start"));
        p = w(2);
        t.add(new SkillNode("p_mart_regen", "+2 HP/5с",
                "+2 HP каждые 5 сек (суммируется).",
                MINOR, 1, p[0], p[1], "p_mart_hp"));
        p = w(3);
        t.add(new SkillNode("p_mart_sacrifice", "Жертвенное лечение",
                "Посох: −1 HP Жрецу → +2 HP союзникам в р.3.",
                MINOR, 1, p[0], p[1], "p_mart_regen"));
        p = w(4);
        t.add(new SkillNode("p_last_word", "Последнее слово",
                "HP<10%: союзники р.12 → Regen III + Str I на 10с.",
                NOTABLE, 1, p[0], p[1], "p_mart_sacrifice"));
        p = w(5);
        t.add(new SkillNode("p_sacrifice_light", "Жертвенное сияние",
                "Актив: −20% HP себе → +6 HP + Resist II союзникам р.10, КД 90 сек.",
                ACTIVE, 2, p[0], p[1], "p_last_word"));
        p = w(6);
        t.add(new SkillNode("p_martyr", "Мученик",
                "Не умирать пока жив союзник в р.20 (КД 20 мин).",
                KEYSTONE, 2, p[0], p[1], "p_sacrifice_light"));
        p = wSide(3, 0, 60);
        t.add(new SkillNode("p_mart_resist", "−10% получаемый урон",
                "Входящий урон ×0.90.",
                MINOR, 1, p[0], p[1], "p_mart_sacrifice"));
        p = wSide(3, 0, 120);
        t.add(new SkillNode("p_mart_heal_bonus", "+5% лечение посохом",
                "healAmount ×1.05.",
                MINOR, 1, p[0], p[1], "p_mart_resist"));

        return t;
    }
}
