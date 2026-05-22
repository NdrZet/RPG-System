package ru.custom.progression.mixin.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.custom.progression.entity.SpaBaseEntity;
import ru.custom.progression.entity.boss.SpaTwoPhaseBoss;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof SpaBaseEntity spaEntity) {
            if (spaEntity.isInvulnerableTo(level, source)) {
                cir.setReturnValue(false);
                return;
            }
            
            if (spaEntity instanceof SpaTwoPhaseBoss boss) {
                 if (boss.isInTransition()) {
                     cir.setReturnValue(false);
                     return;
                 }
                 
                 // Handle lethal damage transition
                 float calculatedDamage = spaEntity.modifyDamage(source, amount);
                 if (boss.getHealth() - calculatedDamage <= 0.0F && boss.getPhase() == 1) {
                     boss.setHealth(1.0F);
                     boss.triggerPhaseTransition();
                     cir.setReturnValue(false);
                     return;
                 }
            }
        }
    }
    
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float modifyDamage(float amount, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof SpaBaseEntity spaEntity) {
            return spaEntity.modifyDamage(source, amount);
        }
        return amount;
    }
}
