package com.moigferdsrte.optional_pickup.client;

import com.moigferdsrte.optional_pickup.config.OptionalPickupConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class OptionalPickupClient implements ClientModInitializer {
	private static final KeyMapping PICKUP_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.optional_pickup.pickup",
			21,
			new KeyMapping.Category(
					Identifier.fromNamespaceAndPath("optional_pickup", "key.categories.optional_pickup")
			)
	));

	public static ConfigHolder<OptionalPickupConfig> configHolder;
	public static OptionalPickupConfig config;
	private static Set<Identifier> tooltipPriorityItemIds = Set.of();

	public static Component getPickupKeyDisplay() {
		return PICKUP_KEY.getTranslatedKeyMessage();
	}

	public static boolean shouldPreferTooltip(Item item) {
		return tooltipPriorityItemIds.contains(BuiltInRegistries.ITEM.getKey(item));
	}

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);

		AutoConfig.register(OptionalPickupConfig.class, GsonConfigSerializer::new);
		configHolder = AutoConfig.getConfigHolder(OptionalPickupConfig.class);
		updateConfig(configHolder.getConfig());
		configHolder.registerSaveListener((holder, updatedConfig) -> {
			updateConfig(updatedConfig);
			return InteractionResult.PASS;
		});
		configHolder.registerLoadListener((holder, updatedConfig) -> {
			updateConfig(updatedConfig);
			return InteractionResult.PASS;
		});
	}

	private static void updateConfig(OptionalPickupConfig updatedConfig) {
		config = updatedConfig;
		if (updatedConfig.tooltipPriorityItems == null) {
			tooltipPriorityItemIds = Set.of();
			return;
		}

		// 保存或加载配置时解析物品 ID，避免每次扫描掉落物都遍历配置列表。
		Set<Identifier> itemIds = HashSet.newHashSet(updatedConfig.tooltipPriorityItems.size());
		for (String itemId : updatedConfig.tooltipPriorityItems) {
			if (itemId == null) {
				continue;
			}
			Identifier id = Identifier.tryParse(itemId.trim());
			if (id != null) {
				itemIds.add(id);
			}
		}
		tooltipPriorityItemIds = Set.copyOf(itemIds);
	}

	private void onEndClientTick(Minecraft minecraft) {
		DropSelectionState.get().tick(minecraft);

		Screen screen = minecraft.gui.screen();
		if (screen != null) {
			return;
		}

		while (PICKUP_KEY.consumeClick()) {
			DropSelectionState.get().requestPickup();
		}
	}
}
