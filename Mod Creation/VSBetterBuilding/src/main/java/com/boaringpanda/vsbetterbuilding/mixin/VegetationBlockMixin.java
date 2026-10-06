package com.boaringpanda.vsbetterbuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterbuilding.block.AimedSegments;

/**
 * Pink petals and wildflowers stand on top of any full block and on leaves ({@link AimedSegments#supports}), as well as on vanilla's
 * dirt-type blocks. Every other plant keeps vanilla's rule.
 */
@Mixin(VegetationBlock.class)
public class VegetationBlockMixin {
	@ModifyReturnValue(method = "mayPlaceOn", at = @At("RETURN"))
	private boolean vsbetterbuilding$flowerBedsOnFullBlocks(boolean supports, BlockState state, BlockGetter level, BlockPos pos) {
		return supports || ((Object) this instanceof FlowerBedBlock && AimedSegments.supports(state, level, pos));
	}
}
