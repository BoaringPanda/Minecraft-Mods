package com.boaringpanda.vslumberjackmod;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

public class VSLumberjackMod implements ModInitializer {
	public static final String MOD_ID = "vslumberjackmod";

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
