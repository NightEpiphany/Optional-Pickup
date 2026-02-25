package com.moigferdsrte.optional_pickup.network;

import com.moigferdsrte.optional_pickup.OptionalPickup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PickupRequestC2SPayload(int entityId) implements CustomPacketPayload {
	public static final ResourceLocation PICKUP_REQUEST_ID = ResourceLocation.fromNamespaceAndPath(OptionalPickup.MOD_ID, "pickup_request");
	public static final Type<PickupRequestC2SPayload> TYPE = new Type<>(PICKUP_REQUEST_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, PickupRequestC2SPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			PickupRequestC2SPayload::entityId,
			PickupRequestC2SPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
