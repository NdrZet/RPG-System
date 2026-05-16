package ru.custom.progression.entity.traits;

import net.minecraft.world.entity.LivingEntity;

/**
 * For bosses that summon minions (minion control).
 */
public interface ISummonerBoss {
    int getMaxMinions();
    int getCurrentMinions();
    void onMinionDeath(LivingEntity minion);
    void summonWave();
    void killAllMinions(); // Usually called during phase transition
}
