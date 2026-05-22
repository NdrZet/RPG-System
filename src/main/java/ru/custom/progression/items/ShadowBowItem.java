package ru.custom.progression.items;

import net.minecraft.world.item.BowItem;

/**
 * Лук Тени — оружие Следопыта T3. Разблокируется нодой {@code r_shadow}.
 * Пассивные бонусы (обрабатываются в
 * {@link ru.custom.progression.skills.SkillEventHooks}):
 * <ul>
 *   <li>+35% урона стрелами</li>
 *   <li>+15% шанс крита со стрел</li>
 *   <li>После убийства стрелой — невидимость 2 сек (КД 5 сек)</li>
 * </ul>
 */
public class ShadowBowItem extends BowItem {

    /** Бонус урона стрелами (+35%). */
    public static final float BONUS_DAMAGE = 0.35f;

    /** Шанс критического попадания стрелой (+15%). */
    public static final float CRIT_CHANCE = 0.15f;

    /** Длительность невидимости после убийства стрелой (в тиках). */
    public static final int INVIS_DURATION_TICKS = 40;

    /** Кулдаун невидимости после убийства (в тиках). */
    public static final int INVIS_COOLDOWN_TICKS = 100;

    public ShadowBowItem(Properties properties) {
        super(properties);
    }
}
