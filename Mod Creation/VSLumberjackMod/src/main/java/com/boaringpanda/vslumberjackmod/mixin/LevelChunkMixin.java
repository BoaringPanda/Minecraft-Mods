package com.boaringpanda.vslumberjackmod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vslumberjackmod.PlacedTreeParts;

// Forgets a placed tree part as soon as it's gone, however that happens (broken, burnt, exploded, pushed, commands), so no stale
// spots are left behind: a tree that later grows there is natural. Every block change in a loaded chunk goes through here.
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
	// A tree part turning into another one (stripping a log with an axe) stays remembered.
	@WrapOperation(
			method = "setBlockState",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState vslumberjackmod$forgetRemovedTreePart(
			LevelChunkSection section, int x, int y, int z, BlockState state, Operation<BlockState> original, @Local(argsOnly = true) BlockPos pos) {
		BlockState oldState = original.call(section, x, y, z, state);
		if (oldState.is(PlacedTreeParts.TREE_PARTS) && !state.is(PlacedTreeParts.TREE_PARTS)) {
			PlacedTreeParts.forget((LevelChunk) (Object) this, pos);
		}
		return oldState;
	}
}
