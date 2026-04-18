package ru.custom.progression.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Пакет Клиент → Сервер: запрос на сброс всех активированных нод
 * древа навыков и возврат потраченных очков.
 */
public record ResetSkillsPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ResetSkillsPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("progression", "reset_skills")
            );

    public static final StreamCodec<FriendlyByteBuf, ResetSkillsPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {}, buf -> new ResetSkillsPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
