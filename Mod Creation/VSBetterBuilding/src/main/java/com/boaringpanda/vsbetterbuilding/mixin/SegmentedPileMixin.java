package com.boaringpanda.vsbetterbuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.vsbetterbuilding.block.AimedSegments;

/**
 * Leaf litter, pink petals and wildflowers take each new piece in the quarter you aim at ({@link AimedSegments}). This changes the
 * questions vanilla already asks when placing, so sneaking, the "can it stay here" check and the place sound are vanilla's.
 */
@Mixin({LeafLitterBlock.class, FlowerBedBlock.class})
public class SegmentedPileMixin {
	/**
	 * A pile only takes another piece of itself into an empty quarter. Aiming at a filled one sends vanilla on to the space above,
	 * where a pile can't stand, so nothing is placed. Leaf litter can still be replaced by anything else, as in vanilla.
	 */
	@ModifyReturnValue(
			method = "canBeReplaced(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/context/BlockPlaceContext;)Z",
			at = @At("RETURN"))
	private boolean vsbetterbuilding$onlyIntoEmptyQuarter(boolean replaceable, BlockState state, BlockPlaceContext context) {
		return replaceable && (!context.getItemInHand().is(state.getBlock().asItem()) || AimedSegments.isFree(state, context));
	}

	@ModifyReturnValue(method = "getStateForPlacement", at = @At("RETURN"))
	private BlockState vsbetterbuilding$intoAimedQuarter(BlockState state, BlockPlaceContext context) {
		return AimedSegments.place(context, state);
	}

	@ModifyReturnValue(method = "getShape", at = @At("RETURN"))
	private VoxelShape vsbetterbuilding$shapeOfPieces(VoxelShape shape, BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return AimedSegments.shape(state, shape);
	}
}
