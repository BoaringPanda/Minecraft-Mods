package com.boaringpanda.vsbetterqol.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;

import com.boaringpanda.vsbetterqol.Carrying;

// The scroll wheel doesn't change hotbar slot while carrying a container (the number keys are blocked in VSBetterQOLClient).
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@WrapWithCondition(
			method = "onScroll",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
	private boolean vsbetterqol$hotbarLockedWhileCarrying(Inventory inventory, int slot) {
		return !Carrying.isCarrying(inventory.player);
	}
}
