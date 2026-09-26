package com.boaringpanda.bettervanillabuilding;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import com.boaringpanda.bettervanillabuilding.block.ExtraStairsAndSlabs;
import com.boaringpanda.bettervanillabuilding.block.FenceRopeInteraction;
import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;
import com.boaringpanda.bettervanillabuilding.block.PlacedRods;
import com.boaringpanda.bettervanillabuilding.entity.RainbowCushions;
import com.boaringpanda.bettervanillabuilding.entity.RopeKnots;
import com.boaringpanda.bettervanillabuilding.item.BuilderStick;

public class BetterVanillaBuilding implements ModInitializer {
	public static final String MOD_ID = "bettervanillabuilding";

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
