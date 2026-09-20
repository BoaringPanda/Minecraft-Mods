package com.boaringpanda.extrablocks.block.custom;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Implemented by every "lily pad + accessory" combo block. Lets
 * {@code LilyPadAccessoryBreaking} and {@code LilyPadTarget} treat all of them uniformly
 * despite them not sharing a common superclass ({@link LilyPadAccessoryBlock} directly,
 * {@link LilyPadCandleBlock} extends the real {@code CandleBlock},
 * {@link LilyPadSeaPickleBlock} extends the real {@code SeaPickleBlock}, and so on).
 */
public interface LilyPadCombo {
	/**
	 * What removing just the accessory (not the lily pad underneath) should drop, for this exact state.
	 * {@code blockEntity} is the combo's own, if it has one - where a head keeps its skin and a banner
	 * its patterns. Combos without one ignore it.
	 */
	List<ItemStack> accessoryDrops(BlockState state, @Nullable BlockEntity blockEntity);

	/** The accessory's own hitbox on its own, without the pad - used to tell which of the two the player is aiming at. */
	VoxelShape accessoryShape(BlockState state);

	/**
	 * The real vanilla block state the accessory would be on its own. A hit or a break on the accessory
	 * shows this state's particles and plays its sounds, so they match the real block. The combo's own
	 * state would give the lily pad's, since that is what its model and sound type come from.
	 */
	BlockState accessoryState(BlockState state);
}
