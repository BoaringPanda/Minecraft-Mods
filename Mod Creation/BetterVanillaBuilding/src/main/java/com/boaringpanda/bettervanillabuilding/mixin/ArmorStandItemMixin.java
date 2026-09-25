package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

/**
 * An armour stand aimed into a lily pad's space (clicking the side of a block next to the pad) stands on the pad. Vanilla would try to
 * fit it inside the pad and fail. It's placed in the space above instead, and vanilla's spawn already lowers it onto the pad, as when
 * the pad's top is clicked.
 */
@Mixin(ArmorStandItem.class)
public class ArmorStandItemMixin {
	@ModifyExpressionValue(method = "useOn",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/context/BlockPlaceContext;getClickedPos()Lnet/minecraft/core/BlockPos;"))
	private BlockPos bettervanillabuilding$abovePad(BlockPos pos, UseOnContext context) {
		return context.getLevel().getBlockState(pos).is(Blocks.LILY_PAD) ? pos.above() : pos;
	}
}
