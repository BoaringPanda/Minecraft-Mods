package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.boaringpanda.vsbetterqol.CropHarvesting;

// Right-click a grown crop to harvest and replant it. Crops don't override useWithoutItem, so they all land here. Vanilla only calls
// it for the main hand and not while sneaking with an item, and before the held item's own use (same as picking sweet berries).
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void vsbetterqol$harvestCrops(
			BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
		if (CropHarvesting.harvest(state, level, pos, player)) {
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
