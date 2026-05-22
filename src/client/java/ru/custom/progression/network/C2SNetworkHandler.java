package ru.custom.progression.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Утилитарный класс для отправки C2S-пакетов с клиента на сервер.
 */
public final class C2SNetworkHandler {

    private C2SNetworkHandler() { }

    public static void register() {
        // S2C receivers are registered separately in client init
    }

    public static void sendStatUpgrade(String statName) {
        ClientPlayNetworking.send(new StatUpgradePayload(statName));
    }

    public static void sendChooseClass(String className) {
        ClientPlayNetworking.send(new ChooseClassPayload(className));
    }

    public static void sendUnlockNode(String nodeId) {
        ClientPlayNetworking.send(new UnlockNodePayload(nodeId));
    }

    public static void sendResetSkills() {
        ClientPlayNetworking.send(new ResetSkillsPayload());
    }
}
