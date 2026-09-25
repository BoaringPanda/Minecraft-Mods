package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.ItemFrameItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

/**
 * An item frame (or glow item frame) clicked onto a lily pad, from any side, lies flat on the pad in the pad's own space, as if set on
 * a floor there. Vanilla would try the space beside the clicked face, where nothing holds it up. {@code ItemFrameMixin} raises it onto
 * the pad's top and lets the pad hold it.
 */
@Mixin(HangingEntityItem.class)
public class HangingEntityItemMixin {
	@ModifyExpressionValue(method = "useOn",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/context/UseOnContext;getClickedFace()Lnet/minecraft/core/Direction;"))
	private Direction bettervanillabuilding$faceUpOnPad(Direction face, UseOnContext context) {
		return framesPad(context) ? Direction.UP : face;
	}

	@ModifyExpressionValue(method = "useOn",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;"))
	private BlockPos bettervanillabuilding$inPadSpace(BlockPos pos, UseOnContext context) {
		return framesPad(context) ? context.getClickedPos() : pos;
	}

	private boolean framesPad(UseOnContext context) {
		return (Object) this instanceof ItemFrameItem && context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.LILY_PAD);
	}
}
