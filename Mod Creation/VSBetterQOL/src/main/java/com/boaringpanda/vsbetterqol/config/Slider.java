package com.boaringpanda.vsbetterqol.config;

import org.jspecify.annotations.Nullable;

// A percentage from min to max in steps of step. A number outside that is clamped and rounded to the nearest step.
public final class Slider extends Setting {
	public final int min;
	public final int max;
	public final int step;
	public int value;

	public Slider(String key, String comment, int min, int max, int step, int defaultValue) {
		super(key, comment);
		this.min = min;
		this.max = max;
		this.step = step;
		this.value = defaultValue;
	}

	@Override
	String write() {
		return Integer.toString(this.value);
	}

	@Override
	void read(@Nullable String text) {
		if (text == null) {
			return;
		}
		try {
			int clamped = Math.clamp(Integer.parseInt(text.trim()), this.min, this.max);
			this.value = this.min + Math.round((clamped - this.min) / (float) this.step) * this.step;
		} catch (NumberFormatException e) {
			// Keeps the current value.
		}
	}
}
