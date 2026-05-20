package ru.custom.progression.entity.boss;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import ru.custom.progression.entity.SpaBaseEntity;

/**
 * Базовый класс для боссов с двумя фазами.
 * Управляет переходом между фазами, кат-сценами и неуязвимостью.
 */
public abstract class SpaTwoPhaseBoss extends SpaBaseEntity {
    
    public static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(SpaTwoPhaseBoss.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IN_TRANSITION = SynchedEntityData.defineId(SpaTwoPhaseBoss.class, EntityDataSerializers.BOOLEAN);
    
    protected int transitionTimer = 0;
    protected final int maxTransitionTime = 100; // 5 секунд при 20 TPS

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

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerableTo(level, source) || isInTransition()) {
            return false;
        }
        
        float currentHealth = this.getHealth();
        // Перехват смертельного урона для активации 2-й фазы
        if (currentHealth - amount <= 0.0F && getPhase() == 1) {
            this.setHealth(1.0F); // Оставляем 1 ХП
            this.startPhaseTransition();
            return false; // Отменяем смерть
        }
        return super.hurtServer(level, source, amount);
    }
    
    protected void startPhaseTransition() {
        this.entityData.set(IN_TRANSITION, true);
        this.removeAllEffects();
        this.setInvulnerable(true); // Временно неуязвим
        
        // Остановка ИИ
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
        this.setHealth(this.getMaxHealth()); // Восстановление ХП
        
        this.registerPhaseTwoGoals();
        this.applyPhaseTwoModifiers();
        this.onPhaseTwoStart(); 
    }
    
    // --- Сохранение состояния ---
    
    @Override
    public void addAdditionalSaveData(ValueOutput compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("CurrentPhase", this.getPhase());
        compound.putBoolean("InTransition", this.isInTransition());
        compound.putInt("TransitionTimer", this.transitionTimer);
    }

    @Override
    public void readAdditionalSaveData(ValueInput compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(PHASE, compound.getInt("CurrentPhase").orElse(1));
        this.entityData.set(IN_TRANSITION, compound.getBoolean("InTransition").orElse(false));
        this.transitionTimer = compound.getInt("TransitionTimer").orElse(0);
        
        // Восстановление ИИ после загрузки мира
        if (this.getPhase() == 2 && !this.isInTransition()) {
            this.registerPhaseTwoGoals();
            this.applyPhaseTwoModifiers();
        }
    }
    
    // --- Хуки для конкретных боссов ---
    
    protected abstract void onPhaseTransitionStart();
    protected abstract void onPhaseTransitionTick(int tick);
    protected abstract void onPhaseTwoStart();
    protected abstract void registerPhaseTwoGoals();
    protected abstract void applyPhaseTwoModifiers();
}
