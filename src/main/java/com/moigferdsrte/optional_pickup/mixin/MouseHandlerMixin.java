package com.moigferdsrte.optional_pickup.mixin;

import com.moigferdsrte.optional_pickup.client.DropSelectionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void optional_pickup$scrollSelect(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (vertical == 0.0) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
			return;
		}

		if (!minecraft.player.isShiftKeyDown()) {
			return;
		}

		DropSelectionState state = DropSelectionState.get();
		if (!state.isActive()) {
			return;
		}
		if (state.getOptionsView().size() <= 1)
			return;

		if (state.scroll(vertical)) {
			ci.cancel();
		}
	}
}
