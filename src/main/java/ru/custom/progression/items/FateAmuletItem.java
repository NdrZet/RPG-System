package ru.custom.progression.items;

import net.minecraft.world.item.Item;

/**
 * Амулет Судьбы — предмет Мага T3. Разблокируется нодой {@code m_fate}.
 * Пассивные бонусы (обрабатываются в
 * {@link ru.custom.progression.skills.SkillEventHooks}):
 * <ul>
 *   <li>+25 к параметру Luck (через AttributeModifier при входе/экипировке)</li>
 *   <li>+10% крит шанс когда активен эффект Luck</li>
 *   <li>20% шанс полностью уклониться от урона (при активном Luck)</li>
 * </ul>
 */
public class FateAmuletItem extends Item {

    /** Бонус к параметру Luck. */
    public static final int LUCK_BONUS = 25;

    /** Дополнительный шанс крита при активном эффекте Luck (+10%). */
    public static final float CRIT_BONUS_WITH_LUCK = 0.10f;

    /** Шанс уклонения от урона при активном эффекте Luck (20%). */
    public static final float DODGE_CHANCE = 0.20f;

    public FateAmuletItem(Properties properties) {
        super(properties);
    }
}
