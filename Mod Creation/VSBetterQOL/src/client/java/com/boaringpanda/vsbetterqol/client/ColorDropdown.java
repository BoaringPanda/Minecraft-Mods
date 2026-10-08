package com.boaringpanda.vsbetterqol.client;

import java.util.function.Consumer;

import com.boaringpanda.vsbetterqol.config.Choice;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

// A colour setting on the settings screen: its name, a box of the current colour and a ▼. Clicking it opens a panel of the colours
// (2 rows of 8) under it, or above it when there's no room; ClientConfigScreen draws the panel and takes its clicks.
public class ColorDropdown extends AbstractButton {
	private static final int BOX = 16;
	private static final int GAP = 2;
	private static final int PADDING = 3;
	private static final int COLUMNS = 8;
	private static final int SWATCH = 10;
	private static final int PANEL_WIDTH = PADDING * 2 + COLUMNS * (BOX + GAP) - GAP;
	private static final int PANEL_HEIGHT = PADDING * 2 + 2 * (BOX + GAP) - GAP;

	private final Choice<TextColor> choice;
	private final Consumer<ColorDropdown> onPress;

	public ColorDropdown(Choice<TextColor> choice, Component message, Tooltip tooltip, Consumer<ColorDropdown> onPress) {
		super(0, 0, 150, 20, message);
		this.choice = choice;
		this.onPress = onPress;
		this.setTooltip(tooltip);
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.onPress.accept(this);
	}

	// Vanilla's button with the name on the left, then the colour box and the arrow on the right.
	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.extractDefaultSprite(graphics);
		graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE)
				.acceptScrollingWithDefaultCenter(this.getMessage(), this.getX() + 2, this.getRight() - 30, this.getY(), this.getBottom());
		int x = this.getRight() - 28;
		int y = this.getY() + (this.getHeight() - SWATCH) / 2;
		graphics.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, 0xFF000000);
		graphics.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | this.choice.value.getValue());
		graphics.text(Minecraft.getInstance().font, "▼", this.getRight() - 13, this.getY() + 6, 0xFFFFFFFF);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}

	private int panelX() {
		return this.getX() + (this.getWidth() - PANEL_WIDTH) / 2;
	}

	private int panelY(int screenHeight) {
		return this.getBottom() + PANEL_HEIGHT + 1 <= screenHeight ? this.getBottom() + 1 : this.getY() - PANEL_HEIGHT - 1;
	}

	public boolean panelContains(double mouseX, double mouseY, int screenHeight) {
		int x = this.panelX();
		int y = this.panelY(screenHeight);
		return mouseX >= x && mouseX < x + PANEL_WIDTH && mouseY >= y && mouseY < y + PANEL_HEIGHT;
	}

	public @Nullable TextColor colorAt(double mouseX, double mouseY, int screenHeight) {
		for (int i = 0; i < this.choice.values.size(); i++) {
			int x = this.boxX(i);
			int y = this.boxY(i, screenHeight);
			if (mouseX >= x && mouseX < x + BOX && mouseY >= y && mouseY < y + BOX) {
				return this.choice.values.get(i);
			}
		}
		return null;
	}

	public void choose(TextColor color) {
		this.choice.value = color;
	}

	// The panel: a dark box with a grey border, one square per colour. The current colour has a white border, the hovered one a grey
	// border and its name as a tooltip.
	public void extractPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int screenHeight) {
		int x = this.panelX();
		int y = this.panelY(screenHeight);
		graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xF0100010);
		graphics.outline(x, y, PANEL_WIDTH, PANEL_HEIGHT, 0xFF808080);
		TextColor hovered = this.colorAt(mouseX, mouseY, screenHeight);
		for (int i = 0; i < this.choice.values.size(); i++) {
			TextColor color = this.choice.values.get(i);
			int boxX = this.boxX(i);
			int boxY = this.boxY(i, screenHeight);
			graphics.fill(boxX, boxY, boxX + BOX, boxY + BOX, 0xFF000000 | color.getValue());
			if (color == this.choice.value) {
				graphics.outline(boxX - 1, boxY - 1, BOX + 2, BOX + 2, 0xFFFFFFFF);
			} else if (color == hovered) {
				graphics.outline(boxX - 1, boxY - 1, BOX + 2, BOX + 2, 0xFFA0A0A0);
			}
		}
		if (hovered != null) {
			graphics.setTooltipForNextFrame(Component.translatable("option.vsbetterqol." + this.choice.key + "." + this.choice.name.apply(hovered)),
					mouseX, mouseY);
		}
	}

	private int boxX(int i) {
		return this.panelX() + PADDING + i % COLUMNS * (BOX + GAP);
	}

	private int boxY(int i, int screenHeight) {
		return this.panelY(screenHeight) + PADDING + i / COLUMNS * (BOX + GAP);
	}
}
