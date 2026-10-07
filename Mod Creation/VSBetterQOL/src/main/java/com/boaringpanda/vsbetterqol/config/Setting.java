package com.boaringpanda.vsbetterqol.config;

import org.jspecify.annotations.Nullable;

// One setting in a config file (ConfigFile): its key, the comment written above it, and its value.
public abstract sealed class Setting permits Option, Slider {
	public final String key;
	final String comment;

	Setting(String key, String comment) {
		this.key = key;
		this.comment = comment;
	}

	abstract String write();

	// Missing (null) or unreadable text keeps the current value.
	abstract void read(@Nullable String text);
}
