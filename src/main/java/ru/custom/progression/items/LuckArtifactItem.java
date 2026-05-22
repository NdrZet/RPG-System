package ru.custom.progression.items;

import net.minecraft.world.item.Item;

/**
 * Артефакт Удачи — предмет Мага T2. Разблокируется нодой {@code m_golden_hands}.
 * Пассивные бонусы (обрабатываются в
 * {@link ru.custom.progression.skills.SkillEventHooks}):
 * <ul>
 *   <li>+15 к параметру Luck (через AttributeModifier при входе/экипировке)</li>
 *   <li>+5% крит шанс когда активен эффект Luck</li>
 * </ul>
 */
public class LuckArtifactItem extends Item {

    /** Бонус к параметру Luck. */
    public static final int LUCK_BONUS = 15;

    /** Дополнительный шанс крита при активном эффекте Luck (+5%). */
    public static final float CRIT_BONUS_WITH_LUCK = 0.05f;

    public LuckArtifactItem(Properties properties) {
        super(properties);
    }
}
