package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.boaringpanda.vsbetterqol.StonecutterStorage;

// One player at a time per stonecutter (Dylan's pick, so its storage can't be shared or duplicated): anyone else gets a message instead.
@Mixin(StonecutterBlock.class)
public abstract class StonecutterBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void vsbetterqol$oneAtATime(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (!level.isClientSide() && StonecutterStorage.inUse(level, pos, player)) {
			StonecutterStorage.sendInUse(player);
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
