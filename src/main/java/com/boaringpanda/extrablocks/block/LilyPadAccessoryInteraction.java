package com.boaringpanda.extrablocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import com.boaringpanda.extrablocks.block.custom.LilyPadAccessoryBlock;

/**
 * Lets a player put a torch or lantern on a placed lily pad by right-clicking
 * its top face with the item in hand, combining them into one
 * {@link LilyPadAccessoryBlock} occupying the lily pad's own space.
 */
public class LilyPadAccessoryInteraction {
	public static void initialize() {
		UseBlockCallback.EVENT.register(LilyPadAccessoryInteraction::onUseBlock);
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (hit.getDirection() != Direction.UP) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hit.getBlockPos();
		BlockState existingState = level.getBlockState(pos);
		if (!existingState.is(Blocks.LILY_PAD)) {
			return InteractionResult.PASS;
		}

		ItemStack heldStack = player.getItemInHand(hand);
		if (!(heldStack.getItem() instanceof BlockItem heldBlockItem)) {
			return InteractionResult.PASS;
		}

		Block accessory = heldBlockItem.getBlock();
		LilyPadAccessoryBlock combined = LilyPadAccessories.get(accessory);
		if (combined == null) {
			// Not a supported accessory - fall through to normal placement
			// (e.g. it'll just try to place in the space above, as usual).
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		level.setBlockAndUpdate(pos, combined.defaultBlockState());
		level.playSound(null, pos, accessory.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		return InteractionResult.SUCCESS;
	}
}
