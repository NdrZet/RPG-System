package ru.custom.progression.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Реестр классовых предметов.
 * Вызов {@link #register()} должен происходить в {@code onInitialize()}.
 * <p>
 * В MC 1.21.11 обязательно передавать {@code ResourceKey} через
 * {@code Item.Properties.setId()} до вызова конструктора {@code Item}.
 */
public final class ModItems {

    private static final String NS = "progression";

    // ── ResourceKey для каждого предмета ────────────────────────────────────

    private static final ResourceKey<Item> HEALING_STAFF_KEY = key("healing_staff");
    private static final ResourceKey<Item> WAR_CRY_KEY       = key("war_cry");
    private static final ResourceKey<Item> LUCK_SCROLL_KEY   = key("luck_scroll");
    private static final ResourceKey<Item> RANGER_TRAP_KEY   = key("ranger_trap");

    // ── Зарегистрированные предметы ──────────────────────────────────────────

    public static final Item HEALING_STAFF = Registry.register(
            BuiltInRegistries.ITEM,
            HEALING_STAFF_KEY,
            new HealingStaffItem(new Item.Properties().stacksTo(1).setId(HEALING_STAFF_KEY))
    );

    public static final Item WAR_CRY = Registry.register(
            BuiltInRegistries.ITEM,
            WAR_CRY_KEY,
            new WarCryItem(new Item.Properties().stacksTo(1).setId(WAR_CRY_KEY))
    );

    public static final Item LUCK_SCROLL = Registry.register(
            BuiltInRegistries.ITEM,
            LUCK_SCROLL_KEY,
            new LuckScrollItem(new Item.Properties().stacksTo(1).setId(LUCK_SCROLL_KEY))
    );

    public static final Item RANGER_TRAP = Registry.register(
            BuiltInRegistries.ITEM,
            RANGER_TRAP_KEY,
            new TrapItem(new Item.Properties().stacksTo(1).setId(RANGER_TRAP_KEY))
    );

    private ModItems() { }

    /** Вызывается из ProgressionMod для инициализации статических полей. */
    public static void register() { /* статические поля инициализируются при загрузке класса */ }

    /**
     * Выдаёт игроку классовый предмет при выборе класса.
     *
     * @param player      игрок
     * @param playerClass выбранный класс
     */
    public static void giveClassItem(ServerPlayer player, String playerClass) {
        Item item = switch (playerClass) {
            case "Жрец"     -> HEALING_STAFF;
            case "Воин"     -> WAR_CRY;
            case "Маг"      -> LUCK_SCROLL;
            case "Следопыт" -> RANGER_TRAP;
            default         -> null;
        };
        if (item == null) return;

        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ── Вспомогательный метод ────────────────────────────────────────────────

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(NS, name));
    }
}
