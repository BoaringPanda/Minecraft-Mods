package com.boaringpanda.extrablocks.block.custom;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A potted plant (the pot itself, not the empty {@code lily_pad_with_flower_pot})
 * standing on a lily pad - the result of right-clicking that empty pot with a
 * valid plant, see {@link com.boaringpanda.extrablocks.block.LilyPadAccessoryInteraction}.
 * <p>
 * Only different from the plain {@link LilyPadAccessoryBlock} in what it drops:
 * a real potted plant drops both the flower pot *and* the plant when broken, not
 * just one item, so this adds the flower pot on top of what the parent class
 * already drops (the plant, via its {@code accessory} field, plus the lily pad).
 */
public class LilyPadPottedPlantBlock extends LilyPadAccessoryBlock {
	public LilyPadPottedPlantBlock(Properties properties, Block plant) {
		super(properties, plant);
	}

	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);

		if (!player.isCreative()) {
			popResource(level, pos, new ItemStack(Blocks.FLOWER_POT));
		}
	}
}
