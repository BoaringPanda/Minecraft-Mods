package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bettervanillabuilding.block.AimedSegments;

/** Leaf litter also stands on leaves ({@link AimedSegments#supports}); vanilla already lets it stand on any full block. */
@Mixin(LeafLitterBlock.class)
public class LeafLitterBlockMixin {
	@ModifyReturnValue(method = "canSurvive", at = @At("RETURN"))
	private boolean bettervanillabuilding$onLeaves(boolean survives, BlockState state, LevelReader level, BlockPos pos) {
		return survives || AimedSegments.supports(level.getBlockState(pos.below()), level, pos.below());
	}
}
