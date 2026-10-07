package com.boaringpanda.vsbetterqol.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.fabricmc.loader.api.FabricLoader;

// A .properties file in the config folder, made to be read and edited in Notepad: a header, then each setting with its comment.
// Used by client/ClientConfig and ServerConfig.
public final class ConfigFile {
	private static final Logger LOGGER = LogUtils.getLogger();

	private final Path path;
	private final List<String> header;
	private final List<? extends Setting> settings;

	public ConfigFile(String fileName, List<String> header, List<? extends Setting> settings) {
		this.path = FabricLoader.getInstance().getConfigDir().resolve(fileName);
		this.header = header;
		this.settings = settings;
	}

	public void load() {
		if (Files.exists(this.path)) {
			Properties file = new Properties();
			try (Reader reader = Files.newBufferedReader(this.path, StandardCharsets.UTF_8)) {
				file.load(reader);
			} catch (IOException e) {
				// Not saved over, so the player's file isn't lost.
				LOGGER.warn("Couldn't read {}, keeping the current settings", this.path, e);
				return;
			}
			for (Setting setting : this.settings) {
				setting.read(file.getProperty(setting.key));
			}
		}
		// Adds any setting the file doesn't have yet (first launch, or one added in an update).
		this.save();
	}

	// Written by hand rather than with Properties.store, so the settings keep their order and comments.
	public void save() {
		StringBuilder text = new StringBuilder();
		for (String line : this.header) {
			text.append("# ").append(line).append('\n');
		}
		for (Setting setting : this.settings) {
			text.append("\n# ").append(setting.comment).append('\n').append(setting.key).append('=').append(setting.write()).append('\n');
		}
		try {
			Files.createDirectories(this.path.getParent());
			Files.writeString(this.path, text, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("Couldn't save {}", this.path, e);
		}
	}
}
