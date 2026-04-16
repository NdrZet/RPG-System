package com.spa.sound.music;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

/**
 * Manages context-sensitive music with smooth 3-second fade-outs when the
 * context changes. Called every client tick from SpasoundClient.
 *
 * State machine:
 *   IDLE        → no music playing, waiting to start
 *   PLAYING     → a FadeableMusicInstance is active at full volume
 *   FADING_OUT  → volume decreasing; once done, new music starts
 */
public class MusicFadeManager {

    /** Ticks over which the old track fades out (60 ticks = 3 seconds). */
    private static final int FADE_OUT_TICKS = 60;

    private enum State { IDLE, PLAYING, FADING_OUT }

    private static State state = State.IDLE;
    private static SoundEvent currentEvent = null;
    private static SoundEvent nextEvent = null;
    private static FadeableMusicInstance currentInstance = null;
    private static int fadeTick = 0;

    /** True once a world is loaded; signals MusicContextMixin to suppress vanilla music. */
    private static boolean ready = false;

    public static boolean isReady() {
        return ready;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            ready = false;
            return;
        }
        ready = true;

        SoundEvent desired = MusicContextHelper.getDesiredEvent(mc);

        switch (state) {

            case IDLE -> {
                if (desired != null) {
                    startPlaying(mc, desired);
                }
            }

            case PLAYING -> {
                // If the track ended naturally, restart or change
                if (currentInstance == null || !mc.getSoundManager().isActive(currentInstance)) {
                    currentInstance = null;
                    if (desired != null) {
                        startPlaying(mc, desired);
                    } else {
                        state = State.IDLE;
                    }
                    return;
                }
                // Context changed → begin fade-out
                if (desired != currentEvent) {
                    nextEvent = desired;
                    fadeTick = 0;
                    state = State.FADING_OUT;
                }
            }

            case FADING_OUT -> {
                fadeTick++;
                float progress = (float) fadeTick / FADE_OUT_TICKS;
                if (currentInstance != null) {
                    currentInstance.setFadeVolume(1.0f - progress);
                }

                if (fadeTick >= FADE_OUT_TICKS) {
                    // Fade complete — stop old track, start new one
                    if (currentInstance != null) {
                        mc.getSoundManager().stop(currentInstance);
                        currentInstance = null;
                    }
                    SoundEvent toPlay = nextEvent;
                    nextEvent = null;
                    if (toPlay != null) {
                        startPlaying(mc, toPlay);
                    } else {
                        state = State.IDLE;
                    }
                }
            }
        }
    }

    /** Immediately start playing a new track (no fade-in; track starts at full volume). */
    private static void startPlaying(Minecraft mc, SoundEvent event) {
        if (currentInstance != null) {
            mc.getSoundManager().stop(currentInstance);
            currentInstance = null;
        }
        currentEvent = event;
        currentInstance = new FadeableMusicInstance(event);
        mc.getSoundManager().play(currentInstance);
        state = State.PLAYING;
    }

    /** Call when the player disconnects / leaves the world. */
    public static void reset() {
        state = State.IDLE;
        currentEvent = null;
        nextEvent = null;
        currentInstance = null;
        fadeTick = 0;
        ready = false;
    }
}
