package com.moigferdsrte.optional_pickup.pickup;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class ServerPickupService {
	private static final ThreadLocal<Request> CURRENT = new ThreadLocal<>();

	private ServerPickupService() {
	}

	public static boolean isManualPickup(Player player, ItemEntity itemEntity) {
		Request request = CURRENT.get();
		return request != null && request.playerUuid.equals(player.getUUID()) && request.entityId == itemEntity.getId();
	}

	public static void tryPickup(ServerPlayer player, ItemEntity itemEntity) {
		if (player.isRemoved() || itemEntity.isRemoved()) {
			return;
		}

		CURRENT.set(new Request(player.getUUID(), itemEntity.getId()));
		try {
			itemEntity.playerTouch(player);
		} finally {
			CURRENT.remove();
		}
	}

	private record Request(UUID playerUuid, int entityId) {
	}
}
