package com.boaringpanda.vsbetterqol.client;

import com.boaringpanda.vsbetterqol.config.Option;
import com.boaringpanda.vsbetterqol.config.Setting;
import com.boaringpanda.vsbetterqol.config.Slider;
import com.mojang.serialization.Codec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

// The settings screen Mod Menu opens: vanilla's own options screen (like Video Settings) with a widget for each ClientConfig setting,
// two per row, with a hover tooltip: an ON/OFF button for a switch, a percent slider for a slider. A change applies at once; the file
// is saved when the screen closes.
public class ClientConfigScreen extends OptionsSubScreen {
	public ClientConfigScreen(Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable("options.vsbetterqol.title"));
	}

	@Override
	protected void addOptions() {
		this.list.addSmall(ClientConfig.ALL.stream().map(ClientConfigScreen::widget).toArray(OptionInstance[]::new));
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
		};
	}

	// Saves our file instead of vanilla's options.txt.
	@Override
	public void removed() {
		ClientConfig.save();
	}
}
