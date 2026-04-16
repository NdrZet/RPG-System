package com.spa.sound.mixin.client;

import com.spa.sound.music.MusicFadeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses vanilla MusicManager while our MusicFadeManager is active.
 * By returning null here, MusicManager stops trying to schedule any tracks,
 * leaving our FadeableMusicInstance as the only thing playing music.
 */
@Mixin(Minecraft.class)
public class MusicContextMixin {

    @Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true, require = 0)
    private void suppressVanillaMusic(CallbackInfoReturnable<Music> cir) {
        if (MusicFadeManager.isReady()) {
            cir.setReturnValue(null);
        }
    }
}
