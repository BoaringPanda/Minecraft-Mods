package com.boaringpanda.bettervanillaqol;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

public class BetterVanillaQOL implements ModInitializer {
	public static final String MOD_ID = "bettervanillaqol";

	@Override
	public void onInitialize() {
		ToolSpeedRules.register();
		NameTagRenaming.register();
		PlacedLogs.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
