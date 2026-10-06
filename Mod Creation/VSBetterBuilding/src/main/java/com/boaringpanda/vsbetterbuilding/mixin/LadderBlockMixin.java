package com.boaringpanda.vsbetterbuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ladders hang from the ladder above them: only the top ladder of a column needs a block behind it. A ladder stays up if it has a
 * block behind it or the ladder directly above (facing the same way) stays up, so when the top one loses its support, every ladder
 * hanging from it breaks and drops in turn.
 */
@Mixin(LadderBlock.class)
public class LadderBlockMixin {
	@ModifyReturnValue(method = "canSurvive", at = @At("RETURN"))
	private boolean vsbetterbuilding$hangFromLadderAbove(boolean attached, BlockState state, LevelReader level, BlockPos pos) {
		if (attached) {
			return true;
		}
		Direction facing = state.getValue(LadderBlock.FACING);
		for (BlockPos above = pos.above(); !level.isOutsideBuildHeight(above); above = above.above()) {
			BlockState aboveState = level.getBlockState(above);
			if (!(aboveState.getBlock() instanceof LadderBlock) || aboveState.getValue(LadderBlock.FACING) != facing) {
				return false;
			}
			BlockPos behind = above.relative(facing.getOpposite());
			if (level.getBlockState(behind).isFaceSturdy(level, behind, facing)) {
				return true;
			}
		}
		return false;
	}

	/** Vanilla only re-checks a ladder when the block behind it changes; a hanging ladder also has to when the ladder above goes. */
	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$fallWithLadderAbove(
			BlockState state,
			LevelReader level,
			ScheduledTickAccess ticks,
			BlockPos pos,
			Direction directionToNeighbour,
			BlockPos neighbourPos,
			BlockState neighbourState,
			RandomSource random,
			CallbackInfoReturnable<BlockState> cir) {
		if (directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)) {
			cir.setReturnValue(Blocks.AIR.defaultBlockState());
		}
	}
}
