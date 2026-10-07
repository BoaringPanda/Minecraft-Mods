package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterqol.EnchantingLapis;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
	// A broken enchanting table drops the lapis stored in it, like a container drops its items (same vanilla hook).
	@Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
	private void vsbetterqol$dropLapis(BlockPos pos, BlockState state, CallbackInfo ci) {
		if ((Object) this instanceof EnchantingTableBlockEntity table && table.getLevel() != null) {
			Containers.dropItemStack(table.getLevel(), pos.getX(), pos.getY(), pos.getZ(), EnchantingLapis.take(table));
		}
	}

	// A brewing stand made from an item (a carried one, CarriedBrewing, or one set down) loads its brew time before its items, so vanilla
	// remembers "brewing nothing" and restarts the brew on the next tick. Once the items are in, remember the real ingredient.
	@Inject(method = "applyComponents", at = @At("TAIL"))
	private void vsbetterqol$keepBrewing(DataComponentMap components, DataComponentPatch patch, CallbackInfo ci) {
		if ((Object) this instanceof BrewingStandBlockEntity stand) {
			BrewingStandBlockEntityAccessor brewing = (BrewingStandBlockEntityAccessor) stand;
			if (brewing.vsbetterqol$getBrewTime() > 0) {
				brewing.vsbetterqol$setIngredient(stand.getItem(3).getItem());
			}
		}
	}
}
