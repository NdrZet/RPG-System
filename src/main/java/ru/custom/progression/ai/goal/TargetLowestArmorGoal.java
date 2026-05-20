package ru.custom.progression.ai.goal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.Comparator;
import java.util.List;

/**
 * Targets players with low armor, ignoring distance priority.
 */
public class TargetLowestArmorGoal extends NearestAttackableTargetGoal<Player> {
    
    public TargetLowestArmorGoal(Mob mob) {
        super(mob, Player.class, true);
    }

    @Override
    protected void findTarget() {
        List<Player> players = this.mob.level().getEntitiesOfClass(Player.class, this.getTargetSearchArea(this.getFollowDistance()), (e) -> true);
        players.sort(Comparator.comparingDouble(p -> p.getAttributeValue(Attributes.ARMOR)));

        if (!players.isEmpty()) {
            this.target = players.get(0);
        }
    }
}
