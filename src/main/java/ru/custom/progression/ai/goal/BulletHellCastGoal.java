package ru.custom.progression.ai.goal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import ru.custom.progression.entity.boss.SpaTwoPhaseBoss;
// import ru.custom.progression.skills.SkillUtils;

import java.util.EnumSet;

/**
 * A goal that makes the boss stop and cast a barrage of projectiles.
 */
public class BulletHellCastGoal extends Goal {
    
    private final SpaTwoPhaseBoss boss;
    private int castTimer = 0;
    private final int maxCastTime = 100; // 5 seconds of casting (at 20 TPS)
    private final int cooldown = 200; // 10 seconds CD
    private int currentCooldown = 0;

    public BulletHellCastGoal(SpaTwoPhaseBoss boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (currentCooldown > 0) {
            currentCooldown--;
            return false;
        }
        return boss.getTarget() != null && boss.getPhase() == 2;
    }

    @Override
    public void start() {
        this.castTimer = 0;
        this.boss.getNavigation().stop(); // Stop moving
        // If using GeckoLib, trigger animation here
        // this.boss.triggerAnim("attack", "bullet_hell_start"); 
    }

    @Override
    public void tick() {
        castTimer++;
        if (castTimer % 5 == 0) {
            // Spawn projectiles every 5 ticks. 
            // In a real scenario, specify the exact EntityType (e.g., EntityType.FIREBALL)
            // SkillUtils.spawnSpiralProjectiles((ServerLevel) boss.level(), boss, EntityType.SMALL_FIREBALL, 12, 1.5f);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return castTimer < maxCastTime && boss.getTarget() != null;
    }

    @Override
    public void stop() {
        this.currentCooldown = cooldown;
        // Reset animation
        // this.boss.triggerAnim("attack", "idle");
    }
}
