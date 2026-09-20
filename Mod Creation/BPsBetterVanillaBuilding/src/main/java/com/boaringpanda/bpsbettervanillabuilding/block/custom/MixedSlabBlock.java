package com.boaringpanda.bpsbettervanillabuilding.block.custom;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A full-size block made of two different slabs stacked into the same block
 * space - one material on the bottom half, a different one on top.
 * <p>
 * Purely visual: rendering is handled entirely by the {@code multipart}
 * blockstate JSON layering the two vanilla half-slab models on top of each
 * other, so this class only needs to describe its drops. Its shape is the
 * default full cube.
 * <p>
 * Never placed directly - it has no {@code BlockItem} and never appears in
 * the creative inventory. It only ever appears as the result of combining
 * two different slabs, see {@link com.boaringpanda.bpsbettervanillabuilding.block.MixedSlabInteraction}.
 */
public class MixedSlabBlock extends Block {
	private final Block bottomSlab;
	private final Block topSlab;

	public MixedSlabBlock(Properties properties, Block bottomSlab, Block topSlab) {
		super(properties);
		this.bottomSlab = bottomSlab;
		this.topSlab = topSlab;
	}

	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);

		// This block always requiresCorrectToolForDrops(), so mirror vanilla's
		// own rule for suppressing drops: nothing in creative, and nothing if
		// the tool used wasn't the required one.
		boolean shouldDrop = !player.isCreative() && (!state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state));
		if (shouldDrop) {
			popResource(level, pos, new ItemStack(this.bottomSlab));
			popResource(level, pos, new ItemStack(this.topSlab));
		}
	}
}
