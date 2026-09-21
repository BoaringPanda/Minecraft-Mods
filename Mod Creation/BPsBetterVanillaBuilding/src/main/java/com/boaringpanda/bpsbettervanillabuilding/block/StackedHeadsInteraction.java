package com.boaringpanda.bpsbettervanillabuilding.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadCombo;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlockEntity;

/**
 * Lets a player put a second head on top of a floor head, inside the same block space, by right-clicking the top of
 * the head with another head. A head is half a block tall, so vanilla would leave the new one in the block above with a
 * half-block gap under it. A wall head, or a head on a lily pad, doesn't count and is left to its own rules.
 * <p>
 * Two cases, both only on the top face:
 * <ul>
 *   <li>a plain single head becomes a {@link StackedHeadsBlock} holding it and the new head;</li>
 *   <li>a pair block left with one head in its bottom half (the top one was broken) takes a new head in the empty top half.</li>
 * </ul>
 * Clicking the top of a full pair falls through to vanilla, which places the next head in the block above, and that one
 * can take a partner in turn, so a tall stack is just more pairs.
 */
public class StackedHeadsInteraction {
	public static void initialize() {
		UseBlockCallback.EVENT.register(StackedHeadsInteraction::onUseBlock);
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack heldStack = player.getItemInHand(hand);
		// getBlock() on a head item is the standing head, so a wall head can't be the "new" one either way.
		if (!(heldStack.getItem() instanceof BlockItem heldBlockItem) || !(heldBlockItem.getBlock() instanceof SkullBlock newHead)) {
			return InteractionResult.PASS;
		}

		if (hit.getDirection() != Direction.UP || !player.mayBuild()) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hit.getBlockPos();
		BlockState existingState = level.getBlockState(pos);

		// Either a lone head in a pair block (its top half is free), or a plain single head. A head on a lily pad is a
		// SkullBlock too, but it is a combo with its own rules.
		StackedHeadsBlockEntity pair = null;
		SkullBlockEntity single = null;
		if (existingState.getBlock() instanceof StackedHeadsBlock) {
			if (!(level.getBlockEntity(pos) instanceof StackedHeadsBlockEntity heads) || !heads.has(false) || heads.has(true)) {
				return InteractionResult.PASS;
			}
			pair = heads;
		} else if (existingState.getBlock() instanceof SkullBlock && !(existingState.getBlock() instanceof LilyPadCombo)) {
			if (!(level.getBlockEntity(pos) instanceof SkullBlockEntity existingHead)) {
				return InteractionResult.PASS;
			}
			single = existingHead;
		} else {
			return InteractionResult.PASS;
		}

		// The new head takes the top half of the block. Something standing there (a player on the first head, say)
		// would end up inside it, so refuse like vanilla's own placement does.
		if (!level.isUnobstructed(null, Shapes.create(0.25, 0.5, 0.25, 0.75, 1.0, 0.75).move(pos.getX(), pos.getY(), pos.getZ()))) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		boolean powered = level.hasNeighborSignal(pos);
		StackedHeadsBlockEntity heads = pair != null ? pair : convertToPair(level, pos, existingState, single, powered);
		if (heads == null) {
			return InteractionResult.PASS;
		}

		// A head faces the player's own yaw, with no +180 (unlike a sign or banner): SkullBlock.getStateForPlacement.
		BlockState topState = newHead.defaultBlockState()
				.setValue(SkullBlock.ROTATION, RotationSegment.convertToSegment(player.getYRot()))
				.setValue(AbstractSkullBlock.POWERED, powered);
		heads.replaceHead(true, topState).applyComponentsFromItemStack(heldStack);
		heads.setChanged();
		// A pair block that was already there needs telling to send its new data; a fresh one is sent with the block itself.
		level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_ALL);

		level.playSound(null, pos, newHead.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		return InteractionResult.SUCCESS;
	}

	/** Swaps a plain head for a pair block holding that same head (its state and data kept exactly) in the bottom half. */
	@Nullable
	private static StackedHeadsBlockEntity convertToPair(Level level, BlockPos pos, BlockState headState, SkullBlockEntity head, boolean powered) {
		var existingData = head.collectComponents();
		level.setBlockAndUpdate(pos, StackedHeads.BLOCK.defaultBlockState().setValue(StackedHeadsBlock.POWERED, powered));
		if (!(level.getBlockEntity(pos) instanceof StackedHeadsBlockEntity heads)) {
			return null;
		}

		heads.replaceHead(false, headState).applyComponents(existingData, DataComponentPatch.EMPTY);
		return heads;
	}
}
