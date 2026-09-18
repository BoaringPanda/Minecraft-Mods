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
 * <p>
 * {@code playerDestroy} below (dropping both the lily pad and the accessory, block
 * gone entirely) is only reached for non-player destruction (explosions, fire,
 * pistons). A player mining it normally goes through
 * {@link com.boaringpanda.extrablocks.block.LilyPadAccessoryBreaking} instead, which
 * intercepts *before* any of this runs and removes just the accessory, leaving a
 * plain lily pad behind - see {@link #accessoryDrops} for what that drops.
 */
public class LilyPadAccessoryBlock extends Block implements LilyPadCombo {
	private final Block accessory;

	public LilyPadAccessoryBlock(Properties properties, Block accessory) {
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
