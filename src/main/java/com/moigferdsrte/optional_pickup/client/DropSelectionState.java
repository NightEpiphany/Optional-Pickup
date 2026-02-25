package com.moigferdsrte.optional_pickup.client;

import com.moigferdsrte.optional_pickup.network.PickupRequestC2SPayload;
import com.moigferdsrte.optional_pickup.util.OverwrittenJudge;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DropSelectionState {
	private static final double SCAN_RANGE = 1.02D;
	private static final int SCAN_INTERVAL_TICKS = 2;
	private static final int MAX_VISIBLE = 9;
	private static final float VISIBILITY_LERP = 0.42F;
	private static final float LAYOUT_LERP = 0.50F;
	private static final DropSelectionState INSTANCE = new DropSelectionState();

	private final List<DropOption> options = new ArrayList<>();
	private final List<ItemStack> prevLayoutKeys = new ArrayList<>();
	private int selectedIndex = 0;
	private int tickCounter = 0;
	private float prevVisibility = 0.0F;
	private float visibility = 0.0F;
	private boolean wantsVisible = false;
	private int prevWindowStartIndex = 0;
	private int prevVisibleCount = 0;
	private long optionsSignature = 0L;
	private float prevLayoutProgress = 1.0F;
	private float layoutProgress = 1.0F;

	private DropSelectionState() {
	}

	public static DropSelectionState get() {
		return INSTANCE;
	}

	public void tick(Minecraft minecraft) {
		if (minecraft.level == null) {
			this.options.clear();
			this.selectedIndex = 0;
			this.prevVisibility = 0.0F;
			this.visibility = 0.0F;
			this.wantsVisible = false;
			return;
		}

		LocalPlayer player = minecraft.player;
		if (player == null) {
			this.options.clear();
			this.selectedIndex = 0;
			this.prevVisibility = 0.0F;
			this.visibility = 0.0F;
			this.wantsVisible = false;
			return;
		}

		this.tickCounter++;
		this.prevVisibility = this.visibility;
		this.prevLayoutProgress = this.layoutProgress;
		boolean doScan = this.tickCounter % SCAN_INTERVAL_TICKS == 0;

		if (doScan) {
			List<ItemStack> oldKeys = snapshotKeys(this.options);
			int oldWindowStart = this.getWindowStartIndex();
			int oldVisibleCount = this.getVisibleCount();

			ItemStack previouslySelected = this.getSelectedKeyStack();
			AABB range = player.getBoundingBox().inflate(SCAN_RANGE);
			List<ItemEntity> entities = minecraft.level.getEntitiesOfClass(ItemEntity.class, range, e -> !e.isRemoved() && !e.getItem().isEmpty());

			Map<StackKey, Group> groups = new HashMap<>();
			for (ItemEntity entity : entities) {
				ItemStack stack = entity.getItem();
				if (stack.isEmpty()) {
					continue;
				}

				ItemStack keyStack = stack.copyWithCount(1);
				StackKey key = new StackKey(keyStack);
				Group group = groups.computeIfAbsent(key, k -> new Group(keyStack));
				group.totalCount += stack.getCount();

				double distSq = entity.distanceToSqr(player);
				if (distSq < group.nearestDistSq) {
					group.nearestDistSq = distSq;
					group.nearestEntityId = entity.getId();
				}
			}

			List<DropOption> nextOptions = new ArrayList<>();
			for (Group group : groups.values()) {
				if (group.nearestEntityId == -1) {
					continue;
				}

			ItemStack displayStack = group.keyStack.copyWithCount(1);
				Component displayName = computeDisplayName(minecraft, player, group.keyStack);

				nextOptions.add(new DropOption(
						group.nearestEntityId,
						group.keyStack,
						displayStack,
						group.totalCount,
						categorize(displayStack),
						displayName
				));
			}

			boolean found = !nextOptions.isEmpty();
			this.wantsVisible = found;

			if (found) {
				this.options.clear();
				this.options.addAll(nextOptions);
			} else {
				if (this.visibility == 0.0F && this.prevVisibility == 0.0F) {
					this.options.clear();
					this.selectedIndex = 0;
				}
			}

			if (this.options.isEmpty()) {
				return;
			}

			this.options.sort(Comparator
					.comparingInt((DropOption o) -> o.category.sortKey())
					.thenComparing(o -> o.displayName.getString(), String.CASE_INSENSITIVE_ORDER)
			);

			this.selectedIndex = clampIndex(this.selectedIndex, this.options.size());

			if (previouslySelected != null) {
				int idx = indexOfKey(previouslySelected, this.options);
				if (idx != -1) {
					this.selectedIndex = idx;
				}
			}

			long newSignature = computeSignature(this.options);
			if (this.optionsSignature == 0L) {
				this.optionsSignature = newSignature;
				this.prevLayoutKeys.clear();
				this.prevLayoutKeys.addAll(snapshotKeys(this.options));
				this.prevWindowStartIndex = this.getWindowStartIndex();
				this.prevVisibleCount = this.getVisibleCount();
				this.layoutProgress = 1.0F;
				this.prevLayoutProgress = 1.0F;
			} else if (newSignature != this.optionsSignature) {
				this.optionsSignature = newSignature;
				this.prevLayoutKeys.clear();
				this.prevLayoutKeys.addAll(oldKeys);
				this.prevWindowStartIndex = oldWindowStart;
				this.prevVisibleCount = oldVisibleCount;
				this.layoutProgress = 0.0F;
			}
		}

		float target = this.wantsVisible ? 1.0F : 0.0F;
		if (this.visibility != target) {
			this.visibility = this.visibility + (target - this.visibility) * VISIBILITY_LERP;
			if (Math.abs(this.visibility - target) < 0.001F) {
				this.visibility = target;
			}
		}

		if (this.layoutProgress < 1.0F) {
			this.layoutProgress = this.layoutProgress + (1.0F - this.layoutProgress) * LAYOUT_LERP;
			if (this.layoutProgress > 0.999F) {
				this.layoutProgress = 1.0F;
			}
		}

		if (!doScan && !this.wantsVisible && this.visibility == 0.0F && this.prevVisibility == 0.0F) {
			this.options.clear();
			this.selectedIndex = 0;
		}
	}

	public boolean isActive() {
		return this.wantsVisible && !this.options.isEmpty();
	}

	public boolean shouldRender() {
		return this.visibility > 0.0F && !this.options.isEmpty();
	}

	public float getVisibility() {
		return this.visibility;
	}

	public float getPrevVisibility() {
		return this.prevVisibility;
	}

	public float getLayoutProgress() {
		return this.layoutProgress;
	}

	public float getPrevLayoutProgress() {
		return this.prevLayoutProgress;
	}

	public int getPrevWindowStartIndex() {
		return this.prevWindowStartIndex;
	}

	public int getPrevVisibleCount() {
		return this.prevVisibleCount;
	}

	public int getPrevIndexFor(ItemStack keyStack) {
		for (int i = 0; i < this.prevLayoutKeys.size(); i++) {
			if (ItemStack.isSameItemSameComponents(keyStack, this.prevLayoutKeys.get(i))) {
				return i;
			}
		}
		return -1;
	}

	public int getVisibleCount() {
		return Math.min(MAX_VISIBLE, this.options.size());
	}

	public int getWindowStartIndex() {
		int size = this.options.size();
		if (size <= MAX_VISIBLE) {
			return 0;
		}

		int start = this.selectedIndex - (MAX_VISIBLE / 2);
		if (start < 0) {
			return 0;
		}

		int maxStart = size - MAX_VISIBLE;
		return Math.min(start, maxStart);
	}

	public List<DropOption> getOptionsView() {
		return this.options;
	}

	public int getSelectedIndex() {
		return this.selectedIndex;
	}

	public void requestPickup() {
		if (this.options.isEmpty()) {
			return;
		}

		DropOption selected = this.options.get(this.selectedIndex);
		if (!ClientPlayNetworking.canSend(PickupRequestC2SPayload.TYPE)) {
			return;
		}

		ClientPlayNetworking.send(new PickupRequestC2SPayload(selected.entityId));
	}

	public boolean scroll(double vertical) {
		if (this.options.isEmpty()) {
			return false;
		}

		int direction = vertical > 0 ? -1 : 1;
		int size = this.options.size();
		this.selectedIndex = Math.floorMod(this.selectedIndex + direction, size);
		return true;
	}

	private ItemStack getSelectedKeyStack() {
		if (this.options.isEmpty() || this.selectedIndex < 0 || this.selectedIndex >= this.options.size()) {
			return null;
		}
		return this.options.get(this.selectedIndex).keyStack;
	}

	private static int clampIndex(int index, int size) {
		if (size <= 0) {
			return 0;
		}
		return Math.min(Math.max(index, 0), size - 1);
	}

	private static int indexOfKey(ItemStack keyStack, List<DropOption> options) {
		for (int i = 0; i < options.size(); i++) {
			if (ItemStack.isSameItemSameComponents(keyStack, options.get(i).keyStack)) {
				return i;
			}
		}
		return -1;
	}

	private static List<ItemStack> snapshotKeys(List<DropOption> options) {
		List<ItemStack> out = new ArrayList<>(options.size());
		for (DropOption option : options) {
			out.add(option.keyStack);
		}
		return out;
	}

	private static long computeSignature(List<DropOption> options) {
		long h = 1469598103934665603L;
		for (DropOption option : options) {
			h ^= ItemStack.hashItemAndComponents(option.keyStack);
			h *= 1099511628211L;
		}
		return h;
	}

	private static DropCategory categorize(ItemStack stack) {
		Item item = stack.getItem();
        switch (item) {
            case SwordItem ignored -> {
                return DropCategory.COMBAT;
            }
            case TieredItem ignored -> {
                return DropCategory.TOOLS;
            }
            case ArmorItem ignored -> {
                return DropCategory.ARMOR;
            }
            case BlockItem ignored -> {
                return DropCategory.BLOCKS;
            }
            default -> {
            }
        }
        if (stack.getComponents().has(DataComponents.FOOD)
				|| item == Items.POTION
				|| item == Items.SPLASH_POTION
				|| item == Items.LINGERING_POTION
				|| item == Items.SUSPICIOUS_STEW
				|| item == Items.MILK_BUCKET
				|| item == Items.HONEY_BOTTLE) {
			return DropCategory.FOOD;
		}
		return DropCategory.MISC;
	}

	private static Component computeDisplayName(Minecraft minecraft, LocalPlayer player, ItemStack keyStack) {
		Item item = keyStack.getItem();
		if (minecraft.level == null) {
			return keyStack.getHoverName();
		}
		if (
				item instanceof HangingEntityItem || item instanceof EnchantedBookItem || item instanceof BannerPatternItem || item instanceof InstrumentItem || item instanceof SuspiciousStewItem ||
				OverwrittenJudge.isHoverTextOverridden(item.getClass())
		) {
			List<Component> tooltip = keyStack.getTooltipLines(TooltipContext.of(minecraft.level), player, TooltipFlag.NORMAL);
			if (tooltip.size() >= 2) {
				if (item == Items.PAINTING) {
					return tooltip.get(1);
				}

				Component first = tooltip.get(1);
				int extra = tooltip.size() - 2;
				if (extra > 0) {
					return Component.empty().append(first).append(Component.literal(" +" + extra));
				}
				return first;
			}
		}

		return keyStack.getHoverName();
	}

	private static final class StackKey {
		private final ItemStack stack;
		private final int hash;

		private StackKey(ItemStack stack) {
			this.stack = stack;
			this.hash = ItemStack.hashItemAndComponents(stack);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof StackKey other)) {
				return false;
			}
			return ItemStack.isSameItemSameComponents(this.stack, other.stack);
		}

		@Override
		public int hashCode() {
			return this.hash;
		}
	}

	private static final class Group {
		private final ItemStack keyStack;
		private int totalCount = 0;
		private int nearestEntityId = -1;
		private double nearestDistSq = Double.MAX_VALUE;

		private Group(ItemStack keyStack) {
			this.keyStack = keyStack;
		}
	}

	public record DropOption(int entityId, ItemStack keyStack, ItemStack displayStack, int totalCount, DropCategory category, Component displayName) {
	}
}
