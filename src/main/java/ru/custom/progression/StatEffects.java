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

    // ── Бонусы от артефактов Мага ─────────────────────────────────────────────
    private static final Identifier ART_LUCK_ID = Identifier.fromNamespaceAndPath("progression", "artifact_luck");

    private StatEffects() { }

    /**
     * Пересчитывает и применяет все модификаторы атрибутов для игрока.
     * Безопасно вызывать повторно — старые модификаторы заменяются новыми.
     */
    public static void apply(ServerPlayer player, PlayerStats stats) {
        applyStatBonuses(player, stats);
        applyClassBonuses(player, stats);
        applyArtifactBonuses(player);
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
     * Классовые бонусы с учетом Тира.
     */
    private static void applyClassBonuses(ServerPlayer player, PlayerStats stats) {
        // Сбрасываем все прежние классовые бонусы
        removeClassModifiers(player);

        String playerClass = stats.getPlayerClass();
        int lvl = stats.getLevel();
        int tier = lvl >= 100 ? 5 : lvl >= 70 ? 4 : lvl >= 40 ? 3 : lvl >= 20 ? 2 : 1;

        switch (playerClass) {
            case "Воин" -> {
                double dmg = switch (tier) {
                    case 5 -> 28.0; case 4 -> 20.0; case 3 -> 14.0; case 2 -> 8.0; default -> 4.0;
                };
                double hp = switch (tier) {
                    case 5 -> 70.0; case 4 -> 50.0; case 3 -> 35.0; case 2 -> 20.0; default -> 10.0;
                };
                setModifier(player, Attributes.ATTACK_DAMAGE, CLS_DMG_ID, dmg, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.MAX_HEALTH, CLS_HP_ID, hp, AttributeModifier.Operation.ADD_VALUE);
            }
            case "Маг" -> {
                double luck = switch (tier) {
                    case 5 -> 75.0; case 4 -> 50.0; case 3 -> 35.0; case 2 -> 20.0; default -> 10.0;
                };
                double spd = switch (tier) {
                    case 5 -> 0.25; case 4 -> 0.20; case 3 -> 0.15; case 2 -> 0.10; default -> 0.05;
                };
                setModifier(player, Attributes.LUCK, CLS_LUCK_ID, luck, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.MOVEMENT_SPEED, CLS_SPD_ID, spd, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            }
            case "Следопыт" -> {
                double spd = switch (tier) {
                    case 5 -> 0.60; case 4 -> 0.45; case 3 -> 0.32; case 2 -> 0.20; default -> 0.10;
                };
                double aspd = switch (tier) {
                    case 5 -> 5.0; case 4 -> 4.0; case 3 -> 3.0; case 2 -> 2.0; default -> 1.0;
                };
                setModifier(player, Attributes.MOVEMENT_SPEED, CLS_SPD_ID, spd, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                setModifier(player, Attributes.ATTACK_SPEED, CLS_ASPD_ID, aspd, AttributeModifier.Operation.ADD_VALUE);
            }
            case "Жрец" -> {
                double hp = switch (tier) {
                    case 5 -> 60.0; case 4 -> 40.0; case 3 -> 28.0; case 2 -> 16.0; default -> 8.0;
                };
                double luck = switch (tier) {
                    case 5 -> 40.0; case 4 -> 30.0; case 3 -> 20.0; case 2 -> 12.0; default -> 5.0;
                };
                setModifier(player, Attributes.MAX_HEALTH, CLS_HP_ID, hp, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.LUCK, CLS_LUCK_ID, luck, AttributeModifier.Operation.ADD_VALUE);
            }
            // "Странник" и неизвестные — без бонусов
        }
    }

    // ── Бонусы от тирированных артефактов Мага ──────────────────────────────

    private static void applyArtifactBonuses(ServerPlayer player) {
        int luckBonus = 0;

        net.minecraft.world.item.Item main = player.getMainHandItem().getItem();
        net.minecraft.world.item.Item off  = player.getOffhandItem().getItem();

        if (main instanceof ru.custom.progression.items.LuckArtifactItem
                || off instanceof ru.custom.progression.items.LuckArtifactItem) {
            luckBonus += ru.custom.progression.items.LuckArtifactItem.LUCK_BONUS;
        }
        if (main instanceof ru.custom.progression.items.FateAmuletItem
                || off instanceof ru.custom.progression.items.FateAmuletItem) {
            luckBonus += ru.custom.progression.items.FateAmuletItem.LUCK_BONUS;
        }

        AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
        if (luckAttr != null) {
            luckAttr.removeModifier(ART_LUCK_ID);
            if (luckBonus > 0) {
                luckAttr.addPermanentModifier(
                        new AttributeModifier(ART_LUCK_ID, luckBonus, AttributeModifier.Operation.ADD_VALUE));
            }
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
