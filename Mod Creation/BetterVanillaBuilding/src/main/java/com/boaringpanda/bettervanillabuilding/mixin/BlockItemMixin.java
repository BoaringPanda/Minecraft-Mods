package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import com.boaringpanda.bettervanillabuilding.block.LilyPadDecorations;
import com.boaringpanda.bettervanillabuilding.block.MixedSlabBlockEntity;
import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;
import com.boaringpanda.bettervanillabuilding.block.StackedHeads;

/**
 * When a slab is placed onto a different slab ({@code SlabBlockMixin}), records the two slabs in the new mixed block and plays the
 * placed slab's sound. This runs on the client as well (its own prediction of the placement), so both slabs show at once.
 *
 * <p>Also places decorations onto lily pads ({@link LilyPadDecorations}) and heads on top of or under heads ({@link StackedHeads}).
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Shadow
	public abstract Block getBlock();

	@WrapOperation(
			method = "place",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean bettervanillabuilding$recordMixedSlab(BlockItem item, BlockPlaceContext context, BlockState placementState,
			Operation<Boolean> original) {
		BlockState replaced = context.getLevel().getBlockState(context.getClickedPos());
		boolean placed = original.call(item, context, placementState);
		if (placed && MixedSlabs.is(placementState) && replaced.getBlock() instanceof SlabBlock
				&& context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MixedSlabBlockEntity entity) {
			Block old = replaced.getBlock();
			entity.setHalves(replaced.getValue(SlabBlock.TYPE) == SlabType.TOP
					? new MixedSlabs.Halves(getBlock(), old)
					: new MixedSlabs.Halves(old, getBlock()));
		}
		return placed;
	}

	@WrapOperation(
			method = "place",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
	private SoundType bettervanillabuilding$placedSlabSoundType(BlockState state, Operation<SoundType> original) {
		return original.call(placedSlab(state));
	}

	@WrapOperation(
			method = "place",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/item/BlockItem;getPlaceSound(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/sounds/SoundEvent;"))
	private SoundEvent bettervanillabuilding$placedSlabSound(BlockItem item, BlockState state, Operation<SoundEvent> original) {
		return original.call(item, placedSlab(state));
	}

	/** Placing into a lily pad places onto it: the block stands in the pad's space ({@link LilyPadDecorations#padContext}). */
	@WrapOperation(
			method = "place",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/item/BlockItem;updatePlacementContext(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/item/context/BlockPlaceContext;"))
	private @Nullable BlockPlaceContext bettervanillabuilding$ontoLilyPad(BlockItem item, BlockPlaceContext context,
			Operation<BlockPlaceContext> original) {
		BlockPlaceContext updated = original.call(item, context);
		BlockPlaceContext onPad = updated == null ? null : LilyPadDecorations.padContext(updated);
		return onPad != null ? onPad : updated;
	}

	/** On a pad, the block's standing form is placed with {@code lily_pad} set. */
	@WrapOperation(
			method = "place",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private @Nullable BlockState bettervanillabuilding$standOnLilyPad(BlockItem item, BlockPlaceContext context,
			Operation<BlockState> original) {
		return LilyPadDecorations.placedOnPad(context, original.call(item, context));
	}

	/**
	 * A head placed into the free half of a standing head ({@link StackedHeads#takesHead}) is always the standing form, facing the
	 * player, as on a floor. {@code StandingAndWallBlockItem} would otherwise try a wall head first when the player looks sideways at a
	 * wall behind.
	 */
	@WrapOperation(
			method = "place",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private @Nullable BlockState bettervanillabuilding$standingHeadInStack(BlockItem item, BlockPlaceContext context, Operation<BlockState> original) {
		return StackedHeads.fillsStack(context) ? StackedHeads.placementState(context, getBlock()) : original.call(item, context);
	}

	/**
	 * The new head goes on top of the one already there, or under a raised one. This runs on the client too, so both heads show at
	 * once.
	 */
	@WrapOperation(
			method = "place",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean bettervanillabuilding$placeHeadInStack(BlockItem item, BlockPlaceContext context, BlockState placementState,
			Operation<Boolean> original, @Share("topHead") LocalBooleanRef topHead) {
		if (StackedHeads.fillsStack(context) && placementState.getBlock() instanceof SkullBlock) {
			// Under a raised head the new head is the real bottom head, so vanilla's handling of the item's data below is right for it.
			topHead.set(!StackedHeads.isRaised(context.getLevel().getBlockState(context.getClickedPos())));
			return StackedHeads.placeHead(context.getLevel(), context.getClickedPos(), placementState, context.getItemInHand(), context.getPlayer());
		}
		return original.call(item, context, placementState);
	}

	/**
	 * After placing, vanilla copies the item's skin, name and block entity data onto the block entity at that spot. For a top head that
	 * is the bottom head's, so it's skipped ({@link StackedHeads#placeHead} already gave the top head its own).
	 */
	@ModifyExpressionValue(method = "place",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
	private boolean bettervanillabuilding$keepBottomHeadData(boolean placed, @Share("topHead") LocalBooleanRef topHead) {
		return placed && !topHead.get();
	}

	/** For a mixed block, the slab this item just placed; otherwise {@code state} unchanged. */
	private BlockState placedSlab(BlockState state) {
		return MixedSlabs.is(state) ? getBlock().defaultBlockState() : state;
	}
}
