package com.boaringpanda.vsbetterbuilding;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import com.boaringpanda.vsbetterbuilding.block.ExtraStairsAndSlabs;
import com.boaringpanda.vsbetterbuilding.block.FenceRopeInteraction;
import com.boaringpanda.vsbetterbuilding.block.MixedSlabs;
import com.boaringpanda.vsbetterbuilding.block.PlacedRods;
import com.boaringpanda.vsbetterbuilding.entity.RainbowCushions;
import com.boaringpanda.vsbetterbuilding.entity.RopeKnots;
import com.boaringpanda.vsbetterbuilding.item.BuilderStick;

public class VSBetterBuilding implements ModInitializer {
	public static final String MOD_ID = "vsbetterbuilding";

	@Override
	public void onInitialize() {
		RopeKnots.initialize();
		FenceRopeInteraction.initialize();
		PlacedRods.initialize();
		MixedSlabs.initialize();
		ExtraStairsAndSlabs.initialize();
		RainbowCushions.initialize();
		BuilderStick.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
