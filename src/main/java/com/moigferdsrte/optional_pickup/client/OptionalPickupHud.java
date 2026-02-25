package com.moigferdsrte.optional_pickup.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class OptionalPickupHud {
	private static final int MARGIN = 8;
	private static final int BOX_WIDTH = 140;
	private static final int BOX_HEIGHT = 22;
	private static final int BOX_SPACING = 3;
	private static final int PADDING = 3;
	private static final int ANIM_Y_SHIFT = 12;
	private static final float ICON_ALPHA_CUTOFF = 0.18F;

	private OptionalPickupHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null) {
			return;
		}

		DropSelectionState state = DropSelectionState.get();
		if (!state.shouldRender()) {
			return;
		}

		float partial = clamp01(deltaTracker.getGameTimeDeltaPartialTick(true));
		float visibility = clamp01(lerp(state.getPrevVisibility(), state.getVisibility(), partial));
		boolean fadingIn = state.getVisibility() >= state.getPrevVisibility();
		float eased = fadingIn ? easeOutExpo(visibility) : easeInExpo(visibility);
		float layoutRaw = clamp01(lerp(state.getPrevLayoutProgress(), state.getLayoutProgress(), partial));
		float layoutT = easeOutExpo(layoutRaw);
		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();

		int visible = state.getVisibleCount();
		int prevVisible = state.getPrevVisibleCount() > 0 ? state.getPrevVisibleCount() : visible;
		int start = state.getWindowStartIndex();
		int prevStart = state.getPrevWindowStartIndex();
		int selected = state.getSelectedIndex();
		List<DropSelectionState.DropOption> options = state.getOptionsView();

		int totalHeight = visible * BOX_HEIGHT + (visible - 1) * BOX_SPACING;
		int prevTotalHeight = prevVisible * BOX_HEIGHT + (prevVisible - 1) * BOX_SPACING;
		int rightX = width - MARGIN - BOX_WIDTH;
		int leftX = MARGIN;
		int xPercent = OptionalPickupClient.config != null ? OptionalPickupClient.config.offsetX : 0;
		float t = Math.max(0.0F, Math.min(1.0F, xPercent / 100.0F));
		int x0 = Math.round(rightX + (leftX - rightX) * t);
		int minX = Math.min(leftX, rightX);
		int maxX = Math.max(leftX, rightX);
		x0 = Math.min(Math.max(x0, minX), maxX);

		int offsetY = OptionalPickupClient.config != null ? OptionalPickupClient.config.offsetY : 0;
		int yShift = Math.round((1.0F - eased) * ANIM_Y_SHIFT);
		int y0 = height - MARGIN - totalHeight - 20 - offsetY + yShift;
		int prevY0 = height - MARGIN - prevTotalHeight - 20 - offsetY + yShift;

		Font font = minecraft.font;
		Component pickupKey = OptionalPickupClient.getPickupKeyDisplay();
		Component pickupKeyLabel = Component.literal("[" + pickupKey.getString() + "]");
		int pickupKeyLabelWidth = font.width(pickupKeyLabel);

		for (int i = 0; i < visible; i++) {
			int idx = start + i;
			DropSelectionState.DropOption option = options.get(idx);

			int currentY = y0 + i * (BOX_HEIGHT + BOX_SPACING);
			int prevIndex = state.getPrevIndexFor(option.keyStack());
			int prevY;
			if (prevIndex == -1) {
				prevY = currentY + (BOX_HEIGHT + BOX_SPACING);
			} else {
				int prevRow = prevIndex - prevStart;
				if (prevRow >= 0 && prevRow < prevVisible) {
					prevY = prevY0 + prevRow * (BOX_HEIGHT + BOX_SPACING);
				} else {
					prevY = currentY + (prevRow < 0 ? -(BOX_HEIGHT + BOX_SPACING) : (BOX_HEIGHT + BOX_SPACING));
				}
			}

			int y = Math.round(lerp(prevY, currentY, layoutT));
			boolean isSelected = idx == selected;

			float entryAlpha = prevIndex == -1 ? (eased * layoutT) : eased;
			int bgAlpha = Math.round(0xAA * entryAlpha);
			float textFactor = entryAlpha * entryAlpha;
			int textAlpha = Math.round(0xFF * textFactor);
			int borderAlpha = isSelected ? textAlpha : Math.round(0x55 * entryAlpha);
			int bg = (bgAlpha << 24);
			int border = (borderAlpha << 24) | 0xFFFFFF;

			graphics.fill(x0, y, x0 + BOX_WIDTH, y + BOX_HEIGHT, bg);
			graphics.fill(x0, y, x0 + BOX_WIDTH, y + 1, border);
			graphics.fill(x0, y + BOX_HEIGHT - 1, x0 + BOX_WIDTH, y + BOX_HEIGHT, border);
			graphics.fill(x0, y, x0 + 1, y + BOX_HEIGHT, border);
			graphics.fill(x0 + BOX_WIDTH - 1, y, x0 + BOX_WIDTH, y + BOX_HEIGHT, border);

			int ix = x0 + PADDING;
			int iy = y + PADDING;

			ItemStack stack = option.displayStack();
			if (entryAlpha >= ICON_ALPHA_CUTOFF) {
				graphics.setColor(1.0F, 1.0F, 1.0F, entryAlpha);
				graphics.renderItem(stack, ix, iy);
				graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			}

			if (textAlpha >= 24) {
				int textX = ix + 18;
				int textY = y + (BOX_HEIGHT - font.lineHeight) / 2;
				int count = option.totalCount();
				Component countLabel = count > 1 ? Component.literal("x" + count) : Component.empty();
				int countWidth = count > 1 ? font.width(countLabel) : 0;
				int reservedRightWidth = (isSelected ? (pickupKeyLabelWidth + 6) : 0) + (countWidth > 0 ? (countWidth + 6) : 0);

				int maxTextWidth = BOX_WIDTH - (textX - x0) - PADDING - reservedRightWidth;
				Component trimmed = trim(font, option.displayName(), maxTextWidth);
				int color = (textAlpha << 24) | 0xFFFFFF;
				graphics.drawString(font, trimmed, textX, textY, color, false);

				int drawRightX = x0 + BOX_WIDTH - PADDING;
				if (isSelected) {
					drawRightX -= pickupKeyLabelWidth;
					graphics.drawString(font, pickupKeyLabel, drawRightX, textY, color, false);
					drawRightX -= 6;
				}
				if (countWidth > 0) {
					drawRightX -= countWidth;
					graphics.drawString(font, countLabel, drawRightX, textY, color, false);
				}
			}
		}
	}

	private static Component trim(Font font, Component component, int maxWidth) {
		if (maxWidth <= 0) {
			return Component.empty();
		}

		if (font.width(component) <= maxWidth) {
			return component;
		}

		String s = component.getString();
		if (s.isEmpty()) {
			return component;
		}

		String dots = "...";
		int dotsWidth = font.width(dots);
		if (dotsWidth >= maxWidth) {
			return Component.literal(dots);
		}

		StringBuilder out = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			out.append(s.charAt(i));
			if (font.width(out.toString()) + dotsWidth > maxWidth) {
				out.deleteCharAt(out.length() - 1);
				break;
			}
		}

		return Component.literal(out + dots);
	}

	private static float clamp01(float v) {
		return Math.max(0.0F, Math.min(1.0F, v));
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static float easeOutExpo(float t) {
		if (t <= 0.0F) {
			return 0.0F;
		}
		if (t >= 1.0F) {
			return 1.0F;
		}
		return 1.0F - (float) Math.pow(2.0D, -10.0D * t);
	}

	private static float easeInExpo(float t) {
		if (t <= 0.0F) {
			return 0.0F;
		}
		if (t >= 1.0F) {
			return 1.0F;
		}
		return (float) Math.pow(2.0D, 10.0D * (t - 1.0D));
	}
}
