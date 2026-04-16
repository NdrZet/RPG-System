package ru.custom.progression.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Пакет Клиент → Сервер: запрос на смену класса персонажа.
 * Отправляется при выборе класса в меню «ВЫБРАТЬ ПУТЬ» (доступно с 5 уровня).
 * <p>
 * Значение {@code chosenClass} — название выбранного класса, например:
 * "Воин", "Маг", "Следопыт", "Жрец".
 */
public record ChooseClassPayload(String chosenClass) implements CustomPacketPayload {

    /** Уникальный идентификатор пакета. */
    public static final CustomPacketPayload.Type<ChooseClassPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("progression", "choose_class")
            );

    /** Кодек: запись / чтение одного строкового поля. */
    public static final StreamCodec<FriendlyByteBuf, ChooseClassPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeUtf(payload.chosenClass()),
                    buf -> new ChooseClassPayload(buf.readUtf())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
