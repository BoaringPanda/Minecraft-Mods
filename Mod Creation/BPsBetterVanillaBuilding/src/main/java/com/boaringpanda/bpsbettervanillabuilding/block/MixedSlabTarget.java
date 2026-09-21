package com.boaringpanda.bpsbettervanillabuilding.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import com.boaringpanda.bpsbettervanillabuilding.block.custom.MixedSlabBlock;

/**
 * Works out which of the two slabs in a {@link MixedSlabBlock} a player is looking at: the upper half of the block is the
 * top slab and the lower half the bottom slab. The same idea as {@link LilyPadTarget}: the server is only told the block's
 * position, so this casts the player's own view ray against the block and reads the height of the hit. Picking
 * ({@link MixedSlabPicking}), the break sound and particles, and the mining tap all use it, so they agree.
 */
public final class MixedSlabTarget {
	private MixedSlabTarget() {
	}

	/** True for the upper half (the top slab), false for the lower half, null if the player's ray doesn't meet the block. */
	@Nullable
	public static Boolean upperHalf(Player player, BlockPos pos) {
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
		BlockHitResult hit = Shapes.block().clip(from, to, pos);
		return hit == null ? null : hit.getLocation().y - pos.getY() >= 0.5;
	}

	/**
	 * The slab being looked at. With no player, or a ray that misses, it is the top slab: the surface you see and stand on,
	 * and what fire and explosions get.
	 */
	public static Block aimedSlab(MixedSlabBlock combo, @Nullable Player player, BlockPos pos) {
		Boolean upper = player == null ? null : upperHalf(player, pos);
		return upper == null || upper ? combo.topSlab() : combo.bottomSlab();
	}
}
