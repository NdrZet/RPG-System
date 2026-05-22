package ru.custom.progression.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload for sending screen distortion effects to the client (e.g., fake health, inverted controls).
 */
public record ScreenDistortionPayload(int effectType, int durationMs) implements CustomPacketPayload {
    public static final Identifier ID_LOC = Identifier.fromNamespaceAndPath("sparpg", "screen_distortion");
    public static final Type<ScreenDistortionPayload> ID = new Type<>(ID_LOC);

    public static final StreamCodec<FriendlyByteBuf, ScreenDistortionPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, ScreenDistortionPayload::effectType,
        ByteBufCodecs.INT, ScreenDistortionPayload::durationMs,
        ScreenDistortionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { 
        return ID; 
    }
}
