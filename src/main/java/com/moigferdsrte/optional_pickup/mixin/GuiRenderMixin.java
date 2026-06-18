package com.moigferdsrte.optional_pickup.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.moigferdsrte.optional_pickup.client.OptionalPickupHud;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public class GuiRenderMixin {
    @Inject(
            method = "extractRenderState", at = @At(
                value = "FIELD",
                target = "Lnet/minecraft/client/gui/Gui;overlay:Lnet/minecraft/client/gui/screens/Overlay;",
                ordinal = 0,
                shift = At.Shift.BEFORE,
                opcode = Opcodes.GETFIELD
            )
    )
    public void render(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci, @Local(name = "graphics") GuiGraphicsExtractor graphics) {
        OptionalPickupHud.render(graphics, deltaTracker);
    }
}
