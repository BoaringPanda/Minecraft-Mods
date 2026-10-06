package com.boaringpanda.vsbetterqol;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

public class VSBetterQOL implements ModInitializer {
	public static final String MOD_ID = "vsbetterqol";

	@Override
	public void onInitialize() {
		ToolSpeedRules.register();
		NameTagRenaming.register();
		PlacedLogs.register();
		EnchantingLapis.register();
		InventorySorting.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
