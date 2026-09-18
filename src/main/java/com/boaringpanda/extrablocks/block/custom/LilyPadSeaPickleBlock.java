package com.boaringpanda.extrablocks.block.custom;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sea pickle cluster standing on a lily pad, with real stacking (1-4), not
 * just a picture. Always non-waterlogged (sitting on top of the pad, not
 * submerged), so it uses the "dry" appearance and doesn't glow - matching
 * a real sea pickle taken out of water.
 * <p>
 * Extends the real {@link SeaPickleBlock} to inherit its {@code PICKLES}/
 * {@code WATERLOGGED} blockstate properties. Stacking isn't inherited
 * though - vanilla's own "click with another pickle to add one" check
 * compares the held item against {@code this.asItem()}, which is
 * {@code AIR} for a block with no registered {@code BlockItem}. Handled
 * explicitly by {@link com.boaringpanda.extrablocks.block.LilyPadAccessoryInteraction}
 * instead, same as the initial lily-pad-to-pickle combine.
 */
public class LilyPadSeaPickleBlock extends SeaPickleBlock implements LilyPadCombo {
	public LilyPadSeaPickleBlock(Properties properties) {
		super(properties);
	}

	@Override
	public List<ItemStack> accessoryDrops(BlockState state) {
		return List.of(new ItemStack(Blocks.SEA_PICKLE, state.getValue(PICKLES)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LilyPadShape.SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LilyPadShape.SHAPE;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		// Never pops off - this only ever exists on top of a lily pad, placed by our
		// own interaction code, not vanilla's normal sea-pickle-placement rules
		// (which require being on top of water).
		return true;
	}

	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);

		if (!player.isCreative()) {
			popResource(level, pos, new ItemStack(Blocks.LILY_PAD));
			for (ItemStack drop : accessoryDrops(state)) {
				popResource(level, pos, drop);
			}
		}
	}
}
