package com.boaringpanda.extrablocks.block.custom;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A potted plant (the pot itself, not the empty {@code lily_pad_with_flower_pot})
 * standing on a lily pad - the result of right-clicking that empty pot with a
 * valid plant, see {@link com.boaringpanda.extrablocks.block.LilyPadAccessoryInteraction}.
 * <p>
 * Only different from the plain {@link LilyPadAccessoryBlock} in what it drops:
 * a real potted plant drops both the flower pot *and* the plant when broken, not
 * just the one item {@code accessoryDrops} would otherwise return.
 */
public class LilyPadPottedPlantBlock extends LilyPadAccessoryBlock {
	public LilyPadPottedPlantBlock(Properties properties, Block plant) {
		super(properties, plant);
	}

	@Override
	public List<ItemStack> accessoryDrops(BlockState state) {
		return List.of(new ItemStack(this.accessory()), new ItemStack(Blocks.FLOWER_POT));
	}
}
