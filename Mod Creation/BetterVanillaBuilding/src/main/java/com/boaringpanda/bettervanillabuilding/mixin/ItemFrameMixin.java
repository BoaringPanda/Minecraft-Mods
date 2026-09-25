package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;

import com.boaringpanda.bettervanillabuilding.block.LilyPadDecorations;

/**
 * An item frame lying on a lily pad ({@code HangingEntityItemMixin} places it in the pad's space, facing up). Its box sits on the pad's
 * top instead of inside the pad, so it can be aimed at and punched; the renderer draws the frame wherever the box is. The pad holds it
 * up as a floor would, and when the pad goes, vanilla's own check (the water below isn't solid) pops it.
 */
@Mixin(ItemFrame.class)
public class ItemFrameMixin {
	@ModifyReturnValue(method = "createBoundingBox", at = @At("RETURN"))
	private AABB bettervanillabuilding$onPadTop(AABB box, BlockPos pos, Direction direction) {
		return LilyPadDecorations.frameOnPad(((ItemFrame) (Object) this).level(), pos, direction) ? LilyPadDecorations.onPadTop(box) : box;
	}

	@ModifyExpressionValue(method = "survives",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolid()Z"))
	private boolean bettervanillabuilding$padHoldsFrame(boolean solid) {
		ItemFrame frame = (ItemFrame) (Object) this;
		return solid || LilyPadDecorations.frameOnPad(frame.level(), frame.getPos(), frame.getDirection());
	}
}
