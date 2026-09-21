package com.boaringpanda.bpsbettervanillabuilding;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessories;
import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessoryBreaking;
import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessoryInteraction;
import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessoryPicking;
import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadSignBlockEntities;
import com.boaringpanda.bpsbettervanillabuilding.block.MixedSlabBlocks;
import com.boaringpanda.bpsbettervanillabuilding.block.MixedSlabPicking;
import com.boaringpanda.bpsbettervanillabuilding.block.StackedFlowers;
import com.boaringpanda.bpsbettervanillabuilding.block.StackedHeads;
import com.boaringpanda.bpsbettervanillabuilding.block.StackedHeadsBreaking;
import com.boaringpanda.bpsbettervanillabuilding.block.StackedHeadsInteraction;
import com.boaringpanda.bpsbettervanillabuilding.block.TerracottaBlocks;

public class BPsBetterVanillaBuilding implements ModInitializer {
	public static final String MOD_ID = "bpsbettervanillabuilding";

	@Override
	public void onInitialize() {
		// Must come before MixedSlabBlocks: the terracotta slabs are mixed-slab materials.
		TerracottaBlocks.initialize();
		MixedSlabBlocks.initialize();
		MixedSlabPicking.initialize();
		StackedFlowers.initialize();
		LilyPadAccessories.initialize();
		// Must come after LilyPadAccessories: reads its fully-populated SIGN_BLOCKS list.
		LilyPadSignBlockEntities.initialize();
		LilyPadAccessoryInteraction.initialize();
		LilyPadAccessoryBreaking.initialize();
		LilyPadAccessoryPicking.initialize();
		StackedHeads.initialize();
		StackedHeadsInteraction.initialize();
		StackedHeadsBreaking.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
