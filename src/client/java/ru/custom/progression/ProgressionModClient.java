package ru.custom.progression;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.custom.progression.api.ClientStatsCache;
import ru.custom.progression.network.ClientNetworkHandler;

/**
 * Точка входа клиентской части мода прогрессии (ClientModInitializer).
 * <p>
 * Инициализирует:
 * <ul>
 *   <li>Сетевые получатели S2C-пакетов через {@link ClientNetworkHandler}</li>
 *   <li>Сброс кэша статов при выходе из мира</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class ProgressionModClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("progression-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Progression] Инициализация клиентской части мода прогрессии...");

        // ── Регистрируем получатель S2C-пакетов ────────────────────────────
        ClientNetworkHandler.register();

        // ── Очищаем кэш при выходе из мира ────────────────────────────────
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientStatsCache.reset();
            LOGGER.debug("[Progression] Кэш статов очищен (отключение от сервера)");
        });

        LOGGER.info("[Progression] Клиентская часть мода прогрессии готова.");
    }
}
