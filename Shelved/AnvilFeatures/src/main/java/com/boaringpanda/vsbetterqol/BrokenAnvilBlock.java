package com.boaringpanda.vsbetterqol;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// An anvil that's used up (see AnvilDurability). Shaped, placed and falls like an anvil, but has no menu until it's repaired with an
// iron block. Not in #minecraft:anvil, so vanilla never treats it as a working anvil and it can't be damaged by falling.
public class BrokenAnvilBlock extends AnvilBlock {
	public BrokenAnvilBlock(Properties properties) {
		super(properties);
	}

	// An iron block turns it back into a brand-new anvil. Anything else goes on to useWithoutItem (the hint).
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult) {
		if (!stack.is(Items.IRON_BLOCK)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
		}

		if (!level.isClientSide()) {
			level.setBlock(pos, Blocks.ANVIL.defaultBlockState().setValue(FACING, state.getValue(FACING)), 3);
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.vsbetterqol.anvil_broken"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return null;
	}
}
