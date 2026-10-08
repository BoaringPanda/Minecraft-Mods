package com.boaringpanda.vsbetterqol.client;

import java.util.List;
import java.util.Locale;

import com.boaringpanda.vsbetterqol.config.Choice;
import com.boaringpanda.vsbetterqol.config.ConfigFile;
import com.boaringpanda.vsbetterqol.config.Option;
import com.boaringpanda.vsbetterqol.config.Setting;
import com.boaringpanda.vsbetterqol.config.Slider;

import net.minecraft.network.chat.TextColor;

// The player's own settings for the client-side features, saved in config/vsbetterqol-client.properties. Changed in-game through
// Mod Menu (ClientConfigScreen) or by hand in the file. Features check their setting every time, so a change applies at once.
public final class ClientConfig {
	public static final Option SATURATION_BAR = new Option("saturation_bar",
			"Shows your saturation as a coloured outline on the hunger bar, and flashes what eating the food in your hand would add. Using AppleSkin? Turn this off so it doesn't show twice.");
	// The 16 named text colours (the ones chat and signs use). Green is the outline's original colour.
	public static final Choice<TextColor> SATURATION_COLOR = new Choice<>("saturation_color",
			"The colour of the saturation outline, on the hunger bar and in food tooltips: black, dark_blue, dark_green, dark_aqua, dark_red, dark_purple, gold, gray, dark_gray, blue, green, aqua, red, light_purple, yellow or white.",
			List.of(TextColor.BLACK, TextColor.DARK_BLUE, TextColor.DARK_GREEN, TextColor.DARK_AQUA, TextColor.DARK_RED, TextColor.DARK_PURPLE,
					TextColor.GOLD, TextColor.GRAY, TextColor.DARK_GRAY, TextColor.BLUE, TextColor.GREEN, TextColor.AQUA, TextColor.RED,
					TextColor.LIGHT_PURPLE, TextColor.YELLOW, TextColor.WHITE),
			TextColor::serialize, TextColor.GREEN);
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
	public static final Option LOWER_SHIELD = new Option("lower_shield",
			"Holds your shield lower in first person (blocking too), so it covers less of the screen.");
	public static final Option LOWER_FIRE = new Option("lower_fire",
			"Shows the flames lower on your screen while you're on fire, so you can still see.");
	public static final Option ELYTRA_SPEED = new Option("elytra_speed",
			"Shows how fast you're flying above the hotbar while gliding with an elytra. Green is safe, red means a crash into a wall could kill you.");
	public static final Slider ELYTRA_SPEED_SIZE = new Slider("elytra_speed_size",
			"How big the elytra speed bar is, in percent: 50 to 200, in steps of 10. 100 is the normal size.", 50, 200, 10, 100);
	public static final Choice<SpeedBarPosition> ELYTRA_SPEED_POSITION = new Choice<>("elytra_speed_position",
			"Where the elytra speed bar is: above_hotbar (above the health and hunger bars), top_left, top_middle, top_right, bottom_left or bottom_right.",
			List.of(SpeedBarPosition.values()), position -> position.name().toLowerCase(Locale.ROOT), SpeedBarPosition.ABOVE_HOTBAR);

	// File and settings screen order (two per row on the screen, so the saturation bar and its colour share a row, and the effect
	// column and the elytra speed bar each with their size).
	public static final List<Setting> ALL = List.of(SATURATION_BAR, SATURATION_COLOR, FOOD_TOOLTIP, ARMOR_BAR_COLORS, EFFECT_COLUMN,
			EFFECT_COLUMN_SIZE, DURABILITY_TOOLTIP, ENCHANTED_BOOK_INFO, LOWER_SHIELD, LOWER_FIRE, ELYTRA_SPEED, ELYTRA_SPEED_SIZE,
			ELYTRA_SPEED_POSITION, SHIFT_DRAG);

	// Where the elytra speed bar goes (client/mixin/ElytraSpeedHudMixin). Above the hotbar is the original spot.
	public enum SpeedBarPosition {
		ABOVE_HOTBAR, TOP_LEFT, TOP_MIDDLE, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
	}

	private static final ConfigFile FILE = new ConfigFile("vsbetterqol-client.properties", List.of(
			"VS Better QOL: your own settings (they only change your game).",
			"true = on, false = off (yes/no works too); a colour by its name. Changes here apply the next time the game starts.",
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
