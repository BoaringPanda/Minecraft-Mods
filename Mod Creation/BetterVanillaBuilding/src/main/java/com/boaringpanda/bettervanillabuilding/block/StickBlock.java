package com.boaringpanda.bettervanillabuilding.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed stick. It can't take any weight: {@value #SNAP_TICKS} ticks after a player or mob first steps on it (even if they've walked off
 * since) it snaps, with no drop and the sound of a log breaking. The pending snap is a scheduled block tick, so it is saved with the
 * chunk. Broken by hand it drops as usual, and it burns in fire (see {@link PlacedRods}).
 */
public class StickBlock extends PlacedRodBlock {
	/** 1.5 seconds. */
	private static final int SNAP_TICKS = 30;

	public StickBlock(Properties properties) {
		super(properties, Items.STICK);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
		if (!level.isClientSide() && entity instanceof LivingEntity && !level.getBlockTicks().hasScheduledTick(pos, this)) {
			level.scheduleTick(pos, this, SNAP_TICKS);
		}

		super.stepOn(level, pos, onState, entity);
	}

	/** Breaks with no drop. {@code destroyBlock} plays the block's own break sound, the wood one, which is a log breaking. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.destroyBlock(pos, false);
	}
}
