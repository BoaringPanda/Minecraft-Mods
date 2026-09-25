package com.boaringpanda.bettervanillabuilding.mixin;

import java.util.ArrayList;
import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bettervanillabuilding.block.CornerTorches;
import com.boaringpanda.bettervanillabuilding.block.FlowerClumps;
import com.boaringpanda.bettervanillabuilding.block.LilyPadDecorations;
import com.boaringpanda.bettervanillabuilding.block.LockedBlocks;
import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;
import com.boaringpanda.bettervanillabuilding.block.StackedHeads;

/**
 * On a map, a mixed slab block is the colour of its top slab, the one seen from above ({@link MixedSlabs}).
 *
 * <p>A decoration on a lily pad ({@link LilyPadDecorations}) is held up by the pad, has the pad in its shape, drops the pad when the
 * pad goes, and is popped by pistons as a lily pad is. A bare lily pad makes room for a decoration placed into it.
 *
 * <p>A standing head makes room for a second head on top, and a stack of two heads drops both ({@link StackedHeads}).
 *
 * <p>A locked fence, pane, bars or wall ignores its neighbours ({@link LockedBlocks}).
 *
 * <p>A flower makes room for the same flower, and a clump of flowers has no random offset ({@link FlowerClumps}). A group of torches
 * makes room for the same torch ({@link CornerTorches}).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {
	@ModifyReturnValue(method = "getMapColor", at = @At("RETURN"))
	private MapColor bettervanillabuilding$topSlabColour(MapColor colour, BlockGetter level, BlockPos pos) {
		BlockState state = (BlockState) (Object) this;
		return MixedSlabs.is(state) ? MixedSlabs.halves(level, pos).topState().getMapColor(level, pos) : colour;
	}

	/**
	 * A decoration on a pad survives wherever a lily pad would. So does one being placed into a pad (its placement state isn't on the
	 * pad yet, but the pad is still in the world), so vanilla's own placement checks accept it.
	 */
	@Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
	private void bettervanillabuilding$heldUpByPad(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		BlockState state = (BlockState) (Object) this;
		if (state.hasProperty(LilyPadDecorations.LILY_PAD)
				&& (state.getValue(LilyPadDecorations.LILY_PAD) || level.getBlockState(pos).is(Blocks.LILY_PAD))) {
			cir.setReturnValue(LilyPadDecorations.padSurvives(level, pos));
		}
	}

	/**
	 * A locked fence, pane, bars, wall, stair, fence gate or placed rod ({@link LockedBlocks}) keeps what the Builder Stick gave it,
	 * whatever changes next to it. Vanilla's own update has still run, so a waterlogged one still schedules its water tick, and if vanilla
	 * removes the block (it lost its support) that still happens.
	 *
	 * <p>When the pad can't stay (the water went), the decoration breaks with it, as a lily pad does. Panes, chains, rods, pots and heads
	 * never check what's under them, so this is done here for all of them (locked or not).
	 */
	@ModifyReturnValue(method = "updateShape", at = @At("RETURN"))
	private BlockState bettervanillabuilding$keepLockedOrBreakWithPad(BlockState updated, LevelReader level, ScheduledTickAccess ticks,
			BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		BlockState state = (BlockState) (Object) this;
		if (LockedBlocks.isLocked(state) && updated.is(state.getBlock())) {
			updated = state;
		}
		return LilyPadDecorations.onPad(updated) && !LilyPadDecorations.padSurvives(level, pos) ? Blocks.AIR.defaultBlockState() : updated;
	}

	@ModifyReturnValue(
			method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
			at = @At("RETURN"))
	private VoxelShape bettervanillabuilding$padOutline(VoxelShape shape) {
		return LilyPadDecorations.onPad((BlockState) (Object) this) ? LilyPadDecorations.withPad(shape, false) : shape;
	}

	@ModifyReturnValue(
			method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
			at = @At("RETURN"))
	private VoxelShape bettervanillabuilding$padCollision(VoxelShape shape) {
		return LilyPadDecorations.onPad((BlockState) (Object) this) ? LilyPadDecorations.withPad(shape, true) : shape;
	}

	/**
	 * A lily pad makes room for a decoration placed into it from any side, as a candle makes room for another candle. A standing head
	 * makes room for a second head in its free half ({@link StackedHeads#takesHead}), a flower for the same flower clicked onto it
	 * ({@link FlowerClumps#takesFlower}), and a group of torches for the same torch in a free spot ({@link CornerTorches#takesTorch}).
	 */
	@ModifyReturnValue(method = "canBeReplaced(Lnet/minecraft/world/item/context/BlockPlaceContext;)Z", at = @At("RETURN"))
	private boolean bettervanillabuilding$padTakesDecoration(boolean replaceable, BlockPlaceContext context) {
		BlockState state = (BlockState) (Object) this;
		return replaceable || state.is(Blocks.LILY_PAD) && LilyPadDecorations.goesOnPad(context.getItemInHand())
				|| StackedHeads.takesHead(state, context) || FlowerClumps.takesFlower(state, context)
				|| CornerTorches.takesTorch(state, context);
	}

	/** Vanilla moves each flower a random bit sideways; a clump's flowers keep to their fixed pattern ({@link FlowerClumps}). */
	@ModifyReturnValue(method = "getOffset", at = @At("RETURN"))
	private Vec3 bettervanillabuilding$clumpInPattern(Vec3 offset) {
		return FlowerClumps.isClump((BlockState) (Object) this) ? Vec3.ZERO : offset;
	}

	/** Whenever a stack of heads breaks as a whole, the top head drops too, by its own loot table ({@link StackedHeads#topDrops}). */
	@ModifyReturnValue(method = "getDrops", at = @At("RETURN"))
	private List<ItemStack> bettervanillabuilding$dropTopHead(List<ItemStack> drops, LootParams.Builder params) {
		BlockState state = (BlockState) (Object) this;
		if (!StackedHeads.isStacked(state)) {
			return drops;
		}
		List<ItemStack> withTop = new ArrayList<>(drops);
		withTop.addAll(StackedHeads.topDrops(state, params));
		return withTop;
	}

	/**
	 * The lily pad drops only if it's gone. An explosion, lost water or breaking the pad itself drops it too, but breaking just the
	 * decoration (the pad stays, {@code LevelMixin}) doesn't.
	 */
	@ModifyReturnValue(method = "getDrops", at = @At("RETURN"))
	private List<ItemStack> bettervanillabuilding$dropPad(List<ItemStack> drops, LootParams.Builder params) {
		if (!LilyPadDecorations.onPad((BlockState) (Object) this)
				|| params.getLevel().getBlockState(BlockPos.containing(params.getParameter(LootContextParams.ORIGIN))).is(Blocks.LILY_PAD)) {
			return drops;
		}
		List<ItemStack> withPad = new ArrayList<>(drops);
		withPad.addAll(Blocks.LILY_PAD.defaultBlockState().getDrops(params));
		return withPad;
	}

	/** Pistons pop a lily pad, so they pop anything on one too (it could never be pushed onto land). */
	@ModifyReturnValue(method = "getPistonPushReaction", at = @At("RETURN"))
	private PushReaction bettervanillabuilding$popLikePad(PushReaction reaction) {
		return LilyPadDecorations.onPad((BlockState) (Object) this) ? PushReaction.POPPED : reaction;
	}

	/** Mining the bare pad goes at the pad's speed (instant). Mining the decoration goes at its own speed and wants its own tool. */
	@ModifyReturnValue(method = "getDestroyProgress", at = @At("RETURN"))
	private float bettervanillabuilding$padMiningSpeed(float progress, Player player, BlockGetter level, BlockPos pos) {
		return LilyPadDecorations.aimsAtPad(player, level, pos, (BlockState) (Object) this)
				? Blocks.LILY_PAD.defaultBlockState().getDestroyProgress(player, level, pos)
				: progress;
	}
}
