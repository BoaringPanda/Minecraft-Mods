package com.boaringpanda.vsbetterbuilding.block;

import java.util.Arrays;
import java.util.stream.Stream;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Placing onto a lily pad: the block goes in the pad's space as if set down on a floor there. The clicked face is up and "down" is
 * the first direction the player looks, so each block's own {@code getStateForPlacement} picks its standing form: a standing torch, sign,
 * banner or head ({@code StandingAndWallBlockItem} tries the directions in order), a floor button, a standing lantern, an upright rod,
 * chain or amethyst. The player's own facing is kept, so signs, banners, heads, buttons and pots still face the way they'd face on land.
 */
public class LilyPadPlaceContext extends BlockPlaceContext {
	public LilyPadPlaceContext(BlockPlaceContext context) {
		super(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(),
				new BlockHitResult(context.getClickLocation(), Direction.UP, context.getClickedPos(), false));
	}

	@Override
	public Direction[] getNearestLookingDirections() {
		return Stream.concat(Stream.of(Direction.DOWN),
				Arrays.stream(super.getNearestLookingDirections()).filter(direction -> direction != Direction.DOWN)).toArray(Direction[]::new);
	}
}
