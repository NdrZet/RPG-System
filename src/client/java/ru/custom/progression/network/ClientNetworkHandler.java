package ru.custom.progression.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.custom.progression.api.ClientStatsCache;

/**
 * Клиентская часть сетевого взаимодействия.
 * <p>
 * Отвечает за:
 * <ul>
 *   <li>Получение S2C-пакетов и обновление {@link ClientStatsCache}</li>
 *   <li>Отправку C2S-запросов (повышение стата, смена класса)</li>
 * </ul>
 * Вызывается из {@link ru.custom.progression.ProgressionModClient#onInitializeClient()}.
 */
@Environment(EnvType.CLIENT)
public final class ClientNetworkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("progression/ClientNetwork");

    private ClientNetworkHandler() { }

    // ────────────────────────────────────────────────────────────────────────
    // Регистрация клиентских получателей
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Регистрирует обработчик входящего S2C-пакета с данными статов.
     * Должен вызываться при инициализации клиента.
     */
    public static void register() {
        // Получаем данные с сервера и обновляем локальный кэш
        ClientPlayNetworking.registerGlobalReceiver(
                StatsUpdatePayload.TYPE,
                (payload, context) -> {
                    // Переносим обновление в render-поток
                    context.client().execute(() -> {
                        ClientStatsCache.update(payload.toStats());
                        LOGGER.debug("[Progression] Данные статов обновлены: уровень={}, класс={}",
                                payload.level(), payload.playerClass());
                    });
                }
        );

        LOGGER.info("[Progression] Клиентские сетевые обработчики зарегистрированы");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Утилиты отправки C2S
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Отправляет серверу запрос на повышение указанного стата.
     * Безопасно вызывать из render-потока (ClientPlayNetworking обрабатывает поточность).
     *
     * @param statName название стата: "strength", "agility", "vitality", "intelligence"
     */
    public static void sendStatUpgrade(String statName) {
        ClientPlayNetworking.send(new StatUpgradePayload(statName));
    }

    /**
     * Отправляет серверу запрос на смену класса персонажа.
     *
     * @param chosenClass название выбранного класса
     */
    public static void sendChooseClass(String chosenClass) {
        ClientPlayNetworking.send(new ChooseClassPayload(chosenClass));
    }
}
