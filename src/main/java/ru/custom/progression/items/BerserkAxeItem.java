package ru.custom.progression.items;

import net.minecraft.world.item.Item;

/**
 * Топор Берсерка — оружие Воина T2. Разблокируется нодой {@code w_fury_berserk}.
 * Бонус-урон обрабатывается в {@link ru.custom.progression.skills.SkillEventHooks}.
 */
public class BerserkAxeItem extends Item {

    /** База +4 к урону в ближнем бою. */
    public static final float BONUS_DAMAGE = 4.0f;

    /** Дополнительный множитель при HP < 50%. */
    public static final float LOW_HP_BONUS_MULT = 0.10f;

    public BerserkAxeItem(Properties props) {
        super(props);
    }
}
