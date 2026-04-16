package ru.custom.progression.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Пакет Клиент → Сервер: запрос на повышение одного из четырёх базовых статов.
 * Отправляется при нажатии кнопки «+» рядом со статом в инвентаре.
 * <p>
 * Значение {@code statName} — одно из: "strength", "agility", "vitality", "intelligence".
 */
public record StatUpgradePayload(String statName) implements CustomPacketPayload {

    /** Уникальный идентификатор пакета. */
    public static final CustomPacketPayload.Type<StatUpgradePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("progression", "stat_upgrade")
            );

    /** Кодек: запись / чтение одного строкового поля. */
    public static final StreamCodec<FriendlyByteBuf, StatUpgradePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeUtf(payload.statName()),
                    buf -> new StatUpgradePayload(buf.readUtf())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
