package com.boaringpanda.vsbetterbuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.vsbetterbuilding.block.CornerTorches;

/** A group of torches is outlined torch by torch ({@link CornerTorches#shape}). */
@Mixin({BaseTorchBlock.class, WallTorchBlock.class})
public class CornerTorchShapeMixin {
	@ModifyReturnValue(method = "getShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
			at = @At("RETURN"))
	private VoxelShape vsbetterbuilding$shapeOfGroup(VoxelShape shape, BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return CornerTorches.shape(state, shape);
	}
}
