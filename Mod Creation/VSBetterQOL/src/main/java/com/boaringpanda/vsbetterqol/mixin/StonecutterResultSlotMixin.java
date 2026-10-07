package com.boaringpanda.vsbetterqol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;

import com.boaringpanda.vsbetterqol.StonecutterMenuStorage;

// The stonecutter's result slot (an unnamed class in vanilla). Taking a result removes one block from the input; while that happens the
// menu is told it's crafting, so an emptied input refills from storage (StonecutterMenuMixin). Covers clicking and shift-clicking.
@Mixin(targets = "net.minecraft.world.inventory.StonecutterMenu$2")
public abstract class StonecutterResultSlotMixin {
	@Shadow
	@Final
	StonecutterMenu this$0;

	@WrapOperation(
			method = "onTake",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;remove(I)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack vsbetterqol$craftingFromInput(Slot inputSlot, int amount, Operation<ItemStack> original) {
		StonecutterMenuStorage menu = (StonecutterMenuStorage) this.this$0;
		menu.vsbetterqol$setCrafting(true);
		try {
			return original.call(inputSlot, amount);
		} finally {
			menu.vsbetterqol$setCrafting(false);
		}
	}
}
