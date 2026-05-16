package ru.custom.progression.entity.traits;

import net.minecraft.server.level.ServerPlayer;
import java.util.List;

/**
 * For bosses that change gravity or physical laws.
 */
public interface IRealityWarper {
    void applyArenaRules(List<ServerPlayer> playersInArena);
    void resetArenaRules();
    int getRuleChangeCooldown();
}
