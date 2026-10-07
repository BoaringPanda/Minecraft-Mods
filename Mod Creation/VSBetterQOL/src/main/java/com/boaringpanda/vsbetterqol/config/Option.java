package com.boaringpanda.vsbetterqol.config;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

// An on/off switch, on by default. false/no/off turn it off, anything else turns it on.
public final class Option extends Setting {
	public boolean on = true;

	public Option(String key, String comment) {
		super(key, comment);
	}

	@Override
	String write() {
		return Boolean.toString(this.on);
	}

	@Override
	void read(@Nullable String text) {
		if (text != null) {
			this.on = switch (text.trim().toLowerCase(Locale.ROOT)) {
				case "false", "no", "off" -> false;
				default -> true;
			};
		}
	}
}
