package com.boaringpanda.bpsbettervanillabuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ladders hang like vines. On top of vanilla's "sturdy block behind me" rule, a ladder also survives when the
 * block above it is a ladder or has a sturdy underside, so a chain of ladders hangs from the block at the
 * top, and breaking that block drops the whole chain. Common code, since the server decides whether a
 * ladder stays.
 * <p>
 * Three injections:
 * <ul>
 * <li>{@code canSurvive}: adds the hanging rule when vanilla says no. Vanilla's own wall rule is untouched.</li>
 * <li>{@code updateShape}: vanilla only re-checks a ladder when the block behind it changes. A hanging ladder
 * has to re-check when the block above changes, or nothing would ever pop the chain. Returning air here drops
 * the item and updates the next ladder down, which pops it the same way.</li>
 * <li>{@code getStateForPlacement}: vanilla takes the first horizontal looking direction where
 * {@code canSurvive} is true. Now that hanging counts, that is the first one even when another side has a
 * wall, so a supported side is swapped in afterwards to keep ladders sticking to walls as they always did.</li>
 * </ul>
 */
@Mixin(LadderBlock.class)
public abstract class LadderBlockMixin {
	@Shadow
	protected abstract boolean canAttachTo(BlockGetter level, BlockPos pos, Direction direction);

	@Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
	private void bpsbettervanillabuilding$hangFromAbove(BlockState state, LevelReader level, BlockPos pos,
			CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			return;
		}
		BlockPos above = pos.above();
		BlockState aboveState = level.getBlockState(above);
		cir.setReturnValue(aboveState.getBlock() instanceof LadderBlock
				|| aboveState.isFaceSturdy(level, above, Direction.DOWN));
	}

	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void bpsbettervanillabuilding$popWhenTopIsGone(BlockState state, LevelReader level,
			ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
			BlockState neighbourState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
		if (directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)) {
			cir.setReturnValue(Blocks.AIR.defaultBlockState());
		}
	}

	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void bpsbettervanillabuilding$preferSupportedSide(BlockPlaceContext context,
			CallbackInfoReturnable<BlockState> cir) {
		BlockState state = cir.getReturnValue();
		if (state == null) {
			return;
		}
		BlockPos pos = context.getClickedPos();
		for (Direction direction : context.getNearestLookingDirections()) {
			if (direction.getAxis().isHorizontal() && this.canAttachTo(context.getLevel(), pos.relative(direction), direction.getOpposite())) {
				cir.setReturnValue(state.setValue(LadderBlock.FACING, direction.getOpposite()));
				return;
			}
		}
	}
}
