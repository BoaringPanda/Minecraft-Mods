package com.boaringpanda.bpsbettervanillabuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadCandleBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadSeaPickleBlock;

/**
 * Lets the real candle and sea pickle add one more to a lily pad combo, wherever vanilla lets them add to their own.
 * <p>
 * Adding to a stack is two of vanilla's placement questions (checked by disassembly). First "can the held item be added
 * to the block already in the space?", asked of that block, which the combo blocks answer themselves
 * ({@code LilyPadCandleBlock.canBeReplaced}, {@code LilyPadSeaPickleBlock.canBeReplaced}). Then "what block results?",
 * asked of the <em>held</em> item's block, which is the real candle or sea pickle. Theirs is
 * {@code if (existing.is(this)) return existing.cycle(CANDLES)}, and a combo isn't the same block as a real candle, so
 * without this it would work out a fresh candle and replace the combo, losing the lily pad. So this answers it for the
 * combos: one more candle or pickle, with everything else (lit, waterlogged) kept.
 * <p>
 * Common code, since the client predicts the placement with the same rules the server uses. A stack already at 4, another
 * colour of candle, or anything that isn't one of the combos falls through to vanilla.
 */
@Mixin({ CandleBlock.class, SeaPickleBlock.class })
public abstract class CandleAndSeaPickleMixin {
	/** {@code this} is the block of the item in the player's hand. */
	@Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
	private void bpsbettervanillabuilding$addToLilyPadCombo(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
		Block held = (Block) (Object) this;

		if (existing.getBlock() instanceof LilyPadCandleBlock combo && combo.accessory() == held) {
			int candles = existing.getValue(CandleBlock.CANDLES);
			if (candles < 4) {
				cir.setReturnValue(existing.setValue(CandleBlock.CANDLES, candles + 1));
			}
		} else if (existing.getBlock() instanceof LilyPadSeaPickleBlock && held == Blocks.SEA_PICKLE) {
			int pickles = existing.getValue(SeaPickleBlock.PICKLES);
			if (pickles < 4) {
				cir.setReturnValue(existing.setValue(SeaPickleBlock.PICKLES, pickles + 1));
			}
		}
	}
}
