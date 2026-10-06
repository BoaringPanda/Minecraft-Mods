package com.boaringpanda.vsbetterqol.client;

import com.boaringpanda.vsbetterqol.VSBetterQOL;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

// Two rows of shanks under a food's name: what it fills on the hunger bar, then its saturation as green-outlined empty shanks
// (rounded to the nearest half shank), drawn with the HUD's own sprites.
public record ClientFoodTooltip(int hunger, int saturation) implements ClientTooltipComponent {
	private static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
	private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
	private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	private static final Identifier SATURATION_HALF = VSBetterQOL.id("hud/saturation_half");
	private static final Identifier SATURATION_FULL = VSBetterQOL.id("hud/saturation_full");
	private static final int ROW_HEIGHT = 10;

	public ClientFoodTooltip(FoodTooltip food) {
		this(food.nutrition(), Math.round(food.saturation()));
	}

	// No saturation row (and no space for it) when the saturation rounds to 0.
	@Override
	public int getHeight(Font font) {
		return ROW_HEIGHT * (this.saturation > 0 ? 2 : 1) + 2;
	}

	@Override
	public int getWidth(Font font) {
		return Math.max(icons(this.hunger), icons(this.saturation)) * 8 + 1;
	}

	@Override
	public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
		for (int i = 0; i < icons(this.hunger); i++) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, x + i * 8, y, 9, 9);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, i * 2 + 2 <= this.hunger ? FOOD_FULL : FOOD_HALF, x + i * 8, y, 9, 9);
		}
		for (int i = 0; i < icons(this.saturation); i++) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, x + i * 8, y + ROW_HEIGHT, 9, 9);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, i * 2 + 2 <= this.saturation ? SATURATION_FULL : SATURATION_HALF,
					x + i * 8, y + ROW_HEIGHT, 9, 9);
		}
	}

	// Shanks needed for this many half shanks.
	private static int icons(int halves) {
		return (halves + 1) / 2;
	}
}
