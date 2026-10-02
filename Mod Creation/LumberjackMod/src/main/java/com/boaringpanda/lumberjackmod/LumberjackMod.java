package com.boaringpanda.lumberjackmod;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

public class LumberjackMod implements ModInitializer {
	public static final String MOD_ID = "lumberjackmod";

	@Override
	public void onInitialize() {
		PlacedTreeParts.register();
		TreeFalling.register();
		FallingCanopy.register();
		Woodcutter.register();
		WoodcutterVillager.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
