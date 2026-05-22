package ru.custom.progression.items;

import net.minecraft.world.item.BowItem;

/**
 * Лук Меткого — оружие Следопыта T2. Разблокируется нодой {@code r_eagle}.
 * Пассивные бонусы (обрабатываются в
 * {@link ru.custom.progression.skills.SkillEventHooks}):
 * <ul>
 *   <li>+20% урона стрелами</li>
 *   <li>+10% шанс крита со стрел (двойной урон)</li>
 * </ul>
 */
public class MarksmanBowItem extends BowItem {

    /** Бонус урона стрелами (+20%). */
    public static final float BONUS_DAMAGE = 0.20f;

    /** Шанс критического попадания стрелой (+10%). */
    public static final float CRIT_CHANCE = 0.10f;

    public MarksmanBowItem(Properties properties) {
        super(properties);
    }
}
