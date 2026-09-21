package com.boaringpanda.bpsbettervanillabuilding.block.custom;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A plain vanilla stair. {@link StairBlock}'s constructor is {@code protected}, and vanilla only ever builds
 * stairs from inside {@code Blocks}, so this subclass exists just to make it callable from
 * {@link com.boaringpanda.bpsbettervanillabuilding.block.TerracottaBlocks}.
 */
public class TerracottaStairBlock extends StairBlock {
	public TerracottaStairBlock(BlockState baseState, Properties properties) {
		super(baseState, properties);
	}
}
