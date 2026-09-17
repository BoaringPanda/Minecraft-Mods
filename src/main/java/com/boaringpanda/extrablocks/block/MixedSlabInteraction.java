package com.boaringpanda.extrablocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import com.boaringpanda.extrablocks.block.custom.MixedSlabBlock;

/**
 * Lets a player combine two different slabs into one {@link MixedSlabBlock}
 * by right-clicking a placed single slab (bottom or top half) with a
 * different slab item, aimed at that slab's exposed flat face - i.e. the
 * same spot you'd click to merge two slabs of the *same* type into a normal
 * vanilla double slab.
 * <p>
 * Only clicks on the exposed flat top/bottom face count; clicking a side
 * face is left alone so a mis-aimed click just places a slab in the
 * adjacent space as usual, matching how same-type slab merging already
 * feels in vanilla.
 */
public class MixedSlabInteraction {
	public static void initialize() {
		UseBlockCallback.EVENT.register(MixedSlabInteraction::onUseBlock);
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack heldStack = player.getItemInHand(hand);
		if (!(heldStack.getItem() instanceof BlockItem heldBlockItem) || !(heldBlockItem.getBlock() instanceof SlabBlock newSlab)) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hit.getBlockPos();
		BlockState existingState = level.getBlockState(pos);
		if (!(existingState.getBlock() instanceof SlabBlock existingSlab) || existingSlab == newSlab) {
			return InteractionResult.PASS;
		}

		SlabType existingType = existingState.getValue(SlabBlock.TYPE);
		if (existingType == SlabType.DOUBLE) {
			return InteractionResult.PASS;
		}

		// Only the face exposing the empty half counts - the same face you'd
		// click to merge two same-type slabs into a vanilla double slab.
		Direction emptyHalfFace = existingType == SlabType.BOTTOM ? Direction.UP : Direction.DOWN;
		if (hit.getDirection() != emptyHalfFace) {
			return InteractionResult.PASS;
		}

		Block bottomSlab = existingType == SlabType.BOTTOM ? existingSlab : newSlab;
		Block topSlab = existingType == SlabType.BOTTOM ? newSlab : existingSlab;

		MixedSlabBlock combined = MixedSlabBlocks.get(bottomSlab, topSlab);
		if (combined == null) {
			// Not a supported material pairing - fall through to normal placement.
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		level.setBlockAndUpdate(pos, combined.defaultBlockState());
		level.playSound(null, pos, existingState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		return InteractionResult.SUCCESS;
	}
}
