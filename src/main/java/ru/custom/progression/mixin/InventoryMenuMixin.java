package ru.custom.progression.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для InventoryMenu — в данный момент не вносит изменений в слоты.
 * Панель прогрессии рендерится отдельно справа от инвентаря.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

    protected InventoryMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/class_1661;ZLnet/minecraft/class_1657;)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void onInit(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
        // Слоты не изменяем
    }
}
