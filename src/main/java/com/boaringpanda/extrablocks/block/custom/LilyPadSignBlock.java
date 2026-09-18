package com.boaringpanda.extrablocks.block.custom;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sign (any of the 13 wood types) standing on a lily pad. Blank/not
 * writable, same simplification as every other decorative accessory - but
 * unlike those, it has a real 16-value rotation, matching a sign placed on
 * any other block, rather than one fixed orientation.
 * <p>
 * Reuses {@link StandingSignBlock#ROTATION} directly rather than declaring a
 * duplicate property - see {@link com.boaringpanda.extrablocks.block.LilyPadAccessoryInteraction}
 * for how it's set from the placing player's facing (the exact vanilla
 * formula, {@code RotationSegment.convertToSegment(player.getYRot() + 180)},
 * checked by disassembly rather than guessed), since this isn't placed
 * through the normal {@code BlockPlaceContext}-driven pipeline that would
 * compute it for a real sign automatically.
 * <p>
 * Trade-off worth knowing: because the rotation now needs to reflect where
 * the player was facing, the merged model's "y" transform is driven by that
 * instead of the lily pad's own position-based pick every other accessory
 * preserves (see the rotation notes on {@code LilyPadAccessories}) - the
 * pad's texture orientation under a sign follows the sign's facing, not
 * "whatever a plain lily pad would show at this position".
 */
public class LilyPadSignBlock extends Block implements LilyPadCombo {
	private final Block accessory;

	public LilyPadSignBlock(Properties properties, Block accessory) {
		super(properties);
		this.accessory = accessory;
	}

	public Block accessory() {
		return this.accessory;
	}

	@Override
	public List<ItemStack> accessoryDrops(BlockState state) {
		return List.of(new ItemStack(this.accessory));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(StandingSignBlock.ROTATION);
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
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);

		boolean shouldDrop = !player.isCreative() && (!state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state));
		if (shouldDrop) {
			popResource(level, pos, new ItemStack(Blocks.LILY_PAD));
			for (ItemStack drop : accessoryDrops(state)) {
				popResource(level, pos, drop);
			}
		}
	}
}
