package com.boaringpanda.vsbetterqol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterqol.AnvilDurability;
import com.boaringpanda.vsbetterqol.PlacedLogs;

// Forgets a placed log or an anvil's wear as soon as the block is gone, however that happens (broken, burnt, exploded, pushed,
// fell, commands), so no stale spots are left behind: a tree that later grows there is natural, a new anvil starts fresh. Every
// block change in a loaded chunk goes through here.
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
	// A log turning into another log (stripping with an axe) stays remembered, and so does an anvil turning chipped or damaged.
	@WrapOperation(
			method = "setBlockState",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState vsbetterqol$forgetRemovedBlock(
			LevelChunkSection section, int x, int y, int z, BlockState state, Operation<BlockState> original, @Local(argsOnly = true) BlockPos pos) {
		BlockState oldState = original.call(section, x, y, z, state);
		if (oldState.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY) && !state.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY)) {
			PlacedLogs.forget((LevelChunk) (Object) this, pos);
		}
		if (oldState.is(BlockTags.ANVIL) && !state.is(BlockTags.ANVIL)) {
			AnvilDurability.forget((LevelChunk) (Object) this, pos);
		}
		return oldState;
	}
}
