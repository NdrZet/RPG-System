package ru.custom.progression.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Пакет Клиент → Сервер: запрос на активацию ноды древа навыков по её id. */
public record UnlockNodePayload(String nodeId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UnlockNodePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("progression", "unlock_node")
            );

    public static final StreamCodec<FriendlyByteBuf, UnlockNodePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeUtf(p.nodeId()),
                    buf -> new UnlockNodePayload(buf.readUtf())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
