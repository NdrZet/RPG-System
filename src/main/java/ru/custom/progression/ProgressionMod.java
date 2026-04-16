package ru.custom.progression;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.custom.progression.StatEffects;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.commands.AdminCommands;
import ru.custom.progression.items.ModItems;
import ru.custom.progression.network.NetworkHandler;
import ru.custom.progression.storage.DataManager;

public class ProgressionMod implements ModInitializer {

    public static final String MOD_ID = "progression";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[Progression] Инициализация серверной части мода прогрессии...");

        ModItems.register();
        NetworkHandler.register();
        AdminCommands.register();
        registerJoinDisconnect();
        registerRespawnEffects();
        registerMobKillXp();
        registerPriestRegen();

        LOGGER.info("[Progression] Серверная часть мода прогрессии готова.");
    }

    // ── Подключение / отключение игрока ──────────────────────────────────────

    private static void registerJoinDisconnect() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            DataManager.setServer(server);
            PlayerStats stats = DataManager.loadPlayer(player.getUUID());
            // Пересчитываем ранг при входе — защита от рассинхрона данных
            stats.setRank(stats.calculateRank());
            server.execute(() -> {
                StatEffects.apply(player, stats);
                NetworkHandler.sendStatsToPlayer(player, stats);
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            DataManager.savePlayer(player.getUUID());
            DataManager.unloadPlayer(player.getUUID());
        });
    }

    // ── Повторное применение эффектов после респауна ─────────────────────────

    private static void registerRespawnEffects() {
        // AFTER_RESPAWN срабатывает в серверном потоке — вызываем напрямую
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PlayerStats stats = DataManager.getPlayer(newPlayer.getUUID());
            if (stats == null) return;
            // Атрибуты сбрасываются при смерти — восстанавливаем сразу
            StatEffects.apply(newPlayer, stats);
            // Устанавливаем полное HP с учётом нового максимума (VIT-бонус)
            newPlayer.setHealth(newPlayer.getMaxHealth());
            NetworkHandler.sendStatsToPlayer(newPlayer, stats);
        });
    }

    // ── XP за убийство мобов ─────────────────────────────────────────────────

    private static void registerMobKillXp() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed, damageSource) -> {
            // Только если убил игрок
            if (!(killer instanceof ServerPlayer player)) return;

            int xp = calculateXp(killed);
            if (xp <= 0) return;

            PlayerStats stats = DataManager.getPlayer(player.getUUID());
            if (stats == null) return;

            // XP-множитель для Мага зависит от тира
            if ("Маг".equals(stats.getPlayerClass())) {
                int lvl = stats.getLevel();
                int tier = lvl >= 100 ? 5 : lvl >= 70 ? 4 : lvl >= 40 ? 3 : lvl >= 20 ? 2 : 1;
                double mult = switch (tier) {
                    case 5 -> 5.0; case 4 -> 3.0; case 3 -> 2.0; case 2 -> 1.5; default -> 1.0;
                };
                xp = (int)(xp * mult);
            }

            int levelBefore = stats.getLevel();
            stats.addExperience(xp);
            int levelAfter  = stats.getLevel();

            // Уведомление при повышении уровня
            if (levelAfter > levelBefore) {
                DataManager.savePlayer(player.getUUID());
                player.sendSystemMessage(
                    Component.literal("⬆ Уровень повышен! ")
                             .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("Уровень " + levelAfter)
                             .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" | +2 очка навыков")
                             .withStyle(ChatFormatting.GREEN))
                );
                LOGGER.info("[Progression] {} достиг {} уровня",
                        player.getName().getString(), levelAfter);

                // Следопыт получает Ловушку на 20-м уровне
                if (levelAfter == 20 && "Следопыт".equals(stats.getPlayerClass())) {
                    ModItems.giveClassItem(player, "Следопыт");
                    player.sendSystemMessage(
                        Component.literal("🪤 Получена Ловушка Следопыта!")
                                 .withStyle(ChatFormatting.GREEN)
                    );
                }
            }

            // Всегда синхронизируем XP с клиентом
            NetworkHandler.sendStatsToPlayer(player, stats);
        });
    }

    // ── Пассивная регенерация Жреца ──────────────────────────────────────────

    private static void registerPriestRegen() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Каждые 100 тиков (5 сек) лечим всех онлайн-Жрецов
            if (server.getTickCount() % 100 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerStats stats = DataManager.getPlayer(player.getUUID());
                if (stats == null || !"Жрец".equals(stats.getPlayerClass())) continue;
                int lvl = stats.getLevel();
                int tier = lvl >= 100 ? 5 : lvl >= 70 ? 4 : lvl >= 40 ? 3 : lvl >= 20 ? 2 : 1;
                float heal = switch (tier) {
                    case 5 -> 6f; case 4 -> 4f; case 3 -> 2f; case 2 -> 1f; default -> 0f;
                };
                if (heal > 0 && player.getHealth() < player.getMaxHealth()) {
                    player.heal(heal);
                }
            }
        });
    }

    /**
     * XP за моба = 2 × макс. здоровье, минимум 5, максимум 500.
     * Убийство игроков XP не даёт.
     */
    private static int calculateXp(LivingEntity mob) {
        if (mob instanceof Player) return 0;
        return Math.min(500, Math.max(5, (int)(mob.getMaxHealth() * 2)));
    }
}
