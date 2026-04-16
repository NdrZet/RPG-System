package com.spa.sound;

import com.spa.sound.music.MusicFadeManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class SpasoundClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Drive the fade manager every client tick
        ClientTickEvents.END_CLIENT_TICK.register(MusicFadeManager::tick);

        // Reset state cleanly when leaving a world
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MusicFadeManager.reset());
    }
}
