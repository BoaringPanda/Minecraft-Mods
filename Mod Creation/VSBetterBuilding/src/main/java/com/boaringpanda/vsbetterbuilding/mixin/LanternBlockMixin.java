package com.boaringpanda.vsbetterbuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.vsbetterbuilding.block.WallLanterns;

/**
 * Lanterns on walls ({@link WallLanterns}): placing by the clicked face, a wall lantern's support (the block behind it) and its shape.
 * Covers the copper lanterns too ({@code WeatheringLanternBlock} extends {@code LanternBlock}).
 */
@Mixin(LanternBlock.class)
public abstract class LanternBlockMixin extends Block {
	private LanternBlockMixin(Properties properties) {
		super(properties);
	}

	/** Vanilla picks standing or hanging by where the player looks; this goes by the face they clicked, and adds the wall. */
	@Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$placeByClickedFace(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		BlockState placed = WallLanterns.place(context, this);
		if (placed != null) {
			cir.setReturnValue(placed);
		}
	}

	@Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$wallSupport(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		Direction wall = WallLanterns.wall(state);
		if (wall != null) {
			cir.setReturnValue(WallLanterns.survives(wall, level, pos));
		}
	}

	/** Vanilla only re-checks a lantern when the block above or below changes; a wall lantern also has to when its wall goes. */
	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$fallWithWall(
			BlockState state,
			LevelReader level,
			ScheduledTickAccess ticks,
			BlockPos pos,
			Direction directionToNeighbour,
			BlockPos neighbourPos,
			BlockState neighbourState,
			RandomSource random,
			CallbackInfoReturnable<BlockState> cir) {
		if (directionToNeighbour == WallLanterns.wall(state) && !state.canSurvive(level, pos)) {
			cir.setReturnValue(Blocks.AIR.defaultBlockState());
		}
	}

	@Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$wallShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context,
			CallbackInfoReturnable<VoxelShape> cir) {
		Direction wall = WallLanterns.wall(state);
		if (wall != null) {
			cir.setReturnValue(WallLanterns.shape(wall));
		}
	}

	/** Structures turn and flip a wall lantern with its wall. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return WallLanterns.rotate(state, rotation);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return WallLanterns.mirror(state, mirror);
	}
}
