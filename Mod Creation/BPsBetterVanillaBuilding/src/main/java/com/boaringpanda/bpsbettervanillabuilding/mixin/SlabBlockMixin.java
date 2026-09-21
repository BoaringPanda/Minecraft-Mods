package com.boaringpanda.bpsbettervanillabuilding.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import com.boaringpanda.bpsbettervanillabuilding.block.MixedSlabBlocks;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.MixedSlabBlock;

/**
 * Makes a <em>different</em> slab combine with a placed single slab everywhere vanilla lets the <em>same</em> slab merge
 * into a double slab, by asking vanilla's own two placement questions instead of listening for clicks.
 * <p>
 * Vanilla places a block in two steps (checked by disassembly), and both go through {@link SlabBlock}:
 * <ol>
 *   <li><b>"Can the held item replace you?"</b> - {@code canBeReplaced}, asked of the block already in the space. The game
 *       first asks the block that was clicked. If that says no, the item goes in the neighbouring space instead
 *       ({@code BlockPlaceContext.getClickedPos()}), and the block there is asked. For a slab that gives three cases: clicking
 *       the slab itself on its empty half's side of it (a face and height test), or clicking a block <i>next to</i> a slab
 *       (no test at all, so the player can click the full block beside it, or the ground under it). A click handler on the
 *       clicked block can only ever see the first one.</li>
 *   <li><b>"What block results?"</b> - {@code getStateForPlacement}, asked of the <em>held</em> block. Vanilla only returns a
 *       double slab when the block already there is the same slab.</li>
 * </ol>
 * So {@link #bpsbettervanillabuilding$replaceableByDifferentSlab} gives the same answer vanilla gives for the same slab, and
 * {@link #bpsbettervanillabuilding$combineWithDifferentSlab} returns the {@link MixedSlabBlock} for the pair. Everything
 * else is then vanilla's own {@code BlockItem.place}: the sound, using up the stack, the game event, sneaking and
 * blocks that have their own use (a chest still opens), adventure mode, and refusing when something is standing in the
 * empty half.
 * <p>
 * Common code, since the client predicts the placement with the same rules the server uses. A same-slab placement, or a
 * pairing {@link MixedSlabBlocks} doesn't have (a modded slab, say), falls straight through to vanilla.
 */
@Mixin(SlabBlock.class)
public abstract class SlabBlockMixin {
	/** {@code this} is the slab already in the space. */
	@Inject(method = "canBeReplaced", at = @At("HEAD"), cancellable = true)
	private void bpsbettervanillabuilding$replaceableByDifferentSlab(BlockState state, BlockPlaceContext context, CallbackInfoReturnable<Boolean> cir) {
		SlabType type = state.getValue(SlabBlock.TYPE);
		SlabBlock held = heldSlab(context);
		SlabBlock existing = (SlabBlock) (Object) this;
		if (type == SlabType.DOUBLE || held == null || held == existing || combo(existing, held, type) == null) {
			return;
		}

		// Exactly what vanilla answers for the same slab: when the click was on this block, the face and height have to
		// point at the empty half; when the click was on a block beside it, always.
		if (!context.replacingClickedOnBlock()) {
			cir.setReturnValue(true);
			return;
		}

		boolean upperHalf = context.getClickLocation().y - context.getClickedPos().getY() > 0.5;
		Direction face = context.getClickedFace();
		boolean horizontal = face.getAxis().isHorizontal();
		cir.setReturnValue(type == SlabType.BOTTOM
				? face == Direction.UP || upperHalf && horizontal
				: face == Direction.DOWN || !upperHalf && horizontal);
	}

	/** {@code this} is the slab being placed. */
	@Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
	private void bpsbettervanillabuilding$combineWithDifferentSlab(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		BlockState existingState = context.getLevel().getBlockState(context.getClickedPos());
		SlabBlock held = (SlabBlock) (Object) this;
		if (!(existingState.getBlock() instanceof SlabBlock existing) || existing == held) {
			return;
		}

		SlabType type = existingState.getValue(SlabBlock.TYPE);
		if (type == SlabType.DOUBLE) {
			return;
		}

		MixedSlabBlock combo = combo(existing, held, type);
		if (combo != null) {
			cir.setReturnValue(combo.defaultBlockState());
		}
	}

	@Nullable
	private static SlabBlock heldSlab(BlockPlaceContext context) {
		return context.getItemInHand().getItem() instanceof BlockItem item && item.getBlock() instanceof SlabBlock slab ? slab : null;
	}

	/** The combo block for an existing single slab plus a held one: the existing one is on the bottom if it's a bottom slab. */
	@Nullable
	private static MixedSlabBlock combo(SlabBlock existing, SlabBlock held, SlabType existingType) {
		return existingType == SlabType.BOTTOM ? MixedSlabBlocks.get(existing, held) : MixedSlabBlocks.get(held, existing);
	}
}
