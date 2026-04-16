package com.spa.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSoundEvents {

    public static final SoundEvent MUSIC_COMBAT       = register("music.combat");
    public static final SoundEvent MUSIC_FISHING      = register("music.fishing");
    public static final SoundEvent MUSIC_EXPLORATION  = register("music.exploration");
    public static final SoundEvent MUSIC_UNDERWATER   = register("music.underwater");
    public static final SoundEvent MUSIC_NETHER       = register("music.nether");
    public static final SoundEvent MUSIC_END          = register("music.end");
    public static final SoundEvent MUSIC_CAVE         = register("music.cave");
    public static final SoundEvent MUSIC_LOW_HP       = register("music.low_hp");
    public static final SoundEvent MUSIC_ON_FIRE      = register("music.on_fire");
    public static final SoundEvent MUSIC_ELYTRA       = register("music.elytra");
    public static final SoundEvent MUSIC_BOAT         = register("music.boat");
    public static final SoundEvent MUSIC_RIDING       = register("music.riding");
    public static final SoundEvent MUSIC_THUNDER      = register("music.thunder");
    public static final SoundEvent MUSIC_RAIN         = register("music.rain");
    public static final SoundEvent MUSIC_NIGHT        = register("music.night");
    public static final SoundEvent MUSIC_HUNGRY       = register("music.hungry");
    public static final SoundEvent MUSIC_SPRINTING    = register("music.sprinting");
    public static final SoundEvent MUSIC_SWIMMING     = register("music.swimming");
    public static final SoundEvent MUSIC_ANCIENT_CITY = register("music.ancient_city");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Spasound.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id,
                SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() {
        // Calling this triggers static field initialization and registers all sound events
    }
}
