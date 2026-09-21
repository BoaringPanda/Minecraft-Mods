package com.boaringpanda.bpsbettervanillabuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bpsbettervanillabuilding.block.StackedFlowers;

/**
 * Lets a small flower be added to by another of the same flower, so it can become a stack (see {@code StackedFlowerBlock}).
 * <p>
 * Vanilla asks the block already in the space being placed into "can the held item replace you?" before it places
 * anything ({@code BlockPlaceContext} calls {@code canBeReplaced}, checked by disassembly), and a plain flower never says yes to
 * its own item. This says yes, not when sneaking, only for the flowers in {@link StackedFlowers}. What block then results is
 * decided by {@link BlockItemMixin}. It is a {@code BlockBehaviour} injection because flowers don't declare the method
 * themselves; the check that runs first is only "is the held item this block's own item", so it costs almost nothing for every
 * other block.
 * <p>
 * Common code, since the client predicts the placement with the same rules the server uses.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
	@Inject(
			method = "canBeReplaced(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/context/BlockPlaceContext;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private void bpsbettervanillabuilding$flowerCanBeAddedTo(BlockState state, BlockPlaceContext context, CallbackInfoReturnable<Boolean> cir) {
		if (context.isSecondaryUseActive()) {
			return;
		}

		if (context.getItemInHand().getItem() instanceof BlockItem held && held.getBlock() == (Object) this && StackedFlowers.of(held.getBlock()) != null) {
			cir.setReturnValue(true);
		}
	}
}
