package ru.custom.progression.items;

import net.minecraft.world.item.BowItem;

/**
 * Охотничий лук — базовое оружие Следопыта T1.
 * Выдаётся при выборе класса «Следопыт» через {@link ModItems#giveClassItem}.
 * Пассивный бонус: +10% урона стрелами (обрабатывается в
 * {@link ru.custom.progression.skills.SkillEventHooks}).
 */
public class HunterBowItem extends BowItem {

    /** Бонус урона стрелами (+10%). */
    public static final float BONUS_DAMAGE = 0.10f;

    public HunterBowItem(Properties properties) {
        super(properties);
    }
}
