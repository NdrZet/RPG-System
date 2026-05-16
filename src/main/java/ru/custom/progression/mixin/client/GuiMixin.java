package ru.custom.progression.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.custom.progression.api.ClientStatsCache;

@Mixin(Gui.class)
public class GuiMixin {
    
    // Intercepts the request for current health when rendering hearts
    @Redirect(method = "renderHealthLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getHealth()F"))
    private float redirectGetHealth(Player player) {
        if (ClientStatsCache.fakeHealthEnabled) {
            return 1.0F; // Render always 0.5 heart (1.0 HP)
        }
        return player.getHealth();
    }
}
