package com.boaringpanda.extrablocks.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The real vanilla lily pad's own collision shape - a slightly inset, thin
 * platform (verified from the compiled {@code LilyPadBlock} class, not
 * guessed). Shared by every {@code LilyPad*Block} so they all stand on
 * exactly like a normal lily pad.
 */
final class LilyPadShape {
	static final VoxelShape SHAPE = Block.column(14.0, 0.0, 1.5);

	private LilyPadShape() {
	}
}
