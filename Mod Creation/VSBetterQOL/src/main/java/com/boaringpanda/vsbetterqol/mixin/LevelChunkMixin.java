package com.boaringpanda.vsbetterqol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterqol.PlacedLogs;
import com.boaringpanda.vsbetterqol.StonecutterStorage;

// Forgets a placed log as soon as it's gone, however that happens (broken, burnt, exploded, pushed, commands), so no stale spots are
// left behind: a tree that later grows there is natural. Every block change in a loaded chunk goes through here.
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
	// A log turning into another log (stripping with an axe) stays remembered.
	@WrapOperation(
			method = "setBlockState",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState vsbetterqol$forgetRemovedLog(
			LevelChunkSection section, int x, int y, int z, BlockState state, Operation<BlockState> original, @Local(argsOnly = true) BlockPos pos) {
		BlockState oldState = original.call(section, x, y, z, state);
		if (oldState.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY) && !state.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY)) {
			PlacedLogs.forget((LevelChunk) (Object) this, pos);
		}
		// A stonecutter that's gone drops its stored items (StonecutterStorage). Carrying one takes them out first, so nothing drops then.
		if (oldState.is(Blocks.STONECUTTER) && !state.is(Blocks.STONECUTTER)) {
			StonecutterStorage.dropRemoved((LevelChunk) (Object) this, pos);
		}
		return oldState;
	}
}
