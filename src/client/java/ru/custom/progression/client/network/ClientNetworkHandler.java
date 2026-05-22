package ru.custom.progression.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ru.custom.progression.api.ClientStatsCache;
import ru.custom.progression.network.ScreenDistortionPayload;

public class ClientNetworkHandler {
    
    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ScreenDistortionPayload.ID, (payload, context) -> {
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
    }
}
