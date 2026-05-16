package ru.custom.progression.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.HostileEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import ru.custom.progression.api.Faction;

/**
 * Базовый класс для всех существ мода.
 * Отвечает за общие проверки, отключение ванильных уязвимостей и базовые резисты.
 */
public abstract class SpaBaseEntity extends HostileEntity {

    protected SpaBaseEntity(EntityType<? extends HostileEntity> type, Level level) {
        super(type, level);
        this.applyFactionTraits(getFaction());
        this.xpReward = 0; // Кастомная выдача опыта
    }

    protected abstract Faction getFaction();

    protected void applyFactionTraits(Faction faction) {
        if (faction == Faction.UNDEAD) {
            // Базовые трейты для нежити, если нужно
        }
    }

    // --- Защита от ванильных абьюзов ---

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
        // Отключаем толкание
    }

    @Override
    public boolean canCollideWith(Entity entity) { 
        return false; // Босс не толкается другими мобами
    }

    // Игнорирование удушья в стенах (критично для больших моделей и терраформинга)
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.CACTUS)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        
        amount = modifyDamageBasedOnFaction(source, amount);
        if (amount <= 0) {
            return false;
        }
        
        return super.hurt(source, amount);
    }
    
    protected float modifyDamageBasedOnFaction(DamageSource source, float amount) {
        Faction faction = getFaction();
        
        if (faction == Faction.CONSTRUCT) {
            // Конструкты получают только 50% урона от стрел и не-кирок
            if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getEntity() instanceof Player) {
                // В будущем можно добавить проверку на кирку (PickaxeItem)
                return amount * 0.5f;
            }
        } else if (faction == Faction.MUTANT) {
            if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)) {
                return amount * 1.5f; // Уязвимость к огню
            }
        }
        
        return amount;
    }
}
