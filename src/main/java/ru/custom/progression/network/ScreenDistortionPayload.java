package ru.custom.progression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload for sending screen distortion effects to the client (e.g., fake health, inverted controls).
 */
public record ScreenDistortionPayload(int effectType, int durationMs) implements CustomPacketPayload {
    public static final ResourceLocation ID_LOC = ResourceLocation.fromNamespaceAndPath("sparpg", "screen_distortion");
    public static final Type<ScreenDistortionPayload> ID = new Type<>(ID_LOC);

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenDistortionPayload> CODEC = StreamCodec.of(
        ScreenDistortionPayload::write, ScreenDistortionPayload::new
    );

    public ScreenDistortionPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(effectType);
        buf.writeInt(durationMs);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { 
        return ID; 
    }
}
