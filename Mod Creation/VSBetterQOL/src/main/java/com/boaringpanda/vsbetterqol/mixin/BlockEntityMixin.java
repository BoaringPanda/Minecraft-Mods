package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterqol.EnchantingLapis;

// A broken enchanting table drops the lapis stored in it, like a container drops its items (same vanilla hook).
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
	@Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
	private void vsbetterqol$dropLapis(BlockPos pos, BlockState state, CallbackInfo ci) {
		if ((Object) this instanceof EnchantingTableBlockEntity table && table.getLevel() != null) {
			Containers.dropItemStack(table.getLevel(), pos.getX(), pos.getY(), pos.getZ(), EnchantingLapis.take(table));
		}
	}
}
