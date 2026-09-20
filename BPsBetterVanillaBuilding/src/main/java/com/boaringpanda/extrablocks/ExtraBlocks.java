package com.boaringpanda.extrablocks;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import com.boaringpanda.extrablocks.block.LilyPadAccessories;
import com.boaringpanda.extrablocks.block.LilyPadAccessoryBreaking;
import com.boaringpanda.extrablocks.block.LilyPadAccessoryInteraction;
import com.boaringpanda.extrablocks.block.LilyPadAccessoryPicking;
import com.boaringpanda.extrablocks.block.LilyPadSignBlockEntities;
import com.boaringpanda.extrablocks.block.MixedSlabBlocks;
import com.boaringpanda.extrablocks.block.MixedSlabInteraction;

public class ExtraBlocks implements ModInitializer {
	public static final String MOD_ID = "extra_blocks";

	@Override
	public void onInitialize() {
		MixedSlabBlocks.initialize();
		MixedSlabInteraction.initialize();
		LilyPadAccessories.initialize();
		// Must come after LilyPadAccessories: reads its fully-populated SIGN_BLOCKS list.
		LilyPadSignBlockEntities.initialize();
		LilyPadAccessoryInteraction.initialize();
		LilyPadAccessoryBreaking.initialize();
		LilyPadAccessoryPicking.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
