package com.boaringpanda.vsbetterbuilding.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A vanilla block that had no {@code axis}, given one so the builder stick can lay it on its side: ancient debris and reinforced
 * deepslate (swapped in by {@code BlocksMixin}). Placed by hand it stands upright, as it always has, instead of following the clicked
 * face like a log; worldgen and old saves get the upright default too.
 */
public class UprightPillarBlock extends RotatedPillarBlock {
	public UprightPillarBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState();
	}
}
