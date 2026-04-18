package ru.custom.progression;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.skills.SkillEffects;

/**
 * Применяет AttributeModifier к игроку на основе его характеристик и класса.
 * Вызывается при входе игрока и при каждой прокачке стата / смене класса.
 */
public final class StatEffects {

    // ── Бонусы от прокачанных статов ─────────────────────────────────────────
    private static final Identifier STR_ID = Identifier.fromNamespaceAndPath("progression", "strength_bonus");
    private static final Identifier AGI_ID = Identifier.fromNamespaceAndPath("progression", "agility_bonus");
    private static final Identifier VIT_ID = Identifier.fromNamespaceAndPath("progression", "vitality_bonus");
    private static final Identifier INT_ID = Identifier.fromNamespaceAndPath("progression", "intelligence_bonus");

    // ── Пассивные бонусы класса ───────────────────────────────────────────────
    private static final Identifier CLS_DMG_ID  = Identifier.fromNamespaceAndPath("progression", "class_damage");
    private static final Identifier CLS_HP_ID   = Identifier.fromNamespaceAndPath("progression", "class_health");
    private static final Identifier CLS_SPD_ID  = Identifier.fromNamespaceAndPath("progression", "class_speed");
    private static final Identifier CLS_LUCK_ID = Identifier.fromNamespaceAndPath("progression", "class_luck");
    private static final Identifier CLS_ASPD_ID = Identifier.fromNamespaceAndPath("progression", "class_attack_speed");

    private StatEffects() { }

    /**
     * Пересчитывает и применяет все модификаторы атрибутов для игрока.
     * Безопасно вызывать повторно — старые модификаторы заменяются новыми.
     */
    public static void apply(ServerPlayer player, PlayerStats stats) {
        applyStatBonuses(player, stats);
        applyClassBonuses(player, stats.getPlayerClass());
        SkillEffects.apply(player, stats.getPlayerClass(), stats.getUnlockedNodes());
    }

    // ── Бонусы от прокачки статов ─────────────────────────────────────────────

    private static void applyStatBonuses(ServerPlayer player, PlayerStats stats) {
        // Сила: +0.5 урона за каждое очко сверх 1
        setModifier(player, Attributes.ATTACK_DAMAGE, STR_ID,
                (stats.getStrength() - 1) * 0.5,
                AttributeModifier.Operation.ADD_VALUE);

        // Ловкость: +2% скорости за каждое очко сверх 1
        setModifier(player, Attributes.MOVEMENT_SPEED, AGI_ID,
                (stats.getAgility() - 1) * 0.02,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // Выносливость: +2 HP (1 сердце) за каждое очко сверх 1
        setModifier(player, Attributes.MAX_HEALTH, VIT_ID,
                (stats.getVitality() - 1) * 2.0,
                AttributeModifier.Operation.ADD_VALUE);

        // Интеллект: +1 удача за каждое очко сверх 1
        setModifier(player, Attributes.LUCK, INT_ID,
                (stats.getIntelligence() - 1) * 1.0,
                AttributeModifier.Operation.ADD_VALUE);
    }

    // ── Пассивные бонусы класса ───────────────────────────────────────────────

    /**
     * Классовые бонусы (дополнительно к бонусам от статов):
     * <ul>
     *   <li><b>Воин</b>    — +4 урона, +10 HP (5 сердец)</li>
     *   <li><b>Маг</b>     — +10 удачи, +0.05 скорости</li>
     *   <li><b>Следопыт</b>— +0.10 скорости, +1.0 скорости атаки (50% быстрее)</li>
     *   <li><b>Жрец</b>    — +8 HP (4 сердца), +5 удачи</li>
     *   <li><b>Странник</b>— без бонусов</li>
     * </ul>
     */
    private static void applyClassBonuses(ServerPlayer player, String playerClass) {
        // Сбрасываем все прежние классовые бонусы
        removeClassModifiers(player);

        switch (playerClass) {
            case "Воин" -> {
                setModifier(player, Attributes.ATTACK_DAMAGE, CLS_DMG_ID,
                        4.0, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.MAX_HEALTH, CLS_HP_ID,
                        10.0, AttributeModifier.Operation.ADD_VALUE);
            }
            case "Маг" -> {
                setModifier(player, Attributes.LUCK, CLS_LUCK_ID,
                        10.0, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.MOVEMENT_SPEED, CLS_SPD_ID,
                        0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            }
            case "Следопыт" -> {
                setModifier(player, Attributes.MOVEMENT_SPEED, CLS_SPD_ID,
                        0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                setModifier(player, Attributes.ATTACK_SPEED, CLS_ASPD_ID,
                        1.0, AttributeModifier.Operation.ADD_VALUE);
            }
            case "Жрец" -> {
                setModifier(player, Attributes.MAX_HEALTH, CLS_HP_ID,
                        8.0, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.LUCK, CLS_LUCK_ID,
                        5.0, AttributeModifier.Operation.ADD_VALUE);
            }
            // "Странник" и неизвестные — без бонусов (модификаторы уже сброшены выше)
        }
    }

    /** Удаляет все классовые модификаторы (вызывается перед применением нового класса). */
    private static void removeClassModifiers(ServerPlayer player) {
        AttributeInstance dmg  = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance hp   = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance spd  = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance luck = player.getAttribute(Attributes.LUCK);
        AttributeInstance aspd = player.getAttribute(Attributes.ATTACK_SPEED);
        if (dmg  != null) dmg.removeModifier(CLS_DMG_ID);
        if (hp   != null) hp.removeModifier(CLS_HP_ID);
        if (spd  != null) spd.removeModifier(CLS_SPD_ID);
        if (luck != null) luck.removeModifier(CLS_LUCK_ID);
        if (aspd != null) aspd.removeModifier(CLS_ASPD_ID);
    }

    // ── Утилита ──────────────────────────────────────────────────────────────

    private static void setModifier(ServerPlayer player,
                                     Holder<Attribute> attribute,
                                     Identifier id,
                                     double value,
                                     AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (value != 0.0) {
            instance.addPermanentModifier(new AttributeModifier(id, value, operation));
        }
    }
}
