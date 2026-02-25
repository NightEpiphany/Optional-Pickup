package com.moigferdsrte.optional_pickup.mixin;

import com.moigferdsrte.optional_pickup.client.OptionalPickupHud;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public class GuiRenderMixin {
    @Inject(method = "render", at = @At(value = "HEAD"))
    public void render(GuiGraphics drawContext, DeltaTracker tickCounter, CallbackInfo callbackInfo) {
        OptionalPickupHud.render(drawContext, tickCounter);
    }
}
