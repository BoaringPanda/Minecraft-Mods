package com.boaringpanda.bpsbettervanillabuilding.block.custom;

import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The three shapes one lily pad combo needs (see {@link LilyPadShape} for the pad itself):
 * <ul>
 *   <li>{@code accessory} - the item alone, so aiming code can tell "the item" from "the pad";</li>
 *   <li>{@code outline} - pad + item, what the crosshair can hit and the wireframe draws;</li>
 *   <li>{@code collision} - pad + whatever the real accessory is solid with (nothing extra for
 *       torches and signs, which have no collision in vanilla).</li>
 * </ul>
 */
record LilyPadShapes(VoxelShape accessory, VoxelShape outline, VoxelShape collision) {
	static LilyPadShapes of(VoxelShape accessoryOutline, VoxelShape accessoryCollision) {
		return new LilyPadShapes(accessoryOutline, LilyPadShape.withPad(accessoryOutline), LilyPadShape.withPad(accessoryCollision));
	}

	/** For accessories whose collision is the same as their outline. */
	static LilyPadShapes solid(VoxelShape accessoryShape) {
		return of(accessoryShape, accessoryShape);
	}
}
