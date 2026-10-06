package com.boaringpanda.vsbetterqol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterqol.PlacedLogs;

// Leaves cut off from their logs are all gone within about 1.5 seconds instead of waiting for a random tick (about a minute on average).
// Player-placed leaves are persistent, so vanilla's decaying() is false for them and they never decay, same as vanilla. Player-placed
// logs don't hold up natural leaves (PlacedLogs).
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
	// 8-30 ticks after a leaf is cut off, on top of the up to ~6 ticks it takes vanilla to spread the distance through the tree. Same
	// as VSLumberjackMod's Tree Falling (FallingCanopy), so both kinds of chopping look the same when both mods are installed.
	private static final int MIN_DECAY_DELAY = 8;
	private static final int MAX_DECAY_DELAY = 30;

	@Shadow
	protected abstract boolean decaying(BlockState state);

	@Shadow
	private static BlockState updateDistance(BlockState state, LevelAccessor level, BlockPos pos) {
		throw new AssertionError();
	}

	// Player-placed logs count as nothing when a leaf works out how far it is from a log, so they never hold up natural leaves.
	@WrapOperation(
			method = "updateDistance",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/LeavesBlock;getDistanceAt(Lnet/minecraft/world/level/block/state/BlockState;)I"))
	private static int vsbetterqol$ignorePlacedLogs(
			BlockState neighbour, Operation<Integer> original, @Local(argsOnly = true) LevelAccessor level, @Local BlockPos.MutableBlockPos neighborPos) {
		if (neighbour.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY) && PlacedLogs.isPlaced(level, neighborPos)) {
			return LeavesBlock.DECAY_DISTANCE;
		}
		return original.call(neighbour);
	}

	// The decay tick scheduled below: if the leaf is still cut off, decay it the same way vanilla's randomTick does. If a natural log
	// grew next to it in the meantime, vanilla's tick runs instead and saves it (a placed log doesn't, see above).
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void vsbetterqol$decay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (this.decaying(state) && this.decaying(updateDistance(state, level, pos))) {
			Block.dropResources(state, level, pos);
			level.removeBlock(pos, false);
			ci.cancel();
		}
	}

	// Vanilla's tick has just worked out the new distance. If that cut the leaf off, schedule the decay a random moment later, so the
	// tree clears in a wave. Only one tick per block can be pending, so neighbours' updates can't bring it forward.
	@Inject(method = "tick", at = @At("TAIL"))
	private void vsbetterqol$scheduleDecay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		BlockState newState = level.getBlockState(pos);
		if (newState.getBlock() == (Object) this && this.decaying(newState)) {
			level.scheduleTick(pos, (Block) (Object) this, MIN_DECAY_DELAY + random.nextInt(MAX_DECAY_DELAY - MIN_DECAY_DELAY + 1));
		}
	}
}
