package com.boaringpanda.bettervanillaqol.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {
	// Farmland is never trampled back to dirt by a landing entity, with or without a crop on it. Only the trampling is skipped: the
	// rest of fallOn (fall damage) still runs.
	@WrapWithCondition(
			method = "fallOn",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToBaseBlock(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
	private boolean bettervanillaqol$noTrampling(FarmlandBlock farmland, Entity entity, BlockState state, Level level, BlockPos pos) {
		return false;
	}
}
