package ru.custom.progression.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.custom.progression.api.ClientStatsCache;
import ru.custom.progression.network.ScreenDistortionPayload;
import ru.custom.progression.network.StatsUpdatePayload;

public class ClientNetworkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("progression-client-net");
    
    public static void registerReceivers() {
        LOGGER.info("[Progression] Registering S2C packet receivers...");

        // NOTE: PayloadTypeRegistry.playS2C().register() is called in NetworkHandler.register()
        // (server-side main entrypoint), which also runs on client in singleplayer.
        // Calling it again here would cause "already registered" exception.
        // We only register the receivers here.

        // S2C: обновление статов игрока
        ClientPlayNetworking.registerGlobalReceiver(StatsUpdatePayload.TYPE, (payload, context) -> {
            LOGGER.info("[Progression] Received StatsUpdatePayload: level={}, class={}", payload.level(), payload.playerClass());
            context.client().execute(() -> {
                ClientStatsCache.update(payload.toStats());
                LOGGER.info("[Progression] ClientStatsCache updated: level={}", ClientStatsCache.get().getLevel());
            });
        });
        LOGGER.info("[Progression] Registered StatsUpdatePayload receiver");

        // S2C: эффекты искажения экрана (фейковое здоровье, инвертированные управление, глухота)
        ClientPlayNetworking.registerGlobalReceiver(ScreenDistortionPayload.ID, (payload, context) -> {
            LOGGER.info("[Progression] Received ScreenDistortionPayload: type={}, duration={}", payload.effectType(), payload.durationMs());
            context.client().execute(() -> {
                int type = payload.effectType();
                int duration = payload.durationMs();
                
                switch (type) {
                    case 0 -> { // FAKE_HEALTH
                        ClientStatsCache.fakeHealthEnabled = true;
                        ClientStatsCache.fakeHealthTimerMs = duration;
                    }
                    case 1 -> { // INVERT_CONTROLS
                        ClientStatsCache.controlsInverted = true;
                        ClientStatsCache.invertTimerMs = duration;
                    }
                    case 2 -> { // DEAFNESS
                        context.client().getSoundManager().stop();
                        // Custom sound logic can go here
                    }
                }
            });
        });
        LOGGER.info("[Progression] Registered ScreenDistortionPayload receiver");
    }
}
