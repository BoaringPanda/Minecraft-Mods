package com.boaringpanda.vsbetterqol.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;

// Whether a slot is in the creative item list, so shift-dragging can skip it (QuickMoveDragMixin).
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenInvoker {
	@Invoker
	boolean invokeIsCreativeSlot(Slot slot);
}
