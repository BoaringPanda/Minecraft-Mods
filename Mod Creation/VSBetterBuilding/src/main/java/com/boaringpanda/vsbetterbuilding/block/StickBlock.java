package com.boaringpanda.vsbetterbuilding.block;

import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed stick. It can't take any weight: once a player or mob has stood on it for {@value #SNAP_TICKS} ticks in a row (sneaking doesn't
 * help) it snaps, with no drop and the sound of a log breaking. Stepping off (or jumping) for more than {@value #GAP_TICKS} ticks starts the
 * count over, so walking across it is safe. Vanilla calls {@code stepOn} every tick while something stands on a block, so the count is
 * just the first and last tick of the current stand; it isn't saved. Broken by hand it drops as usual, and it burns in fire (see
 * {@link PlacedRods}).
 */
public class StickBlock extends PlacedRodBlock {
	/** 1.5 seconds. */
	private static final int SNAP_TICKS = 30;
	/** How long something can be off the stick (a tick where it isn't on the ground) and still count as standing on it. */
	private static final int GAP_TICKS = 2;
	/** Server side, per level: each stick being stood on (its {@code BlockPos.asLong}) → {first tick of this stand, last tick stood on}. */
	private static final Map<Level, Long2ObjectMap<long[]>> STANDS = new WeakHashMap<>();

	public StickBlock(Properties properties) {
		super(properties, Items.STICK);
	}

	/** Snapping breaks it with no drop. {@code destroyBlock} plays the block's own break sound, the wood one, which is a log breaking. */
	@Override
	public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
		if (!level.isClientSide() && entity instanceof LivingEntity) {
			long now = level.getGameTime();
			Long2ObjectMap<long[]> stands = STANDS.computeIfAbsent(level, key -> new Long2ObjectOpenHashMap<>());
			long[] stand = stands.get(pos.asLong());
			if (stand == null || now - stand[1] > GAP_TICKS) {
				// A new stand. Every stick nobody is on any more is forgotten here, so sticks stepped on once don't pile up.
				stands.values().removeIf(other -> now - other[1] > GAP_TICKS);
				stand = new long[] {now, now};
				stands.put(pos.asLong(), stand);
			}
			stand[1] = now;
			if (now - stand[0] >= SNAP_TICKS) {
				stands.remove(pos.asLong());
				level.destroyBlock(pos, false);
				return;
			}
		}

		super.stepOn(level, pos, onState, entity);
	}
}
