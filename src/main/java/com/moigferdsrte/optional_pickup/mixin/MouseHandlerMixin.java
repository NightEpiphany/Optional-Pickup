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
	private void optional_pickup$scrollSelect(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		if (yoffset == 0.0) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) {
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

		if (state.scroll(yoffset)) {
			ci.cancel();
		}
	}
}
