package ru.custom.progression.entity.traits;

import net.minecraft.world.damagesource.DamageSource;
import java.util.Set;

/**
 * For bosses with adaptation mechanics (like Mahoraga).
 */
public interface IAdaptiveBoss {
    void analyzeDamage(DamageSource source, float amount);
    void triggerAdaptation(String damageType);
    boolean hasImmunityTo(String damageType);
    void resetAdaptations();
    Set<String> getImmunities();
}
