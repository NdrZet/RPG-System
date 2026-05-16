package ru.custom.progression.skills;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Utility class for complex boss skills and math (Bullet hells, singularity, etc.).
 */
public class SkillUtils {

    /**
     * Pulls entities towards a center point (e.g., Black Hole effect).
     */
    public static void pullEntitiesToCenter(ServerLevel level, Vec3 center, double radius, double strength) {
        AABB box = new AABB(center, center).inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box);
        
        for (LivingEntity target : targets) {
            double distSqr = target.distanceToSqr(center);
            if (distSqr < radius * radius && distSqr > 1.0) { // Don't pull entities already at the center
                Vec3 dir = center.subtract(target.position()).normalize();
                double dist = Math.sqrt(distSqr);
                // Linear interpolation of strength
                double distStr = strength * (1.0 - (dist / radius));
                
                target.setDeltaMovement(target.getDeltaMovement().add(dir.scale(distStr)));
                target.hurtMarked = true; // Important for syncing to client
            }
        }
    }

    /**
     * Spawns a ring/spiral of projectiles for Bullet Hell mechanics.
     */
    public static void spawnSpiralProjectiles(ServerLevel level, LivingEntity source, EntityType<?> projType, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            
            Entity proj = projType.create(level);
            if (proj != null) {
                proj.setPos(source.getX(), source.getY() + source.getBbHeight() / 2, source.getZ());
                // Dir X, Dir Y, Dir Z, speed, accuracy/divergence
                // shoot method depends on the specific projectile class (usually Projectile or ThrowableProjectile)
                // Using generic logic here. Requires actual projectile implementation cast if needed.
                // Example for Projectile:
                // ((Projectile) proj).shoot(x, 0, z, (float)speed, 0.0f); 
                
                level.addFreshEntity(proj);
            }
        }
    }
}
