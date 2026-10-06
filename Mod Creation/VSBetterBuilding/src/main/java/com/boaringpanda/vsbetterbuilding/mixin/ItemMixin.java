package com.boaringpanda.vsbetterbuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import com.boaringpanda.vsbetterbuilding.block.PlacedRods;

/**
 * Sticks, blaze rods and breeze rods place their rod block when used on a block, as a block item would. {@code useOn} is where a block
 * item places too, after the clicked block has had its own say, so chests, doors and sneaking all behave as they do for any block.
 */
@Mixin(Item.class)
public class ItemMixin {
	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$placeRod(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		Block rod = PlacedRods.blockFor((Item) (Object) this);
		if (rod != null) {
			cir.setReturnValue(PlacedRods.place(new BlockPlaceContext(context), rod));
		}
	}
}
