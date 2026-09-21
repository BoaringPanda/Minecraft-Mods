package com.boaringpanda.bpsbettervanillabuilding.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlockEntity;

/**
 * Works out which of the two heads in a {@link com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlock}
 * a player is aiming at. The same idea as {@link LilyPadTarget}: the server is never told where on a block the player
 * was aiming when they break it, so this casts the player's own view ray against each head's shape and takes the
 * nearer one. Breaking ({@link StackedHeadsBreaking}), picking and the wireframe that lights up all use it, so they
 * agree. An empty half has an empty shape, so it can never be the answer.
 */
public final class StackedHeadsTarget {
	public enum Part {
		TOP,
		BOTTOM,
		/** The ray hits neither (not looking at this block, or a rounding-level miss). */
		NONE
	}

	private StackedHeadsTarget() {
	}

	public static Part aimedPart(StackedHeadsBlockEntity heads, Player player, BlockPos pos) {
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));

		BlockHitResult bottomHit = heads.shape(false, false).clip(from, to, pos);
		BlockHitResult topHit = heads.shape(true, false).clip(from, to, pos);
		if (topHit == null) {
			return bottomHit == null ? Part.NONE : Part.BOTTOM;
		}

		boolean topNearer = bottomHit == null || topHit.getLocation().distanceToSqr(from) <= bottomHit.getLocation().distanceToSqr(from);
		return topNearer ? Part.TOP : Part.BOTTOM;
	}

	/**
	 * Which half a break or a pick acts on. With one head in the block it is that head, whatever the player aims at.
	 * With two, "neither" counts as the top head: the one on the outside, which is what a break did before there was a choice.
	 */
	public static boolean aimedAtTop(StackedHeadsBlockEntity heads, Player player, BlockPos pos) {
		if (!heads.has(true)) {
			return false;
		}
		if (!heads.has(false)) {
			return true;
		}

		return aimedPart(heads, player, pos) != Part.BOTTOM;
	}

	/**
	 * The shape the block reports as its outline. The game uses the same {@code getShape} call both to cast the crosshair
	 * ray and to draw the wireframe, and gives it the looking player as context, so returning only the aimed head makes
	 * just that head light up. Anything else gets both heads together.
	 */
	public static VoxelShape outline(StackedHeadsBlockEntity heads, BlockPos pos, CollisionContext context) {
		VoxelShape whole = Shapes.or(heads.shape(false, false), heads.shape(true, false));
		if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player) {
			return switch (aimedPart(heads, player, pos)) {
				case TOP -> heads.shape(true, false);
				case BOTTOM -> heads.shape(false, false);
				case NONE -> whole;
			};
		}

		return whole;
	}
}
