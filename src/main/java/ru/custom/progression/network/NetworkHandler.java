package ru.custom.progression.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.custom.progression.StatEffects;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.items.ModItems;
import ru.custom.progression.storage.DataManager;

/**
 * Серверная часть сетевого взаимодействия:
 * <ul>
 *   <li>Регистрация типов пакетов в {@link PayloadTypeRegistry}</li>
 *   <li>Обработка входящих C2S-пакетов (повышение стата, смена класса)</li>
 *   <li>Отправка S2C-пакетов обновления статов</li>
 * </ul>
 * Вызывается из {@link ru.custom.progression.ProgressionMod#onInitialize()}.
 */
public final class NetworkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("progression/NetworkHandler");

    private NetworkHandler() { }

    // ────────────────────────────────────────────────────────────────────────
    // Регистрация
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Регистрирует все типы пакетов и серверные обработчики входящих запросов.
     * Должен вызываться один раз при инициализации мода.
     */
    public static void register() {
        // ── Регистрируем типы пакетов в реестре ────────────────────────────

        // S2C — отправка данных статов клиенту
        PayloadTypeRegistry.playS2C().register(
                StatsUpdatePayload.TYPE,
                StatsUpdatePayload.STREAM_CODEC
        );

        // C2S — повышение стата
        PayloadTypeRegistry.playC2S().register(
                StatUpgradePayload.TYPE,
                StatUpgradePayload.STREAM_CODEC
        );

        // C2S — смена класса
        PayloadTypeRegistry.playC2S().register(
                ChooseClassPayload.TYPE,
                ChooseClassPayload.STREAM_CODEC
        );

        // ── Регистрируем серверные обработчики ─────────────────────────────
        registerStatUpgradeHandler();
        registerChooseClassHandler();

        LOGGER.info("[Progression] Сетевые обработчики зарегистрированы");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Обработчики C2S
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Обрабатывает запрос клиента на повышение стата.
     * Проверяет наличие очков навыков и применяет изменение на сервере.
     */
    private static void registerStatUpgradeHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                StatUpgradePayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();

                    // Выполняем логику строго в серверном потоке
                    context.server().execute(() -> {
                        PlayerStats stats = DataManager.getPlayer(player.getUUID());
                        if (stats == null) {
                            LOGGER.warn("[Progression] Данные игрока {} не найдены в кэше", player.getName().getString());
                            return;
                        }

                        boolean upgraded = stats.upgradeStat(payload.statName());
                        if (upgraded) {
                            LOGGER.debug("[Progression] {} повысил стат: {}", player.getName().getString(), payload.statName());
                            StatEffects.apply(player, stats);
                            // Сохраняем и синхронизируем с клиентом
                            DataManager.savePlayer(player.getUUID());
                            sendStatsToPlayer(player, stats);
                        } else {
                            LOGGER.warn("[Progression] {} не может повысить стат {} (недостаточно очков)",
                                    player.getName().getString(), payload.statName());
                        }
                    });
                }
        );
    }

    /**
     * Обрабатывает запрос клиента на смену класса.
     * Класс можно сменить только с 5-го уровня и только один раз (пока Странник).
     */
    private static void registerChooseClassHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                ChooseClassPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();

                    context.server().execute(() -> {
                        PlayerStats stats = DataManager.getPlayer(player.getUUID());
                        if (stats == null) return;

                        // Дополнительная проверка: менять класс может только Странник (уровень >= 5)
                        if (!"Странник".equals(stats.getPlayerClass())) {
                            LOGGER.warn("[Progression] {} уже выбрал класс: {}", player.getName().getString(), stats.getPlayerClass());
                            return;
                        }

                        boolean changed = stats.chooseClass(payload.chosenClass());
                        if (changed) {
                            LOGGER.info("[Progression] {} сменил класс на: {}", player.getName().getString(), payload.chosenClass());
                            StatEffects.apply(player, stats);
                            ModItems.giveClassItem(player, stats.getPlayerClass());
                            DataManager.savePlayer(player.getUUID());
                            sendStatsToPlayer(player, stats);
                        }
                    });
                }
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // Утилиты отправки S2C
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Отправляет актуальные данные прогрессии конкретному игроку.
     * Должен вызываться из серверного потока.
     *
     * @param player целевой игрок
     * @param stats  актуальные данные
     */
    public static void sendStatsToPlayer(ServerPlayer player, PlayerStats stats) {
        ServerPlayNetworking.send(player, StatsUpdatePayload.from(stats));
    }
}
