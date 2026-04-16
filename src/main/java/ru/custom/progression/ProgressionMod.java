package ru.custom.progression;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
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
        registerJoinDisconnect();
        registerMobKillXp();

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

    // ── XP за убийство мобов ─────────────────────────────────────────────────

    private static void registerMobKillXp() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed, damageSource) -> {
            // Только если убил игрок
            if (!(killer instanceof ServerPlayer player)) return;

            int xp = calculateXp(killed);
            if (xp <= 0) return;

            PlayerStats stats = DataManager.getPlayer(player.getUUID());
            if (stats == null) return;

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
            }

            // Всегда синхронизируем XP с клиентом
            NetworkHandler.sendStatsToPlayer(player, stats);
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
