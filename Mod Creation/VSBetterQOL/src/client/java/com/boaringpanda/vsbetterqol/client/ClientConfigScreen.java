package com.boaringpanda.vsbetterqol.client;

import com.boaringpanda.vsbetterqol.config.Choice;
import com.boaringpanda.vsbetterqol.config.Option;
import com.boaringpanda.vsbetterqol.config.Setting;
import com.boaringpanda.vsbetterqol.config.Slider;
import com.mojang.serialization.Codec;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

// The settings screen Mod Menu opens: vanilla's own options screen (like Video Settings) with a widget for each ClientConfig setting,
// two per row, with a hover tooltip: an ON/OFF button for a switch, a percent slider for a slider, a cycling button for a choice, and for
// the saturation colour a ColorDropdown whose colour panel is drawn on top of everything here. A change applies at once; the file is saved when the screen closes.
public class ClientConfigScreen extends OptionsSubScreen {
	private @Nullable ColorDropdown open;

	public ClientConfigScreen(Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable("options.vsbetterqol.title"));
	}

	// The first row is the saturation bar and its colour dropdown (vanilla's overload that keeps the bar's OptionInstance, so
	// applyUnsavedChanges still finds it), then the rest two per row.
	@Override
	protected void addOptions() {
		OptionInstance<?> saturationBar = widget(ClientConfig.SATURATION_BAR);
		this.list.addSmall(saturationBar.createButton(this.options), saturationBar, this.dropdown(ClientConfig.SATURATION_COLOR));
		this.list.addSmall(ClientConfig.ALL.stream().filter(setting -> setting != ClientConfig.SATURATION_BAR && setting != ClientConfig.SATURATION_COLOR)
				.map(ClientConfigScreen::widget).toArray(OptionInstance[]::new));
	}

	private static OptionInstance<?> widget(Setting setting) {
		String caption = "option.vsbetterqol." + setting.key;
		Component tooltip = Component.translatable(caption + ".tooltip");
		return switch (setting) {
			case Option option -> OptionInstance.createBoolean(caption, OptionInstance.cachedConstantTooltip(tooltip), option.on,
					value -> option.on = value);
			// Like vanilla's Max Framerate slider: slider positions are steps, mapped to the percentage.
			case Slider slider -> new OptionInstance<Integer>(caption, OptionInstance.cachedConstantTooltip(tooltip),
					(label, value) -> Component.translatable("options.percent_value", label, value),
					new OptionInstance.IntRange(slider.min / slider.step, slider.max / slider.step)
							.<Integer>xmap(position -> position * slider.step, value -> value / slider.step, true),
					Codec.intRange(slider.min, slider.max), slider.value, value -> slider.value = value);
			case Choice<?> choice -> choiceWidget(caption, tooltip, choice);
		};
	}

	// A button that cycles through the values, each named by its lang key option.vsbetterqol.<key>.<name>. Vanilla's cycle button puts
	// the setting's name in front itself, so this gives just the value.
	private static <T> OptionInstance<T> choiceWidget(String caption, Component tooltip, Choice<T> choice) {
		return new OptionInstance<>(caption, OptionInstance.cachedConstantTooltip(tooltip),
				(label, value) -> Component.translatable(caption + "." + choice.name.apply(value)),
				new OptionInstance.Enum<>(choice.values, Codec.stringResolver(choice.name, name -> choice.values.stream()
						.filter(value -> choice.name.apply(value).equals(name)).findFirst().orElse(null))),
				choice.value, value -> choice.value = value);
	}

	private ColorDropdown dropdown(Choice<TextColor> choice) {
		String caption = "option.vsbetterqol." + choice.key;
		return new ColorDropdown(choice, Component.translatable(caption), Tooltip.create(Component.translatable(caption + ".tooltip")),
				dropdown -> this.open = this.open == dropdown ? null : dropdown);
	}

	// While the panel is open, the widgets under it don't see the mouse (no hover or tooltip through it).
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		boolean overPanel = this.open != null && this.open.panelContains(mouseX, mouseY, this.height);
		super.extractRenderState(graphics, overPanel ? -1 : mouseX, overPanel ? -1 : mouseY, a);
		if (this.open != null) {
			graphics.nextStratum();
			this.open.extractPanel(graphics, mouseX, mouseY, this.height);
		}
	}

	// While the panel is open every click closes it: on a colour it picks that colour first. Nothing behind it is pressed.
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.open == null) {
			return super.mouseClicked(event, doubleClick);
		}
		TextColor color = this.open.colorAt(event.x(), event.y(), this.height);
		if (color != null) {
			this.open.choose(color);
			AbstractWidget.playButtonClickSound(this.minecraft.getSoundManager());
		}
		this.open = null;
		return true;
	}

	// Esc closes the panel first, then the screen.
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.open != null && event.isEscape()) {
			this.open = null;
			return true;
		}
		return super.keyPressed(event);
	}

	// The list moves, so the panel closes.
	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		this.open = null;
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	// Saves our file instead of vanilla's options.txt.
	@Override
	public void removed() {
		ClientConfig.save();
	}
}
