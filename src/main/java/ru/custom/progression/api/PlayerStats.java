package ru.custom.progression.api;

import java.util.HashSet;
import java.util.Set;

/**
 * Модель данных игрока: уровень, опыт, ранг, очки навыков,
 * класс персонажа и четыре базовых стата.
 * <p>
 * Gson сериализует все поля автоматически (нет аннотаций — всё по имени поля).
 * Конструктор по умолчанию инициализирует новичка-Странника.
 */
public class PlayerStats {

    // ── Прогрессия ──────────────────────────────────────────────────────────
    private int    level       = 1;
    private int    experience  = 0;
    private String rank        = "Новичок";
    private int    skillPoints = 0;
    private int    statPoints  = 0; // Очки характеристик, выдаваемые за тиры

    // ── Класс персонажа ─────────────────────────────────────────────────────
    /** Стартовый класс — «Странник». Меняется кнопкой «ВЫБРАТЬ ПУТЬ». */
    private String playerClass = "Странник";

    // ── Базовые характеристики ───────────────────────────────────────────────
    private int strength     = 1;   // STR — физическая сила
    private int agility      = 1;   // AGI — скорость и ловкость
    private int vitality     = 1;   // VIT — здоровье и выносливость
    private int intelligence = 1;   // INT — магия и опыт получения

    // ── Древо навыков ────────────────────────────────────────────────────────
    /** ID активированных нод древа навыков. */
    private Set<String> unlockedNodes = new HashSet<>();

    // ────────────────────────────────────────────────────────────────────────
    // Конструкторы
    // ────────────────────────────────────────────────────────────────────────

    /** Создаёт нового игрока со стартовыми значениями. */
    public PlayerStats() { }

    // ────────────────────────────────────────────────────────────────────────
    // Игровая логика
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Добавляет опыт и повышает уровень, если накоплено достаточно.
     * Каждое повышение уровня даёт 1 очко навыков и пересчитывает ранг.
     *
     * @param amount количество добавляемого опыта
     */
    public void addExperience(int amount) {
        this.experience += amount;
        int xpNeeded = getRequiredXp(this.level);
        while (this.experience >= xpNeeded) {
            this.experience -= xpNeeded;
            this.level++;
            this.skillPoints += 1;
            this.rank = calculateRank();
            
            // Начисляем очки статов за достижение новых тиров
            if (this.level == 20) this.statPoints += 10;
            else if (this.level == 40) this.statPoints += 15;
            else if (this.level == 70) this.statPoints += 20;
            else if (this.level == 100) this.statPoints += 30;

            xpNeeded = getRequiredXp(this.level);
        }
    }

    /**
     * Возвращает требуемый опыт для перехода на следующий уровень.
     * Реализует ступенчато-экспоненциальный рост.
     */
    private int getRequiredXp(int currentLevel) {
        if (currentLevel < 10) return (int) (currentLevel * 100 * 1.0);
        if (currentLevel < 20) return (int) (currentLevel * 100 * 2.0);
        if (currentLevel < 30) return (int) (currentLevel * 100 * 4.0);
        if (currentLevel < 40) return (int) (currentLevel * 100 * 7.0);
        if (currentLevel < 50) return (int) (currentLevel * 100 * 11.0);
        if (currentLevel < 60) return (int) (currentLevel * 100 * 16.5);
        if (currentLevel < 70) return (int) (currentLevel * 100 * 24.0);
        if (currentLevel < 80) return (int) (currentLevel * 100 * 34.0);
        if (currentLevel < 90) return (int) (currentLevel * 100 * 46.0);
        return (int) (currentLevel * 100 * 60.6061);
    }

    /**
     * Вычисляет ранг на основе текущего уровня.
     * Новичок (1–5) → Искатель (6–10) → Ветеран (11–20) → Мастер (21–30) → Легенда (31+)
     *
     * @return строка ранга
     */
    public String calculateRank() {
        if (this.level < 10)  return "Новичок";
        if (this.level < 25)  return "Искатель";
        if (this.level < 50)  return "Ветеран";
        if (this.level < 75)  return "Мастер";
        if (this.level < 100) return "Чемпион";
        return "Легенда";
    }

