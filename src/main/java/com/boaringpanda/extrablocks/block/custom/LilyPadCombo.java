package com.boaringpanda.extrablocks.block.custom;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Implemented by every "lily pad + accessory" combo block. Lets
 * {@code LilyPadAccessoryBreaking} treat all of them uniformly for the
 * "remove just the top, keep the lily pad" break logic, despite them not
 * sharing a common superclass ({@link LilyPadAccessoryBlock} directly,
 * {@link LilyPadCandleBlock} extends the real {@code CandleBlock},
 * {@link LilyPadSeaPickleBlock} extends the real {@code SeaPickleBlock}).
 */
public interface LilyPadCombo {
	/** What removing just the accessory (not the lily pad underneath) should drop, for this exact state. */
	List<ItemStack> accessoryDrops(BlockState state);
}
