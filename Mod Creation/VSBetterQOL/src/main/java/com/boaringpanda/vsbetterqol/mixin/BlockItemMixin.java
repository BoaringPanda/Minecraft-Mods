package com.boaringpanda.vsbetterqol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterqol.PlacedLogs;
import com.boaringpanda.vsbetterqol.StonecutterStorage;

// Remembers every log placed from an item, so it doesn't hold up natural leaves (see PlacedLogs), and puts a carried stonecutter's
// storage back.
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	// setPlacedBy only runs once the block has really been placed.
	@WrapOperation(
			method = "place",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/Block;setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"))
	private void vsbetterqol$rememberPlacedLog(
			Block block, Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack itemStack, Operation<Void> original) {
		original.call(block, level, pos, state, by, itemStack);
		if (state.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY)) {
			PlacedLogs.remember(level, pos);
		}
		// A carried stonecutter (Carrying) brings its storage along in the item.
		ItemContainerContents storage = itemStack.get(DataComponents.CONTAINER);
		if (state.is(Blocks.STONECUTTER) && storage != null) {
			StonecutterStorage.store(level, pos, storage);
		}
	}
}