    /**
     * Тратит одно очко статов на повышение указанного стата.
     *
     * @param statName название стата: "strength", "agility", "vitality", "intelligence"
     * @return {@code true}, если очко потрачено успешно
     */
    public boolean upgradeStat(String statName) {
        if (this.statPoints <= 0) return false;
        switch (statName) {
            case "strength"     -> { if (this.strength     >= 50) return false; this.strength++; }
            case "agility"      -> { if (this.agility      >= 50) return false; this.agility++; }
            case "vitality"     -> { if (this.vitality     >= 50) return false; this.vitality++; }
            case "intelligence" -> { if (this.intelligence >= 50) return false; this.intelligence++; }
            default -> { return false; }
        }
        this.statPoints--;
        return true;
    }

    public void addStatPoints(int amount) {
        this.statPoints += amount;
    }

    /**
     * Сбрасывает все данные игрока до стартовых значений.
     * Используется командой администратора {@code /progression reset}.
     */
    public void reset() {
        this.level       = 1;
        this.experience  = 0;
        this.rank        = "Новичок";
        this.skillPoints = 0;
        this.statPoints  = 0;
        this.playerClass = "Странник";
        this.strength     = 1;
        this.agility      = 1;
        this.vitality     = 1;
        this.intelligence = 1;
        if (this.unlockedNodes != null) this.unlockedNodes.clear();
        else this.unlockedNodes = new HashSet<>();
    }

    // ── Древо навыков ─────────────────────────────────────────────────────

    public Set<String> getUnlockedNodes() {
        if (unlockedNodes == null) unlockedNodes = new HashSet<>();
        return unlockedNodes;
    }

    public void setUnlockedNodes(Set<String> nodes) {
        this.unlockedNodes = nodes != null ? nodes : new HashSet<>();
    }

    public boolean isNodeUnlocked(String id) {
        return unlockedNodes != null && unlockedNodes.contains(id);
    }

    /**
     * Тратит очки навыков и разблокирует указанную ноду.
     * Вся валидация (граф, стоимость) — в вызывающем коде.
     */
    public boolean unlockNode(String id, int cost) {
        if (this.skillPoints < cost) return false;
        if (unlockedNodes == null) unlockedNodes = new HashSet<>();
        if (!unlockedNodes.add(id)) return false;
        this.skillPoints -= cost;
        return true;
    }

    /**
     * Возвращает все потраченные очки и очищает активированные ноды.
     * @param totalRefund сумма очков к возврату (рассчитывается по стоимостям нод)
     */
    public void resetSkillTree(int totalRefund) {
        if (unlockedNodes == null) unlockedNodes = new HashSet<>();
        unlockedNodes.clear();
        this.skillPoints += totalRefund;
    }

    /**
     * Меняет класс персонажа (доступно с 5-го уровня).
     *
     * @param newClass новый класс персонажа
     * @return {@code true}, если смена прошла успешно
     */
    public boolean chooseClass(String newClass) {
        if (this.level < 5) return false;
        if (newClass == null || newClass.isBlank()) return false;
        this.playerClass = newClass;
        return true;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Геттеры и сеттеры
    // ────────────────────────────────────────────────────────────────────────

    public int    getLevel()        { return level; }
    public void   setLevel(int v)   { this.level = v; }

    public int    getExperience()       { return experience; }
    public void   setExperience(int v)  { this.experience = v; }

    public String getRank()          { return rank; }
    public void   setRank(String v)  { this.rank = v; }

    public int    getSkillPoints()       { return skillPoints; }
    public void   setSkillPoints(int v)  { this.skillPoints = v; }

    public int    getStatPoints()        { return statPoints; }
    public void   setStatPoints(int v)   { this.statPoints = v; }

    public String getPlayerClass()           { return playerClass; }
    public void   setPlayerClass(String v)   { this.playerClass = v; }

    public int  getStrength()        { return strength; }
    public void setStrength(int v)   { this.strength = v; }

    public int  getAgility()         { return agility; }
    public void setAgility(int v)    { this.agility = v; }

    public int  getVitality()        { return vitality; }
    public void setVitality(int v)   { this.vitality = v; }

    public int  getIntelligence()         { return intelligence; }
    public void setIntelligence(int v)    { this.intelligence = v; }

    @Override
    public String toString() {
        return String.format(
            "PlayerStats{level=%d, exp=%d, rank='%s', class='%s', sp=%d, statp=%d, STR=%d, AGI=%d, VIT=%d, INT=%d}",
            level, experience, rank, playerClass, skillPoints, statPoints,
            strength, agility, vitality, intelligence
        );
    }
}
