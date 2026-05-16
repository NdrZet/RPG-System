package ru.custom.progression.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages boss arenas, locking chunks, saving and restoring blocks.
 */
public class BossEncounter {
    private final UUID encounterId;
    private final AABB arenaBounds;
    private final Set<UUID> activePlayers;
    private final Map<BlockPos, BlockState> originalBlocks;
    private boolean isActive;

    public BossEncounter(AABB bounds) {
        this.encounterId = UUID.randomUUID();
        this.arenaBounds = bounds;
        this.activePlayers = new HashSet<>();
        this.originalBlocks = new ConcurrentHashMap<>();
    }

    public void addPlayer(ServerPlayer player) {
        this.activePlayers.add(player.getUUID());
    }

    public void startEncounter(ServerLevel level) {
        this.isActive = true;
        snapshotArena(level);
        lockChunks(level, true);
    }

    private void snapshotArena(ServerLevel level) {
        BlockPos.betweenClosedStream(arenaBounds).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                originalBlocks.put(pos.immutable(), state);
            }
        });
    }

    public void resetArena(ServerLevel level) {
        for (Map.Entry<BlockPos, BlockState> entry : originalBlocks.entrySet()) {
            // Flag 2 = Send to Client
            // Flag 16 = Prevent Neighbor Updates
            // Flag 32 = Prevent Drops
            level.setBlock(entry.getKey(), entry.getValue(), 2 | 16 | 32); 
        }
        originalBlocks.clear();
        lockChunks(level, false);
        this.isActive = false;
    }

    private void lockChunks(ServerLevel level, boolean lock) {
        int minChunkX = ((int)arenaBounds.minX) >> 4;
        int maxChunkX = ((int)arenaBounds.maxX) >> 4;
        int minChunkZ = ((int)arenaBounds.minZ) >> 4;
        int maxChunkZ = ((int)arenaBounds.maxZ) >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                level.setChunkForced(x, z, lock);
            }
        }
    }

    public void tick(ServerLevel level) {
        if (!isActive) return;
        
        for (UUID playerId : activePlayers) {
            ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(playerId);
            if (player != null && !player.isDeadOrDying()) {
                if (!arenaBounds.contains(player.position())) {
                    // Teleport back to center and punish
                    player.teleportTo(arenaBounds.getCenter().x, arenaBounds.minY + 2, arenaBounds.getCenter().z);
                    player.hurt(level.damageSources().magic(), 10.0f);
                }
            }
        }
    }
}
