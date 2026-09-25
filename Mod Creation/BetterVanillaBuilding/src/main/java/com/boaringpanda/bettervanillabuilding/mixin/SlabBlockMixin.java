package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;

/**
 * Lets a slab stack on a different slab. Vanilla only merges a slab with the same slab; this widens its own two checks, so the rest of
 * vanilla's rules (which face and which half you click, the block beside it, sneaking) still decide when stacking happens.
 */
@Mixin(SlabBlock.class)
public class SlabBlockMixin {
	/**
	 * {@code canBeReplaced} asks "is the item in hand this slab?"; any slab counts. ({@code ItemStack.is(Item)} is the generic
	 * {@code TypedInstance.is(T)}, so in bytecode it takes an {@code Object}.)
	 */
	@WrapOperation(
			method = "canBeReplaced(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/context/BlockPlaceContext;)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
	private boolean bettervanillabuilding$anySlabStacks(ItemStack stack, Object item, Operation<Boolean> original) {
		return original.call(stack, item) || MixedSlabs.isSlabItem(stack);
	}

	/** Placing into a different half slab makes a mixed slab block; the same slab still makes vanilla's double slab. */
	@Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
	private void bettervanillabuilding$placeMixed(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		BlockState replaced = context.getLevel().getBlockState(context.getClickedPos());
		if (replaced.getBlock() instanceof SlabBlock && !replaced.is((SlabBlock) (Object) this)
				&& replaced.getValue(SlabBlock.TYPE) != SlabType.DOUBLE) {
			cir.setReturnValue(MixedSlabs.MIXED_SLAB.defaultBlockState());
		}
	}
}
