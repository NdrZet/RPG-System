package ru.custom.progression.mixin.client;

import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.custom.progression.api.ClientStatsCache;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void invertControls(boolean slowDown, float f, CallbackInfo ci) {
        if (ClientStatsCache.controlsInverted) {
            KeyboardInput input = (KeyboardInput) (Object) this;
            
            // Invert physical impulses
            input.forwardImpulse = -input.forwardImpulse;
            input.leftImpulse = -input.leftImpulse;
            
            // Invert logical flags for animations
            boolean tempUp = input.up;
            input.up = input.down;
            input.down = tempUp;
            
            boolean tempLeft = input.left;
            input.left = input.right;
            input.right = tempLeft;
        }
    }
}
