package ru.custom.progression.items;

import net.minecraft.world.item.Item;

/**
 * Клинок Жажды — оружие Воина T3. Разблокируется нодой {@code w_fury_bloodthirst}.
 * Бонус-урон и хил при убийстве обрабатываются в {@link ru.custom.progression.skills.SkillEventHooks}.
 */
public class BloodthirstBladeItem extends Item {

    /** База +6 к урону в ближнем бою. */
    public static final float BONUS_DAMAGE = 6.0f;

    /** Доля макс. HP, восстанавливаемая при убийстве этим оружием. */
    public static final float LIFESTEAL_ON_KILL = 0.05f;

    public BloodthirstBladeItem(Properties props) {
        super(props);
    }
}
