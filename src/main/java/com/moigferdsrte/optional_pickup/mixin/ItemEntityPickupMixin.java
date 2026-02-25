package com.moigferdsrte.optional_pickup.mixin;

import com.moigferdsrte.optional_pickup.pickup.ServerPickupService;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityPickupMixin {
	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void optional_pickup$blockAutoPickup(Player player, CallbackInfo ci) {
		if (ServerPickupService.isManualPickup(player, (ItemEntity) (Object) this)) {
			return;
		}

		ci.cancel();
	}
}
