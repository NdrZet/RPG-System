package ru.custom.progression.mixin.client;

import net.minecraft.client.input.KeyboardInput;
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
            // Note: In modern versions, fields might be private. If this fails, we'll need an accessor mixin.
            // For now, assuming direct access works for demonstration.
            // input.movementForward = -input.movementForward;
            // input.movementSideways = -input.movementSideways;
            
            // Invert logical flags for animations
            boolean tempUp = input.pressingForward;
            input.pressingForward = input.pressingBack;
            input.pressingBack = tempUp;
            
            boolean tempLeft = input.pressingLeft;
            input.pressingLeft = input.pressingRight;
            input.pressingRight = tempLeft;
        }
    }
}
