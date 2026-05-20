package ru.custom.progression.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import ru.custom.progression.api.Faction;

/**
 * Base class for all mod's creatures.
 * Handles common checks, disables vanilla vulnerabilities, and provides base resistances.
 */
public abstract class SpaBaseEntity extends Monster {

    protected SpaBaseEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.applyFactionTraits(getFaction());
        this.xpReward = 0; // Custom XP handling
    }

    protected abstract Faction getFaction();

    protected void applyFactionTraits(Faction faction) {
        // Apply base traits for the faction if needed
    }

    // --- Vanilla Exploit Protection ---

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Disable pushing
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false; // Bosses are not pushed by other mobs
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.CACTUS)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    public float modifyDamage(DamageSource source, float amount) {
        Faction faction = getFaction();

        if (faction == Faction.CONSTRUCT) {
            if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getEntity() instanceof Player) {
                // TODO: Check if the player is holding a PickaxeItem
                return amount * 0.5f;
            }
        } else if (faction == Faction.MUTANT) {
            if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)) {
                return amount * 1.5f; // Vulnerable to fire
            }
        }

        return amount;
    }
}
