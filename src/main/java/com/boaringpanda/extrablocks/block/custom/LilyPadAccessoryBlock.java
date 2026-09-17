package com.boaringpanda.extrablocks.block.custom;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A lily pad with a torch or lantern standing on it, in the same block
 * space. Purely visual layering - the blockstate JSON's {@code multipart}
 * stacks the unmodified vanilla lily pad model and the accessory's own
 * unmodified model, so no BlockEntity or custom renderer is needed.
 * <p>
 * Never placed directly - see {@link com.boaringpanda.extrablocks.block.LilyPadAccessoryInteraction}
 * for how it's created by right-clicking a placed lily pad with a torch or lantern.
 */
public class LilyPadAccessoryBlock extends Block {
	/** The real vanilla lily pad's own collision shape - a slightly inset, thin platform. */
	private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 1.5);

	private final Block accessory;

	public LilyPadAccessoryBlock(Properties properties, Block accessory) {
		super(properties);
		this.accessory = accessory;
	}

	public Block accessory() {
		return this.accessory;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);

		boolean shouldDrop = !player.isCreative() && (!state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state));
		if (shouldDrop) {
			popResource(level, pos, new ItemStack(Blocks.LILY_PAD));
			popResource(level, pos, new ItemStack(this.accessory));
		}
	}
}
