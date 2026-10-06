package com.boaringpanda.vslumberjackmod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vslumberjackmod.PlacedTreeParts;

// Remembers every tree part placed from an item, so Tree Falling leaves it alone (see PlacedTreeParts).
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	// setPlacedBy only runs once the block has really been placed.
	@WrapOperation(
			method = "place",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/Block;setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"))
	private void vslumberjackmod$rememberPlacedTreePart(
			Block block, Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack itemStack, Operation<Void> original) {
		original.call(block, level, pos, state, by, itemStack);
		if (state.is(PlacedTreeParts.TREE_PARTS)) {
			PlacedTreeParts.remember(level, pos);
		}
	}
}
