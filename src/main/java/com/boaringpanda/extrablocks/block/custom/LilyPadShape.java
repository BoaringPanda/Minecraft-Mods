package com.boaringpanda.extrablocks.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The real vanilla lily pad's own collision shape - a slightly inset, thin
 * platform (verified from the compiled {@code LilyPadBlock} class, not
 * guessed). Shared by every {@code LilyPad*Block} so they all stand on
 * exactly like a normal lily pad.
 */
public final class LilyPadShape {
	public static final VoxelShape SHAPE = Block.column(14.0, 0.0, 1.5);

	private LilyPadShape() {
	}

	/**
	 * The pad plus whatever is standing on it. Pass the accessory's own collision
	 * shape: it is empty for blocks with no collision in vanilla (torches, signs),
	 * so those combos end up with just the pad.
	 */
	static VoxelShape withPad(VoxelShape accessoryShape) {
		return Shapes.or(SHAPE, accessoryShape);
	}
}
