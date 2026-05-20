package ru.custom.progression.entity.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import ru.custom.progression.entity.SpaBaseEntity;

/**
 * Base class for two-phase bosses.
 * Manages phase transitions, cutscenes, and invulnerability.
 */
public abstract class SpaTwoPhaseBoss extends SpaBaseEntity {

    public static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(SpaTwoPhaseBoss.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IN_TRANSITION = SynchedEntityData.defineId(SpaTwoPhaseBoss.class, EntityDataSerializers.BOOLEAN);

    protected int transitionTimer = 0;
    protected final int maxTransitionTime = 100; // 5 seconds at 20 TPS

    protected SpaTwoPhaseBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(IN_TRANSITION, false);
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public boolean isInTransition() {
        return this.entityData.get(IN_TRANSITION);
    }

    public void triggerPhaseTransition() {
        this.entityData.set(IN_TRANSITION, true);
        this.removeAllEffects();
        this.setInvulnerable(true); // Temporarily invulnerable

        // Stop AI
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);

        this.transitionTimer = 0;

        this.onPhaseTransitionStart();
    }

    @Override
    public void tick() {
        super.tick();
        if (isInTransition()) {
            transitionTimer++;
            this.onPhaseTransitionTick(transitionTimer);
            if (transitionTimer >= maxTransitionTime) {
                completePhaseTransition();
            }
        }
    }

    protected void completePhaseTransition() {
        this.entityData.set(PHASE, 2);
        this.entityData.set(IN_TRANSITION, false);
        this.setInvulnerable(false);
        this.setHealth(this.getMaxHealth()); // Restore HP

        this.registerPhaseTwoGoals();
        this.applyPhaseTwoModifiers();
        this.onPhaseTwoStart();
    }

    // --- State Saving ---

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("CurrentPhase", this.getPhase());
        compound.putBoolean("InTransition", this.isInTransition());
        compound.putInt("TransitionTimer", this.transitionTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CurrentPhase")) {
            this.entityData.set(PHASE, compound.getInt("CurrentPhase"));
        }
        if (compound.contains("InTransition")) {
            this.entityData.set(IN_TRANSITION, compound.getBoolean("InTransition"));
        }
        if (compound.contains("TransitionTimer")) {
            this.transitionTimer = compound.getInt("TransitionTimer");
        }

        // Restore AI after world load
        if (this.getPhase() == 2 && !this.isInTransition()) {
            this.registerPhaseTwoGoals();
            this.applyPhaseTwoModifiers();
        }
    }

    // --- Hooks for specific bosses ---

    protected abstract void onPhaseTransitionStart();
    protected abstract void onPhaseTransitionTick(int tick);
    protected abstract void onPhaseTwoStart();
    protected abstract void registerPhaseTwoGoals();
    protected abstract void applyPhaseTwoModifiers();
}
