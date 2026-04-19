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
 * Реестр классовых и активных предметов (разблокируемых через дерево навыков).
 * Вызов {@link #register()} должен происходить в {@code onInitialize()}.
 */
public final class ModItems {

    private static final String NS = "progression";

    // ── ResourceKey для каждого предмета ────────────────────────────────────
    private static final ResourceKey<Item> HEALING_STAFF_KEY = key("healing_staff");
    private static final ResourceKey<Item> WAR_CRY_KEY       = key("war_cry");
    private static final ResourceKey<Item> LUCK_SCROLL_KEY   = key("luck_scroll");
    private static final ResourceKey<Item> RANGER_TRAP_KEY   = key("ranger_trap");
    private static final ResourceKey<Item> WARRIOR_SHIELD_KEY = key("warrior_shield");
    private static final ResourceKey<Item> TELEPORT_KEY       = key("teleport_rod");
    private static final ResourceKey<Item> SMOKE_CLOUD_KEY    = key("smoke_cloud");
    private static final ResourceKey<Item> AURA_KEY           = key("aura_relic");
    private static final ResourceKey<Item> LEAP_KEY           = key("warrior_leap");
    private static final ResourceKey<Item> TIME_BUBBLE_KEY    = key("time_bubble");
    private static final ResourceKey<Item> FAN_ARROWS_KEY     = key("fan_arrows");
    private static final ResourceKey<Item> SACRIFICE_KEY      = key("sacrifice_relic");

    // ── Зарегистрированные предметы ──────────────────────────────────────────
    public static final Item HEALING_STAFF = Registry.register(
            BuiltInRegistries.ITEM, HEALING_STAFF_KEY,
            new HealingStaffItem(new Item.Properties().stacksTo(1).setId(HEALING_STAFF_KEY))
    );
    public static final Item WAR_CRY = Registry.register(
            BuiltInRegistries.ITEM, WAR_CRY_KEY,
            new WarCryItem(new Item.Properties().stacksTo(1).setId(WAR_CRY_KEY))
    );
    public static final Item LUCK_SCROLL = Registry.register(
            BuiltInRegistries.ITEM, LUCK_SCROLL_KEY,
            new LuckScrollItem(new Item.Properties().stacksTo(1).setId(LUCK_SCROLL_KEY))
    );
    public static final Item RANGER_TRAP = Registry.register(
            BuiltInRegistries.ITEM, RANGER_TRAP_KEY,
            new TrapItem(new Item.Properties().stacksTo(1).setId(RANGER_TRAP_KEY))
    );
    public static final Item WARRIOR_SHIELD = Registry.register(
            BuiltInRegistries.ITEM, WARRIOR_SHIELD_KEY,
            new WarriorShieldItem(new Item.Properties().stacksTo(1).setId(WARRIOR_SHIELD_KEY))
    );
    public static final Item TELEPORT_ROD = Registry.register(
            BuiltInRegistries.ITEM, TELEPORT_KEY,
            new TeleportItem(new Item.Properties().stacksTo(1).setId(TELEPORT_KEY))
    );
    public static final Item SMOKE_CLOUD = Registry.register(
            BuiltInRegistries.ITEM, SMOKE_CLOUD_KEY,
            new SmokeCloudItem(new Item.Properties().stacksTo(1).setId(SMOKE_CLOUD_KEY))
    );
    public static final Item AURA_RELIC = Registry.register(
            BuiltInRegistries.ITEM, AURA_KEY,
            new AuraItem(new Item.Properties().stacksTo(1).setId(AURA_KEY))
    );
    public static final Item WARRIOR_LEAP = Registry.register(
            BuiltInRegistries.ITEM, LEAP_KEY,
            new LeapItem(new Item.Properties().stacksTo(1).setId(LEAP_KEY))
    );
    public static final Item TIME_BUBBLE = Registry.register(
            BuiltInRegistries.ITEM, TIME_BUBBLE_KEY,
            new TimeBubbleItem(new Item.Properties().stacksTo(1).setId(TIME_BUBBLE_KEY))
    );
    public static final Item FAN_ARROWS = Registry.register(
            BuiltInRegistries.ITEM, FAN_ARROWS_KEY,
            new FanArrowsItem(new Item.Properties().stacksTo(1).setId(FAN_ARROWS_KEY))
    );
    public static final Item SACRIFICE_RELIC = Registry.register(
            BuiltInRegistries.ITEM, SACRIFICE_KEY,
            new SacrificeRelicItem(new Item.Properties().stacksTo(1).setId(SACRIFICE_KEY))
    );

    private ModItems() { }

    public static void register() { /* инициализация через загрузку класса */ }

    /** Выдаёт игроку классовый предмет при выборе класса. */
    public static void giveClassItem(ServerPlayer player, String playerClass) {
        Item item = switch (playerClass) {
            case "Жрец"     -> HEALING_STAFF;
            case "Воин"     -> WAR_CRY;
            case "Маг"      -> LUCK_SCROLL;
            case "Следопыт" -> RANGER_TRAP;
            default         -> null;
        };
        if (item == null) return;
        giveStack(player, new ItemStack(item));
    }

    /**
     * Выдаёт активный предмет за разблокированную ноду древа.
     * @param nodeId id активной ноды
     * @return {@code true} если предмет существует и был выдан
     */
    public static boolean giveNodeItem(ServerPlayer player, String nodeId) {
        Item item = switch (nodeId) {
            case "w_cmd_shield"       -> WARRIOR_SHIELD;
            case "m_teleport"         -> TELEPORT_ROD;
            case "r_trap_smoke"       -> SMOKE_CLOUD;
            case "p_aura"             -> AURA_RELIC;
            case "w_dom_leap"         -> WARRIOR_LEAP;
            case "m_time_bubble"      -> TIME_BUBBLE;
            case "r_fan_arrows"       -> FAN_ARROWS;
            case "p_sacrifice_light"  -> SACRIFICE_RELIC;
            default                   -> null;
        };
        if (item == null) return false;
        // Защита от дублирования — не выдаём, если предмет уже в инвентаре
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == item) return false;
        }
        giveStack(player, new ItemStack(item));
        return true;
    }

    private static void giveStack(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(NS, name));
    }
}
