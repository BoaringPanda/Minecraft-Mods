package com.boaringpanda.vsbetterbuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterbuilding.block.LilyPadDecorations;

/**
 * A flower pot on a lily pad stays on it when a plant goes in or comes out, or a potted eyeblossom opens or closes. Each of those
 * swaps in another pot block built from its default state, which isn't on a pad ({@link LilyPadDecorations#keepPad}).
 */
@Mixin(FlowerPotBlock.class)
public class FlowerPotBlockMixin {
	@WrapOperation(method = {"useItemOn", "useWithoutItem"},
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean vsbetterbuilding$keepPad(Level level, BlockPos pos, BlockState state, Operation<Boolean> original) {
		return original.call(level, pos, LilyPadDecorations.keepPad(level.getBlockState(pos), state));
	}

	@WrapOperation(method = "randomTick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean vsbetterbuilding$keepPadEyeblossom(ServerLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
		return original.call(level, pos, LilyPadDecorations.keepPad(level.getBlockState(pos), state));
	}
}
