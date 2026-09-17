package com.boaringpanda.extrablocks;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boaringpanda.extrablocks.block.LilyPadAccessories;
import com.boaringpanda.extrablocks.block.LilyPadAccessoryInteraction;
import com.boaringpanda.extrablocks.block.MixedSlabBlocks;
import com.boaringpanda.extrablocks.block.MixedSlabInteraction;

public class ExtraBlocks implements ModInitializer {
	public static final String MOD_ID = "extra_blocks";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		MixedSlabBlocks.initialize();
		MixedSlabInteraction.initialize();
		LilyPadAccessories.initialize();
		LilyPadAccessoryInteraction.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
