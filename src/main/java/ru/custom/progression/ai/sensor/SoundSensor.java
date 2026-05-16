package ru.custom.progression.ai.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import ru.custom.progression.entity.SpaBaseEntity;

import java.util.Set;

/**
 * Sensor that detects noisy players (moving without sneaking).
 */
public class SoundSensor extends Sensor<SpaBaseEntity> {
    
    // In 1.21, MemoryModuleType registration is handled differently, often via registries or directly passing to brain.
    // For this boilerplate, assuming we have a custom or standard memory module.
    // Replace with a valid MemoryModuleType if needed.
    public static final MemoryModuleType<Vec3> HEARD_POSITION = MemoryModuleType.register("heard_position");

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(HEARD_POSITION);
    }

    @Override
    protected void doTick(ServerLevel level, SpaBaseEntity entity) {
        Player nearestNoisyPlayer = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Player player : level.players()) {
            if (player.distanceTo(entity) < 30.0 && !player.isCrouching() && !player.isSpectator()) {
                // If player is moving (delta movement > 0.01)
                if (player.getDeltaMovement().lengthSqr() > 0.01) {
                    double dist = player.distanceToSqr(entity);
                    if (dist < minDistance) {
                        minDistance = dist;
                        nearestNoisyPlayer = player;
                    }
                }
            }
        }
        
        if (nearestNoisyPlayer != null) {
            entity.getBrain().setMemory(HEARD_POSITION, nearestNoisyPlayer.position());
        }
    }
}
