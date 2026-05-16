package ru.custom.progression.ai.goal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.List;

/**
 * Targets players with low armor, ignoring distance priority.
 */
public class TargetLowestArmorGoal extends NearestAttackableTargetGoal<Player> {
    
    public TargetLowestArmorGoal(Mob mob) {
        super(mob, Player.class, 20, true, false, (entity) -> {
            if (entity instanceof Player player) {
                if (!player.canBeSeenAsEnemy() || player.isCreative() || player.isSpectator()) {
                    return false;
                }
                
                double armorValue = player.getAttributeValue(Attributes.ARMOR);
                return armorValue <= 12.0; // Targets "squishy" players
            }
            return false;
        });
    }

    @Override
    protected void findTarget() {
        if (this.targetType == Player.class || this.targetType == ServerPlayer.class) {
            ServerLevel serverLevel = (ServerLevel) this.mob.level();
            List<Player> players = serverLevel.players().stream()
                .filter(p -> this.targetConditions.test(serverLevel, this.mob, p))
                .sorted(Comparator.comparingDouble(p -> p.getAttributeValue(Attributes.ARMOR))) // Sort by armor
                .map(p -> (Player) p)
                .toList();

            if (!players.isEmpty()) {
                this.targetMob = players.get(0);
            }
        }
    }
}
