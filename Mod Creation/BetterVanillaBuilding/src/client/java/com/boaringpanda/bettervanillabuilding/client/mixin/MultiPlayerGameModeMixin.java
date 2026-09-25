package com.boaringpanda.bettervanillabuilding.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bettervanillabuilding.block.LilyPadDecorations;
import com.boaringpanda.bettervanillabuilding.block.StackedHeads;

/**
 * The client's own guess at a break matches the server's: breaking a decoration on a lily pad leaves the pad unless the player is
 * aiming at the bare pad ({@link LilyPadDecorations}). Otherwise the pad would vanish until the server put it back. Likewise breaking
 * one head of a stack leaves the other head where it is ({@link StackedHeads}).
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@WrapOperation(method = "destroyBlock",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private boolean bettervanillabuilding$leavePad(Level level, BlockPos pos, BlockState newState, int flags, Operation<Boolean> original) {
		BlockState old = level.getBlockState(pos);
		boolean pad = LilyPadDecorations.aimsAtPad(minecraft.player, level, pos, old);
		if (!pad && StackedHeads.isStacked(old)) {
			return StackedHeads.breakHead(level, pos, old, StackedHeads.aimsAtTop(minecraft.player, level, pos, old), flags) != null;
		}
		if (LilyPadDecorations.onPad(old) && !pad) {
			return original.call(level, pos, Blocks.LILY_PAD.defaultBlockState(), flags);
		}
		return original.call(level, pos, newState, flags);
	}
}
