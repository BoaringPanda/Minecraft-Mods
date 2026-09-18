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
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import com.boaringpanda.extrablocks.block.custom.LilyPadCandleBlock;
import com.boaringpanda.extrablocks.block.custom.LilyPadSeaPickleBlock;

/**
 * Lets a player put a supported accessory on a placed lily pad by
 * right-clicking its top face with the item in hand, combining them into
 * one block occupying the lily pad's own space (see {@link LilyPadAccessories}
 * for what's supported).
 * <p>
 * Also handles stacking a candle or sea pickle already on a lily pad up to
 * 4, since that isn't inherited "for free" the way lighting/extinguishing
 * are - see {@link LilyPadCandleBlock}/{@link LilyPadSeaPickleBlock}'s own
 * docs for why.
 */
public class LilyPadAccessoryInteraction {
	private static final int MAX_STACK = 4;

	public static void initialize() {
		UseBlockCallback.EVENT.register(LilyPadAccessoryInteraction::onUseBlock);
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (hit.getDirection() != Direction.UP) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hit.getBlockPos();
		BlockState existingState = level.getBlockState(pos);

		ItemStack heldStack = player.getItemInHand(hand);
		if (!(heldStack.getItem() instanceof BlockItem heldBlockItem)) {
			return InteractionResult.PASS;
		}

		Block heldBlock = heldBlockItem.getBlock();

		if (existingState.getBlock() instanceof LilyPadCandleBlock candleCombo && candleCombo.accessory() == heldBlock) {
			return tryStack(level, pos, existingState, player, heldStack, CandleBlock.CANDLES, candleCombo.accessory());
		}

		if (existingState.getBlock() instanceof LilyPadSeaPickleBlock && heldBlock == Blocks.SEA_PICKLE) {
			return tryStack(level, pos, existingState, player, heldStack, SeaPickleBlock.PICKLES, Blocks.SEA_PICKLE);
		}

		if (!existingState.is(Blocks.LILY_PAD)) {
			return InteractionResult.PASS;
		}

		return combine(level, pos, heldBlock, player, heldStack);
	}

	private static InteractionResult combine(Level level, BlockPos pos, Block accessory, Player player, ItemStack heldStack) {
		Block combined = LilyPadAccessories.get(accessory);
		if (combined == null) {
			// Not a supported accessory - fall through to normal placement
			// (e.g. it'll just try to place in the space above, as usual).
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockState placedState = combined.defaultBlockState();
		if (combined instanceof LilyPadCandleBlock) {
			placedState = placedState.setValue(CandleBlock.CANDLES, 1).setValue(CandleBlock.LIT, false);
		} else if (combined instanceof LilyPadSeaPickleBlock) {
			placedState = placedState.setValue(SeaPickleBlock.PICKLES, 1).setValue(SeaPickleBlock.WATERLOGGED, false);
		}

		level.setBlockAndUpdate(pos, placedState);
		level.playSound(null, pos, accessory.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		return InteractionResult.SUCCESS;
	}

	private static InteractionResult tryStack(
			Level level,
			BlockPos pos,
			BlockState existingState,
			Player player,
			ItemStack heldStack,
			IntegerProperty countProperty,
			Block accessory
	) {
		int count = existingState.getValue(countProperty);
		if (count >= MAX_STACK) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		level.setBlockAndUpdate(pos, existingState.setValue(countProperty, count + 1));
		level.playSound(null, pos, accessory.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		return InteractionResult.SUCCESS;
	}
}
