package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bettervanillabuilding.block.FlowerClumps;

/** A clump of mushrooms spreads one mushroom at a time, as a single one does; vanilla would copy the whole clump ({@link FlowerClumps}). */
@Mixin(MushroomBlock.class)
public class MushroomBlockMixin {
	@WrapOperation(method = "randomTick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private boolean bettervanillabuilding$spreadOne(ServerLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
		return original.call(level, pos, FlowerClumps.single(state), flags);
	}
}
