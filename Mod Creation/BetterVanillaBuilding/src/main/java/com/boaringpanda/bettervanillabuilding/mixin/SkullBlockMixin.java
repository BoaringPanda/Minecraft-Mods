package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bettervanillabuilding.block.StackedHeads;

/** A standing head with another on top ({@link StackedHeads}) has both heads in its shape. */
@Mixin(SkullBlock.class)
public class SkullBlockMixin {
	@ModifyReturnValue(method = "getShape", at = @At("RETURN"))
	private VoxelShape bettervanillabuilding$topHeadOutline(VoxelShape shape, BlockState state) {
		return StackedHeads.shape(shape, state, true);
	}

	@ModifyReturnValue(method = "getCollisionShape", at = @At("RETURN"))
	private VoxelShape bettervanillabuilding$topHeadCollision(VoxelShape shape, BlockState state) {
		return StackedHeads.shape(shape, state, false);
	}
}
