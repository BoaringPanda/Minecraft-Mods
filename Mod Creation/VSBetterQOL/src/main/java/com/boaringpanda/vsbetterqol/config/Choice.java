package com.boaringpanda.vsbetterqol.config;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

// One of a fixed list of values, written in the file by name. A name that isn't in the list keeps the current value.
public final class Choice<T> extends Setting {
	public final List<T> values;
	public final Function<T, String> name;
	public T value;

	public Choice(String key, String comment, List<T> values, Function<T, String> name, T defaultValue) {
		super(key, comment);
		this.values = values;
		this.name = name;
		this.value = defaultValue;
	}

	@Override
	String write() {
		return this.name.apply(this.value);
	}

	@Override
	void read(@Nullable String text) {
		if (text == null) {
			return;
		}
		String wanted = text.trim().toLowerCase(Locale.ROOT);
		for (T candidate : this.values) {
			if (this.name.apply(candidate).equals(wanted)) {
				this.value = candidate;
				return;
			}
		}
	}
}
