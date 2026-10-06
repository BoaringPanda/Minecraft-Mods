package com.boaringpanda.vsbetterbuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.vsbetterbuilding.block.FlowerClumps;

/** A clump of flowers is outlined around every flower in it ({@link FlowerClumps#shape}). */
@Mixin({FlowerBlock.class, MushroomBlock.class})
public class FlowerClumpMixin {
	@ModifyReturnValue(method = "getShape", at = @At("RETURN"))
	private VoxelShape vsbetterbuilding$shapeOfClump(VoxelShape shape, BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return FlowerClumps.shape(state, shape);
	}
}
