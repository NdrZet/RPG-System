package com.spa.sound.music;

import com.spa.sound.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;

/**
 * Pure context detection — returns which SoundEvent should be playing
 * right now. No Music records, no volume logic; that's MusicFadeManager's job.
 */
public class MusicContextHelper {

    private static long lastCombatTick = -1L;
    private static final long COMBAT_LINGER_TICKS = 200L; // 10 seconds at 20 tps

    /** Returns the SoundEvent that matches the current game context, or null if undetermined. */
    public static SoundEvent getDesiredEvent(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;

        LocalPlayer player = mc.player;
        Level level = mc.level;
        BlockPos pos = player.blockPosition();

        // --- Dimension (highest priority) ---
        if (level.dimension().equals(Level.NETHER)) return ModSoundEvents.MUSIC_NETHER;
        if (level.dimension().equals(Level.END))    return ModSoundEvents.MUSIC_END;

        // --- Critical status ---
        if (player.getHealth() < 10.0f) return ModSoundEvents.MUSIC_LOW_HP;
        if (player.isOnFire())          return ModSoundEvents.MUSIC_ON_FIRE;

        // --- Environment ---
        if (player.isUnderWater()) return ModSoundEvents.MUSIC_UNDERWATER;
        if (player.isSwimming())   return ModSoundEvents.MUSIC_SWIMMING;
        if (player.isFallFlying()) return ModSoundEvents.MUSIC_ELYTRA;

        // --- Activities ---
        if (player.fishing != null) return ModSoundEvents.MUSIC_FISHING;

        // Combat: linger 10 seconds after last hit
        if (player.hurtTime > 0) {
            lastCombatTick = level.getGameTime();
        }
        boolean inCombat = lastCombatTick >= 0
                && (level.getGameTime() - lastCombatTick) < COMBAT_LINGER_TICKS;
        if (inCombat) return ModSoundEvents.MUSIC_COMBAT;

        // --- Biome ---
        if (level.getBiome(pos).is(Biomes.DEEP_DARK)) return ModSoundEvents.MUSIC_ANCIENT_CITY;

        // --- Vehicle ---
        if (player.getVehicle() instanceof Boat) return ModSoundEvents.MUSIC_BOAT;
        if (player.getVehicle() != null)         return ModSoundEvents.MUSIC_RIDING;

        // --- Weather ---
        if (level.isThundering()) return ModSoundEvents.MUSIC_THUNDER;
        if (level.isRaining())    return ModSoundEvents.MUSIC_RAIN;

        // --- Time of day ---
        if (level.getDayTime() % 24000L >= 12000L) return ModSoundEvents.MUSIC_NIGHT;

        // --- Player state ---
        if (player.getFoodData().getFoodLevel() <= 6) return ModSoundEvents.MUSIC_HUNGRY;
        if (player.isSprinting())                     return ModSoundEvents.MUSIC_SPRINTING;

        // --- Underground ---
        if (!level.canSeeSky(pos)) return ModSoundEvents.MUSIC_CAVE;

        // --- Default ---
        return ModSoundEvents.MUSIC_EXPLORATION;
    }
}
