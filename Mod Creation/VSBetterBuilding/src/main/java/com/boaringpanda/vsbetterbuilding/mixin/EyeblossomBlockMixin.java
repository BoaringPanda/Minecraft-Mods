package com.boaringpanda.vsbetterbuilding.mixin;

import java.util.function.Predicate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EyeblossomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.blockscan.BlockMatcher;

import com.boaringpanda.vsbetterbuilding.block.FlowerClumps;

/**
 * A clump of eyeblossoms ({@link FlowerClumps}) opens and closes as a clump. Vanilla swaps in the other eyeblossom's default state (a
 * single flower), and passes the change on only to nearby eyeblossoms in exactly the same state, which would leave clumps out.
 */
@Mixin(EyeblossomBlock.class)
public class EyeblossomBlockMixin {
	@WrapOperation(method = "tryChangingState",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean vsbetterbuilding$keepClump(ServerLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
		return original.call(level, pos, FlowerClumps.keepFlowers(level.getBlockState(pos), state));
	}

	@WrapOperation(method = "tryChangingState",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/blockscan/BlockMatcher;filterState(Ljava/util/function/Predicate;)Lnet/minecraft/world/level/blockscan/BlockMatcher;"))
	private BlockMatcher vsbetterbuilding$waveReachesClumps(BlockMatcher matcher, Predicate<BlockState> sameState,
			Operation<BlockMatcher> original) {
		Block self = (Block) (Object) this;
		return original.call(matcher, (Predicate<BlockState>) nearby -> nearby.is(self));
	}
}
