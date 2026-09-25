package com.boaringpanda.bettervanillabuilding.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed blaze rod: it smoulders, with small flames (and the odd wisp of smoke) flickering off random points along it, and it burns
 * whoever walks on it exactly as a campfire does (vanilla {@code CampfireBlock}: campfire damage, 1 per hit, even when sneaking).
 */
public class BlazeRodBlock extends PlacedRodBlock {
	/** A normal campfire's damage (a soul campfire's is 2). */
	private static final float CAMPFIRE_DAMAGE = 1.0F;

	public BlazeRodBlock(Properties properties) {
		super(properties, Items.BLAZE_ROD);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
		if (level instanceof ServerLevel serverLevel && entity instanceof LivingEntity) {
			entity.hurtServer(serverLevel, serverLevel.damageSources().campfire(), CAMPFIRE_DAMAGE);
		}

		super.stepOn(level, pos, onState, entity);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) == 0) {
			addAlongRod(ParticleTypes.SMALL_FLAME, state, level, pos, random);
		}
		if (random.nextInt(8) == 0) {
			addAlongRod(ParticleTypes.SMOKE, state, level, pos, random);
		}
	}

	/** A particle at a random point along the rod, rising gently. */
	private static void addAlongRod(ParticleOptions particle, BlockState state, Level level, BlockPos pos, RandomSource random) {
		Direction direction = state.getValue(FACING);
		double along = random.nextDouble() - 0.5;
		double x = pos.getX() + 0.5 + direction.getStepX() * along;
		double y = pos.getY() + 0.5 + direction.getStepY() * along;
		double z = pos.getZ() + 0.5 + direction.getStepZ() * along;
		level.addParticle(particle, x, y, z, 0.0, 0.01, 0.0);
	}
}
