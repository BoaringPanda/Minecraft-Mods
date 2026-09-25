package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import com.boaringpanda.bettervanillabuilding.block.LilyPadDecorations;
import com.boaringpanda.bettervanillabuilding.block.LockedBlocks;
import com.boaringpanda.bettervanillabuilding.block.StackedHeads;

/**
 * Gives every block that can stand on a lily pad the {@code lily_pad} property ({@link LilyPadDecorations}), off by default, every
 * standing head the {@code top_head} property ({@link StackedHeads}), and fences, panes, bars, walls and rails the {@code locked}
 * property ({@link LockedBlocks}), off by default.
 */
@Mixin(Block.class)
public class BlockMixin {
	@WrapOperation(
			method = "<init>",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/Block;createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V"))
	private void bettervanillabuilding$addProperties(Block block, StateDefinition.Builder<Block, BlockState> builder, Operation<Void> original) {
		original.call(block, builder);
		if (LilyPadDecorations.canSitOnPad(block)) {
			builder.add(LilyPadDecorations.LILY_PAD);
		}
		// Standing heads only: player heads and wither skulls are SkullBlocks too, wall heads aren't.
		if (block instanceof SkullBlock) {
			builder.add(StackedHeads.TOP, StackedHeads.RAISED);
		}
		if (LockedBlocks.canLock(block)) {
			builder.add(LockedBlocks.LOCKED);
		}
	}

	/**
	 * A boolean property's first value is true, and blocks build their default state from {@code stateDefinition.any()} (the first
	 * value of every property they don't set), so without this every torch would default to standing on a pad, every head to
	 * floating half a block up, and every fence to being locked.
	 */
	@ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
	private BlockState bettervanillabuilding$offByDefault(BlockState state) {
		if (state.hasProperty(StackedHeads.RAISED)) {
			state = state.setValue(StackedHeads.RAISED, false);
		}
		if (state.hasProperty(LockedBlocks.LOCKED)) {
			state = state.setValue(LockedBlocks.LOCKED, false);
		}
		return state.hasProperty(LilyPadDecorations.LILY_PAD) ? state.setValue(LilyPadDecorations.LILY_PAD, false) : state;
	}

	/** Breaking the bare pad of a decoration shows the pad's break particles and sound. */
	@Inject(method = "spawnDestroyByEntityParticles", at = @At("HEAD"), cancellable = true)
	private void bettervanillabuilding$padBreakParticles(Level level, @Nullable Entity entity, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (entity instanceof Player player && LilyPadDecorations.aimsAtPad(player, level, pos, state)) {
			level.levelEvent(entity, 2001, pos, Block.getId(Blocks.LILY_PAD.defaultBlockState()));
			ci.cancel();
		}
	}
}
