package com.moigferdsrte.optional_pickup.client;

import java.util.List;
import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

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

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null && minecraft.level != null) {
			DropSelectionState state = DropSelectionState.get();
			if (state.shouldRender()) {
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
				int totalHeight = visible * BOX_HEIGHT + (visible - 1) * 3;
				int prevTotalHeight = prevVisible * BOX_HEIGHT + (prevVisible - 1) * 3;
				int rightX = width - MARGIN - BOX_WIDTH;
				int leftX = MARGIN;
				int xPercent = OptionalPickupClient.config != null ? OptionalPickupClient.config.offsetX : 0;
				float t = Math.clamp((float) xPercent / 100.0F, 0.0F, 1.0F);
				int x0 = Math.round((float)rightX + (float)(leftX - rightX) * t);
				int minX = Math.min(leftX, rightX);
				int maxX = Math.max(leftX, rightX);
				x0 = Math.clamp(x0, minX, maxX);
				int offsetY = OptionalPickupClient.config != null ? OptionalPickupClient.config.offsetY : 0;
				int yShift = Math.round((1.0F - eased) * 12.0F);
				int y0 = height - MARGIN - totalHeight - 20 - offsetY + yShift;
				int prevY0 = height - MARGIN - prevTotalHeight - 20 - offsetY + yShift;
				Font font = minecraft.font;
				Component pickupKey = OptionalPickupClient.getPickupKeyDisplay();
				Component pickupKeyLabel = Component.literal("[" + pickupKey.getString() + "]");
				int pickupKeyLabelWidth = font.width(pickupKeyLabel);

				for(int i = 0; i < visible; ++i) {
					int idx = start + i;
					DropSelectionState.DropOption option = (DropSelectionState.DropOption)options.get(idx);
					int currentY = y0 + i * 25;
					int prevIndex = state.getPrevIndexFor(option.keyStack());
					int prevY;
					if (prevIndex == -1) {
						prevY = currentY + 25;
					} else {
						int prevRow = prevIndex - prevStart;
						if (prevRow >= 0 && prevRow < prevVisible) {
							prevY = prevY0 + prevRow * 25;
						} else {
							prevY = currentY + (prevRow < 0 ? -25 : 25);
						}
					}

					int y = Math.round(lerp((float)prevY, (float)currentY, layoutT));
					boolean isSelected = idx == selected;
					float entryAlpha = prevIndex == -1 ? eased * layoutT : eased;
					int bgAlpha = Math.round(170.0F * entryAlpha);
					float textFactor = entryAlpha * entryAlpha;
					int textAlpha = Math.round(255.0F * textFactor);
					int borderAlpha = isSelected ? textAlpha : Math.round(85.0F * entryAlpha);
					int bg = bgAlpha << 24;
					int border = borderAlpha << 24 | 16777215;
					graphics.fill(x0, y, x0 + 140, y + 22, bg);
					graphics.fill(x0, y, x0 + 140, y + 1, border);
					graphics.fill(x0, y + 22 - 1, x0 + 140, y + 22, border);
					graphics.fill(x0, y, x0 + 1, y + 22, border);
					graphics.fill(x0 + 140 - 1, y, x0 + 140, y + 22, border);
					int ix = x0 + 3;
					int iy = y + 3;
					ItemStack stack = option.displayStack();
					if (entryAlpha >= 0.18F) {
						graphics.item(stack, ix, iy, ARGB.white(entryAlpha));
					}

					if (textAlpha >= 24) {
						int textX = ix + 18;
						Objects.requireNonNull(font);
						int textY = y + (22 - 9) / 2;
						int count = option.totalCount();
						Component countLabel = count > 1 ? Component.literal("x" + count) : Component.empty();
						int countWidth = count > 1 ? font.width(countLabel) : 0;
						int reservedRightWidth = (isSelected ? pickupKeyLabelWidth + 6 : 0) + (countWidth > 0 ? countWidth + 6 : 0);
						int maxTextWidth = 140 - (textX - x0) - 3 - reservedRightWidth;
						Component trimmed = trim(font, option.displayName(), maxTextWidth);
						int color = textAlpha << 24 | 16777215;
						graphics.text(font, trimmed, textX, textY, color, false);
						int drawRightX = x0 + 140 - 3;
						if (isSelected) {
							drawRightX -= pickupKeyLabelWidth;
							graphics.text(font, pickupKeyLabel, drawRightX, textY, color, false);
							drawRightX -= 6;
						}

						if (countWidth > 0) {
							drawRightX -= countWidth;
							graphics.text(font, countLabel, drawRightX, textY, color, false);
						}
					}
				}

			}
		}
	}

	private static Component trim(Font font, Component component, int maxWidth) {
		if (maxWidth <= 0) {
			return Component.empty();
		} else if (font.width(component) <= maxWidth) {
			return component;
		} else {
			String s = component.getString();
			if (s.isEmpty()) {
				return component;
			} else {
				String dots = "...";
				int dotsWidth = font.width(dots);
				if (dotsWidth >= maxWidth) {
					return Component.literal(dots);
				} else {
					StringBuilder out = new StringBuilder();

					for(int i = 0; i < s.length(); ++i) {
						out.append(s.charAt(i));
						if (font.width(out.toString()) + dotsWidth > maxWidth) {
							out.deleteCharAt(out.length() - 1);
							break;
						}
					}

					String var10000 = String.valueOf(out);
					return Component.literal(var10000 + dots);
				}
			}
		}
	}

	private static float clamp01(float v) {
		return Math.clamp(v, 0.0F, 1.0F);
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static float easeOutExpo(float t) {
		if (t <= 0.0F) {
			return 0.0F;
		} else {
			return t >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0F, (double)-10.0F * (double)t);
		}
	}

	private static float easeInExpo(float t) {
		if (t <= 0.0F) {
			return 0.0F;
		} else {
			return t >= 1.0F ? 1.0F : (float)Math.pow(2.0F, (double)10.0F * ((double)t - (double)1.0F));
		}
	}
}
