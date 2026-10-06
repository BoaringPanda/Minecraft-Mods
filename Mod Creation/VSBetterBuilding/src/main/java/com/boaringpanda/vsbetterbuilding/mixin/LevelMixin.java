package com.boaringpanda.vsbetterbuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.boaringpanda.vsbetterbuilding.block.LilyPadDecorations;

/**
 * Removing a decoration from a lily pad leaves the pad ({@link LilyPadDecorations}). This is the removal vanilla uses when a player
 * breaks a block (survival and creative) and when a turtle egg is trampled. Breaking the pad itself goes round this
 * ({@code ServerPlayerGameModeMixin}).
 */
@Mixin(Level.class)
public class LevelMixin {
	@Inject(method = "removeBlock", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$leavePad(BlockPos pos, boolean movedByPiston, CallbackInfoReturnable<Boolean> cir) {
		Level level = (Level) (Object) this;
		if (!movedByPiston && LilyPadDecorations.onPad(level.getBlockState(pos))) {
			cir.setReturnValue(level.setBlock(pos, Blocks.LILY_PAD.defaultBlockState(), Block.UPDATE_ALL));
		}
	}
}
