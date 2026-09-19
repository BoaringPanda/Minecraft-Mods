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
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import com.boaringpanda.extrablocks.block.custom.LilyPadBannerBlock;
import com.boaringpanda.extrablocks.block.custom.LilyPadCandleBlock;
import com.boaringpanda.extrablocks.block.custom.LilyPadSeaPickleBlock;
import com.boaringpanda.extrablocks.block.custom.LilyPadSignBlock;
import com.boaringpanda.extrablocks.block.custom.LilyPadSkullBlock;

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

		if (existingState.getBlock() == LilyPadAccessories.LILY_PAD_WITH_FLOWER_POT) {
			return plant(level, pos, heldBlock, player, heldStack);
		}

		if (!existingState.is(Blocks.LILY_PAD)) {
			return InteractionResult.PASS;
		}

		return combine(level, pos, heldBlock, player, heldStack);
	}

	/**
	 * Right-clicking the empty flower pot combo with a plant it actually
	 * accepts (checked via {@link LilyPadAccessories#pottedFor}, which is
	 * only ever populated with real plant/pot pairings - never, say,
	 * sugarcane, which a real flower pot doesn't accept either).
	 */
	private static InteractionResult plant(Level level, BlockPos pos, Block heldBlock, Player player, ItemStack heldStack) {
		Block potted = LilyPadAccessories.pottedFor(heldBlock);
		if (potted == null) {
			// Not something a real flower pot accepts - fall through to
			// normal placement, same as vanilla's own TRY_WITH_EMPTY_HAND.
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		level.setBlockAndUpdate(pos, potted.defaultBlockState());
		level.playSound(null, pos, heldBlock.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		return InteractionResult.SUCCESS;
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
		} else if (combined instanceof LilyPadSignBlock) {
			// Exact vanilla formula for a placed standing sign's rotation, checked
			// by disassembling StandingSignBlock.getStateForPlacement - not guessed.
			int rotation = RotationSegment.convertToSegment(player.getYRot() + 180.0F);
			placedState = placedState.setValue(StandingSignBlock.ROTATION, rotation);
		} else if (combined instanceof LilyPadSkullBlock) {
			// Exact vanilla formulas, from SkullBlock/AbstractSkullBlock.getStateForPlacement (checked by
			// disassembly): a head faces the player's own yaw - no +180, unlike a sign or banner - and
			// starts powered if something next to it already is.
			int rotation = RotationSegment.convertToSegment(player.getYRot());
			placedState = placedState
					.setValue(SkullBlock.ROTATION, rotation)
					.setValue(AbstractSkullBlock.POWERED, level.hasNeighborSignal(pos));
		} else if (combined instanceof LilyPadBannerBlock) {
			// Same formula as a sign, from BannerBlock.getStateForPlacement.
			int rotation = RotationSegment.convertToSegment(player.getYRot() + 180.0F);
			placedState = placedState.setValue(BannerBlock.ROTATION, rotation);
		}

		level.setBlockAndUpdate(pos, placedState);
		if (placedState.hasBlockEntity()) {
			copyItemData(level, player, pos, heldStack);
		}
		level.playSound(null, pos, accessory.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

		if (combined instanceof LilyPadSignBlock signBlock) {
			// Reuses vanilla's own setPlacedBy (not-waxed/editable-text checks, opens
			// the text editor for the placer) rather than reimplementing it - this
			// block is swapped in directly instead of placed via a BlockPlaceContext,
			// so setPlacedBy never fires on its own the way it would for a real sign.
			signBlock.setPlacedBy(level, pos, placedState, player, heldStack);
		}

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * Puts the held item's data (a head's skin, a banner's patterns, a custom name) onto the combo's
	 * freshly placed block entity - what BlockItem.place does for every block entity (checked by
	 * disassembly), which a combo never goes through. Vanilla's own helper for the second half is
	 * private, so it's repeated here. It runs before the stack shrinks, so the data is still on it,
	 * and in the same tick as the placement, so the chunk update that sends the new block to clients
	 * carries the data too.
	 */
	private static void copyItemData(Level level, Player player, BlockPos pos, ItemStack heldStack) {
		BlockItem.updateCustomBlockEntityTag(level, player, pos, heldStack);

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity != null) {
			blockEntity.applyComponentsFromItemStack(heldStack);
			blockEntity.setChanged();
		}
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
