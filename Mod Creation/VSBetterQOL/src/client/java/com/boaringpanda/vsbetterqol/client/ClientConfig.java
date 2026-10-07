package com.boaringpanda.vsbetterqol.client;

import java.util.List;

import com.boaringpanda.vsbetterqol.config.ConfigFile;
import com.boaringpanda.vsbetterqol.config.Option;
import com.boaringpanda.vsbetterqol.config.Setting;
import com.boaringpanda.vsbetterqol.config.Slider;

// The player's own settings for the client-side features, saved in config/vsbetterqol-client.properties. Changed in-game through
// Mod Menu (ClientConfigScreen) or by hand in the file. Features check their setting every time, so a change applies at once.
public final class ClientConfig {
	public static final Option SATURATION_BAR = new Option("saturation_bar",
			"Shows your saturation as a green outline on the hunger bar, and flashes what eating the food in your hand would add. Using AppleSkin? Turn this off so it doesn't show twice.");
	public static final Option FOOD_TOOLTIP = new Option("food_tooltip",
			"Shows how much hunger and saturation a food gives when you hover over it. Using AppleSkin? Turn this off so it doesn't show twice.");
	public static final Option EFFECT_COLUMN = new Option("effect_column",
			"Shows status effects in a column in the top right with the time left. Off: vanilla's effect icons, and the effect list beside your inventory comes back.");
	public static final Slider EFFECT_COLUMN_SIZE = new Slider("effect_column_size",
			"How big the effect column's icons and times are, in percent: 50 to 200, in steps of 10. 100 is the normal size.", 50, 200, 10, 100);
	public static final Option ARMOR_BAR_COLORS = new Option("armor_bar_colors",
			"Colours the armor bar by what each worn armor piece is made of.");
	public static final Option DURABILITY_TOOLTIP = new Option("durability_tooltip",
			"Shows the durability of damaged items in their tooltip, without needing F3+H.");
	public static final Option ENCHANTED_BOOK_INFO = new Option("enchanted_book_info",
			"Hold Shift on an enchanted book to read what each enchantment does.");
	public static final Option SHIFT_DRAG = new Option("shift_drag",
			"Hold Shift and drag the left mouse across slots to quick-move each one. Using Mouse Tweaks or another inventory mod that does this? Turn this off so they don't clash.");

	// File and settings screen order (two per row on the screen, so the effect column and its size share a row).
	public static final List<Setting> ALL = List.of(SATURATION_BAR, FOOD_TOOLTIP, EFFECT_COLUMN, EFFECT_COLUMN_SIZE, ARMOR_BAR_COLORS,
			DURABILITY_TOOLTIP, ENCHANTED_BOOK_INFO, SHIFT_DRAG);

	private static final ConfigFile FILE = new ConfigFile("vsbetterqol-client.properties", List.of(
			"VS Better QOL: your own settings (they only change your game).",
			"true = on, false = off (yes/no works too). Changes here apply the next time the game starts.",
			"With Mod Menu installed you can change them in-game instead: Mods > VS Better QOL > settings button."), ALL);

	private ClientConfig() {
	}

	// Called once at startup.
	public static void load() {
		FILE.load();
	}

	public static void save() {
		FILE.save();
	}
}
