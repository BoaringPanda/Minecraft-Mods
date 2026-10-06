package com.boaringpanda.vsbetterbuilding.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed breeze rod: standing on it slows you (Slowness I, wearing off about a second after you step off) and chills you a little, with
 * powder snow's frost on the screen at up to {@value #CHILL_TICKS} of the {@code getTicksRequiredToFreeze()} (140), never enough to
 * freeze or hurt.
 */
public class BreezeRodBlock extends PlacedRodBlock {
	/** How long the slowness lasts, refreshed every tick you stand on the rod. */
	private static final int SLOWNESS_TICKS = 20;
	/** How far the chill goes: 30% frozen, a faint frost. Freezing damage only starts when fully frozen. */
	private static final int CHILL_TICKS = 42;
	/**
	 * Added to the freeze counter each tick you stand on the rod. Vanilla thaws 2 a tick when you aren't in powder snow, so this builds the
	 * chill up by 1 a tick, as powder snow does. When you step off it thaws the same way it does out of powder snow.
	 */
	private static final int CHILL_PER_TICK = 3;

	public BreezeRodBlock(Properties properties) {
		super(properties, Items.BREEZE_ROD);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
		if (!level.isClientSide() && entity instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOWNESS_TICKS, 0));

			// canFreeze() is false with leather armour on, as in powder snow. Already colder (from real powder snow): left alone.
			if (living.canFreeze() && living.getTicksFrozen() < CHILL_TICKS) {
				living.setTicksFrozen(Math.min(CHILL_TICKS, living.getTicksFrozen() + CHILL_PER_TICK));
			}
		}

		super.stepOn(level, pos, onState, entity);
	}
}
