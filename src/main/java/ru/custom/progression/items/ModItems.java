package ru.custom.progression.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Реестр классовых предметов.
 * Вызов {@link #register()} должен происходить в {@code onInitialize()}.
 */
public final class ModItems {

    public static final Item HEALING_STAFF = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath("progression", "healing_staff"),
            new HealingStaffItem()
    );

    public static final Item WAR_CRY = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath("progression", "war_cry"),
            new WarCryItem()
    );

    public static final Item LUCK_SCROLL = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath("progression", "luck_scroll"),
            new LuckScrollItem()
    );

    private ModItems() { }

    /** Вызывается из ProgressionMod для инициализации статических полей. */
    public static void register() { /* статические поля инициализируются при загрузке класса */ }

    /**
     * Выдаёт игроку классовый предмет при выборе класса.
     * Следопыт предмета не получает — его бонус в атрибутах скорости.
     *
     * @param player      игрок
     * @param playerClass выбранный класс
     */
    public static void giveClassItem(ServerPlayer player, String playerClass) {
        Item item = switch (playerClass) {
            case "Жрец"  -> HEALING_STAFF;
            case "Воин"  -> WAR_CRY;
            case "Маг"   -> LUCK_SCROLL;
            default      -> null;
        };
        if (item == null) return;

        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            // Инвентарь полон — выбрасываем предмет у ног
            player.drop(stack, false);
        }
    }
}
