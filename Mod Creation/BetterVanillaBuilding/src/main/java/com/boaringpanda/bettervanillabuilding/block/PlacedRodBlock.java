package com.boaringpanda.bettervanillabuilding.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A vanilla item (a stick, blaze rod or breeze rod) placed as a block, the way an end rod is: pointing out from the face it was placed
 * on. It is vanilla's {@link RodBlock} (the end rod's parent: facing, the rod-shaped hitbox, rotating and mirroring) with the end rod's
 * placement copied, and it gives back its item when picked. It has no item of its own; {@link PlacedRods} places it from the vanilla one.
 */
public class PlacedRodBlock extends RodBlock {
	private final Item item;

	public PlacedRodBlock(Properties properties, Item item) {
		super(properties);
		this.item = item;
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
	}

	/** Copied from {@code EndRodBlock}: out from the clicked face, flipped when placed against the same rod pointing the same way. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clickedFace = context.getClickedFace();
		BlockState against = context.getLevel().getBlockState(context.getClickedPos().relative(clickedFace.getOpposite()));
		return against.is(this) && against.getValue(FACING) == clickedFace
				? this.defaultBlockState().setValue(FACING, clickedFace.getOpposite())
				: this.defaultBlockState().setValue(FACING, clickedFace);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.item);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
