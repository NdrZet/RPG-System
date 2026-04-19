package ru.custom.progression;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.custom.progression.api.ClientStatsCache;
import ru.custom.progression.gui.OmniscienceHud;
import ru.custom.progression.gui.StatsScreen;
import ru.custom.progression.network.ClientNetworkHandler;

/**
 * Точка входа клиентской части мода прогрессии (ClientModInitializer).
 * <p>
 * Инициализирует:
 * <ul>
 *   <li>Сетевые получатели S2C-пакетов через {@link ClientNetworkHandler}</li>
 *   <li>Сброс кэша статов при выходе из мира</li>
 *   <li>Горячую клавишу для открытия экрана прогрессии</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class ProgressionModClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("progression-client");

    private static KeyMapping openStatsKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Progression] Инициализация клиентской части мода прогрессии...");

        ClientNetworkHandler.register();
        OmniscienceHud.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientStatsCache.reset();
            LOGGER.debug("[Progression] Кэш статов очищен (отключение от сервера)");
        });

        // ── Горячая клавиша открытия экрана прогрессии (P) ────────────────
        openStatsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.progression.open_stats",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_P,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openStatsKey.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new StatsScreen());
                }
            }
        });

        LOGGER.info("[Progression] Клиентская часть мода прогрессии готова.");
    }
}
