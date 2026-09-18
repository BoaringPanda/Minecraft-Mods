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
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A candle (any of the 17 colors) standing on a lily pad, with the real
 * stacking (1-4), lighting and extinguishing behaviour, not just a picture.
 * <p>
 * Extends the real {@link CandleBlock} to inherit its {@code CANDLES}/
 * {@code LIT}/{@code WATERLOGGED} blockstate properties and (via the
 * {@code minecraft:candles} block tag it's registered into, see
 * {@link com.boaringpanda.extrablocks.block.LilyPadAccessories}) flint-and-steel lighting, for free.
 * <p>
 * What it does NOT inherit: vanilla's own "click with another candle to add
 * one" check compares the held item against {@code this.asItem()}, which is
 * {@code AIR} for a block with no registered {@code BlockItem} - so it never
 * matches. Stacking is instead handled explicitly by
 * {@link com.boaringpanda.extrablocks.block.LilyPadAccessoryInteraction},
 * the same way the initial lily-pad-to-candle combine is.
 */
public class LilyPadCandleBlock extends CandleBlock implements LilyPadCombo {
	/** The real vanilla candle block for this color - used for drops and for matching the held item when stacking. */
	private final Block accessory;

	public LilyPadCandleBlock(Properties properties, Block accessory) {
		super(properties);
		this.accessory = accessory;
	}

	public Block accessory() {
		return this.accessory;
	}

	@Override
	public List<ItemStack> accessoryDrops(BlockState state) {
		return List.of(new ItemStack(this.accessory, state.getValue(CANDLES)));
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
		// own interaction code, not vanilla's normal candle-placement rules.
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
