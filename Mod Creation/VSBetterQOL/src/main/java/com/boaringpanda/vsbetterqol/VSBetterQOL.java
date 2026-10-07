package com.boaringpanda.vsbetterqol;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

public class VSBetterQOL implements ModInitializer {
	public static final String MOD_ID = "vsbetterqol";

	@Override
	public void onInitialize() {
		ServerConfig.register();
		ToolSpeedRules.register();
		PlacedLogs.register();
		EnchantingLapis.register();
		PickingUp.register();
		Carrying.register();
		CarriedBrewing.register();
		StonecutterStorage.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
