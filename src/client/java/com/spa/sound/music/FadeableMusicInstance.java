package com.spa.sound.music;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * A music SoundInstance whose volume can be changed while it is playing.
 * Minecraft's SoundEngine calls getVolume() every tick for every active
 * sound channel, so reducing fadeVolume here produces a real audio fade-out.
 */
public class FadeableMusicInstance extends AbstractSoundInstance {

    private float fadeVolume = 1.0f;

    public FadeableMusicInstance(SoundEvent event) {
        super(event, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = false;
        this.delay = 0;
        this.relative = true; // Not positioned in the world
        this.volume = 1.0f;
        this.pitch = 1.0f;
    }

    @Override
    public float getVolume() {
        // SoundEngine multiplies this by the Music category slider.
        return this.volume * fadeVolume;
    }

    public void setFadeVolume(float v) {
        this.fadeVolume = Math.max(0f, Math.min(1f, v));
    }

    public float getFadeVolume() {
        return fadeVolume;
    }
}
