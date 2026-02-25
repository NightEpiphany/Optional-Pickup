package com.moigferdsrte.optional_pickup;

import com.moigferdsrte.optional_pickup.network.PickupRequestC2SPayload;
import com.moigferdsrte.optional_pickup.pickup.ServerPickupService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionalPickup implements ModInitializer {
	public static final String MOD_ID = "optional_pickup";
	@SuppressWarnings("unused")
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(PickupRequestC2SPayload.TYPE, PickupRequestC2SPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(PickupRequestC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			Entity entity = player.level().getEntity(payload.entityId());

			if (!(entity instanceof ItemEntity itemEntity)) {
				return;
			}

			if (!itemEntity.closerThan(player, 3.0)) {
				return;
			}

			ServerPickupService.tryPickup(player, itemEntity);
		});
	}
}
