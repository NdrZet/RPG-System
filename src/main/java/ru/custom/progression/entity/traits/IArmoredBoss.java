package ru.custom.progression.entity.traits;

/**
 * For bosses that have a local shield or breakable armor.
 */
public interface IArmoredBoss {
    float getArmorMitigation();
    void breakArmor();
    boolean isArmorBroken();
    void restoreArmor();
}
