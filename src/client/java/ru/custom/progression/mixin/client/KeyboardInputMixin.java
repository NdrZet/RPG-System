package ru.custom.progression.mixin.client;

import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.custom.progression.api.ClientStatsCache;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void invertControls(CallbackInfo ci) {
        if (ClientStatsCache.controlsInverted) {
            KeyboardInput self = (KeyboardInput) (Object) this;
            Input old = self.keyPresses;
            // Swap forward/backward and left/right
            self.keyPresses = new Input(
                old.backward(),   // forward  <- backward
                old.forward(),    // backward <- forward
                old.right(),      // left     <- right
                old.left(),       // right    <- left
                old.jump(),
                old.shift(),
                old.sprint()
            );
        }
    }
}
