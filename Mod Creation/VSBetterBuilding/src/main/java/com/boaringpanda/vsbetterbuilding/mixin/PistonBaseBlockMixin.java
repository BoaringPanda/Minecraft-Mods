package com.boaringpanda.vsbetterbuilding.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import com.boaringpanda.vsbetterbuilding.block.MixedSlabCarrier;
import com.boaringpanda.vsbetterbuilding.block.MixedSlabs;

/**
 * Pistons push and pull mixed slab blocks like vanilla double slabs. Vanilla won't move a block with a block entity, and a moving block
 * only remembers its block state, so the two slabs ride along in the moving-piston block entity ({@code PistonMovingBlockEntityMixin}).
 * {@code moveBlocks} runs on the client too (from the piston's block event), so the client carries them the same way.
 */
@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {
	@WrapOperation(
			method = "isPushable",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"))
	private static boolean vsbetterbuilding$mixedSlabsArePushable(BlockState state, Operation<Boolean> original) {
		return !MixedSlabs.is(state) && original.call(state);
	}

	/** A mixed block with an immovable half (an obsidian slab) stays put, as that slab would on its own. */
	@ModifyReturnValue(method = "isPushable", at = @At("RETURN"))
	private static boolean vsbetterbuilding$immovableHalfStays(boolean pushable, @Local(argsOnly = true) BlockState state,
			@Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
		if (!pushable || !MixedSlabs.is(state)) {
			return pushable;
		}
		MixedSlabs.Halves halves = MixedSlabs.halves(level, pos);
		return halves.bottomState().getPistonPushReaction() != PushReaction.IMMOVEABLE
				&& halves.topState().getPistonPushReaction() != PushReaction.IMMOVEABLE;
	}

	/** Reads every pushed mixed block's slabs before any block moves, keyed by where it starts. */
	@WrapOperation(
			method = "moveBlocks",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToPush()Ljava/util/List;"))
	private List<BlockPos> vsbetterbuilding$readPushedSlabs(PistonStructureResolver resolver, Operation<List<BlockPos>> original,
			@Local(argsOnly = true) Level level, @Share("halves") LocalRef<Map<BlockPos, MixedSlabs.Halves>> halves) {
		List<BlockPos> toPush = original.call(resolver);
		Map<BlockPos, MixedSlabs.Halves> found = new HashMap<>();
		for (BlockPos pos : toPush) {
			if (MixedSlabs.is(level.getBlockState(pos))) {
				found.put(pos, MixedSlabs.halves(level, pos));
			}
		}
		halves.set(found);
		return toPush;
	}

	/** Hands a mixed block's slabs to the moving block that replaces it one step along. */
	@WrapOperation(
			method = "moveBlocks",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/piston/MovingPistonBlock;newMovingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;ZZ)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
	private BlockEntity vsbetterbuilding$carrySlabs(BlockPos destination, BlockState blockState, BlockState movedState, Direction direction,
			boolean extending, boolean isSourcePiston, Operation<BlockEntity> original,
			@Share("halves") LocalRef<Map<BlockPos, MixedSlabs.Halves>> halves) {
		BlockEntity moving = original.call(destination, blockState, movedState, direction, extending, isSourcePiston);
		Map<BlockPos, MixedSlabs.Halves> found = halves.get();
		if (found != null && MixedSlabs.is(movedState) && moving instanceof MixedSlabCarrier carrier) {
			Direction pushDirection = extending ? direction : direction.getOpposite();
			carrier.vsbetterbuilding$setHalves(found.get(destination.relative(pushDirection.getOpposite())));
		}
		return moving;
	}
}
