package com.moigferdsrte.optional_pickup.client;

import com.moigferdsrte.optional_pickup.config.OptionalPickupConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class OptionalPickupClient implements ClientModInitializer {
	private static final KeyMapping PICKUP_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.optional_pickup.pickup",
			GLFW.GLFW_KEY_R,
			"key.categories.optional_pickup"
	));

	public static ConfigHolder<OptionalPickupConfig> configHolder;
	public static OptionalPickupConfig config;

	public static Component getPickupKeyDisplay() {
		return PICKUP_KEY.getTranslatedKeyMessage();
	}

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);

		AutoConfig.register(OptionalPickupConfig.class, GsonConfigSerializer::new);
		configHolder = AutoConfig.getConfigHolder(OptionalPickupConfig.class);
		config = configHolder.getConfig();
	}

	private void onEndClientTick(Minecraft minecraft) {
		DropSelectionState.get().tick(minecraft);

		Screen screen = minecraft.screen;
		if (screen != null) {
			return;
		}

		while (PICKUP_KEY.consumeClick()) {
			DropSelectionState.get().requestPickup();
		}
	}
}
