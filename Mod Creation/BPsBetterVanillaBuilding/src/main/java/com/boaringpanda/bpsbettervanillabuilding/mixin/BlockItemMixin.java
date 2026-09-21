package com.boaringpanda.bpsbettervanillabuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bpsbettervanillabuilding.block.StackedFlowers;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadCandleBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadSeaPickleBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.MixedSlabBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedFlowerBlock;

/**
 * Plays the place sound of the item that was just placed when it makes one of this mod's combined blocks, instead of the
 * combined block's. Vanilla's {@code BlockItem.place} plays the placed <em>block state's</em> place sound (via
 * {@code getPlaceSound}), and a combined block is one block that can only have one sound. So without this a stone slab placed
 * on a wood slab made the wood sound, and a candle added to a lily pad made the lily pad's. {@code this.getBlock()} here is the
 * block of the item in the player's hand, which is exactly what was placed, so it is the sound to play. Two cases:
 * <ul>
 *   <li>a slab placed to make a {@link MixedSlabBlock};</li>
 *   <li>a candle or sea pickle added to a lily pad that already has one (the first one is placed by
 *       {@code LilyPadAccessoryInteraction}, which plays its own sound).</li>
 * </ul>
 * <p>
 * Common code, since the placing client plays its own sound and the server plays it for everyone nearby. Anything else falls
 * through to vanilla. Volume and pitch still come from the combined block's sound type, which is close to the same.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Shadow
	protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

	/**
	 * A small flower placed onto the same flower makes a stack of two, and onto a stack of fewer than four adds one, wherever
	 * vanilla lets the click add to it (see {@link BlockBehaviourMixin}, which says the flower can be added to). Vanilla asks the
	 * <em>held</em> item's block what block results, and a flower would only make a fresh single one, replacing the flower that is
	 * there. So when the block in the space is that same flower or its stack, this answers instead, and refuses (null) rather than
	 * falling back if the result can't be placed, since falling back would replace the flower with a new single one and use up
	 * the item.
	 */
	@Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
	private void bpsbettervanillabuilding$stackFlowers(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		Block held = ((BlockItem) (Object) this).getBlock();
		StackedFlowerBlock stacked = StackedFlowers.of(held);
		if (stacked == null) {
			return;
		}

		BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
		BlockState result;
		if (existing.is(held)) {
			result = stacked.defaultBlockState();
		} else if (existing.getBlock() == stacked) {
			int flowers = existing.getValue(StackedFlowerBlock.FLOWERS);
			result = flowers < 4 ? existing.setValue(StackedFlowerBlock.FLOWERS, flowers + 1) : null;
		} else {
			return;
		}

		cir.setReturnValue(result != null && this.canPlace(context, result) ? result : null);
	}

	@Inject(method = "getPlaceSound", at = @At("HEAD"), cancellable = true)
	private void bpsbettervanillabuilding$placeSoundOfThePlacedItem(BlockState state, CallbackInfoReturnable<SoundEvent> cir) {
		Block held = ((BlockItem) (Object) this).getBlock();
		boolean placedFromItsOwnParts = state.getBlock() instanceof MixedSlabBlock combo && (held == combo.bottomSlab() || held == combo.topSlab())
				|| state.getBlock() instanceof LilyPadCandleBlock candles && held == candles.accessory()
				|| state.getBlock() instanceof LilyPadSeaPickleBlock && held == Blocks.SEA_PICKLE;
		if (placedFromItsOwnParts) {
			cir.setReturnValue(held.defaultBlockState().getSoundType().getPlaceSound());
		}
	}
}
