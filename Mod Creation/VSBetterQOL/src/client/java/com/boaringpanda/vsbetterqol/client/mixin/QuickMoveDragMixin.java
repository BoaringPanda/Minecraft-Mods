package com.boaringpanda.vsbetterqol.client.mixin;

import com.boaringpanda.vsbetterqol.client.ClientConfig;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

// Holding shift while dragging the left mouse across slots quick-moves each one, the same as shift-clicking them one by one. A slot
// moves each time the cursor enters it, so dragging back over the slots the items went to moves them back. The creative item list is
// skipped; everywhere else in creative it does what shift-click does there (in item tabs that clears hotbar slots).
@Mixin(AbstractContainerScreen.class)
public abstract class QuickMoveDragMixin {
	// Small enough that a fast swipe can't jump over a 16px slot between two drag events.
	@Unique
	private static final double STEP = 4.0;

	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	// The slot the cursor was last over during this drag, so staying inside one slot doesn't move it again.
	@Unique
	private @Nullable Slot vsbetterqol$lastSlot;

	@Shadow
	private @Nullable Slot getHoveredSlot(double x, double y) {
		throw new AssertionError();
	}

	@Shadow
	protected abstract void slotClicked(Slot slot, int slotId, int button, ContainerInput input);

	// A new press starts a new drag. Vanilla's own shift-click moves the pressed slot.
	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void vsbetterqol$startDrag(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		this.vsbetterqol$lastSlot = this.getHoveredSlot(event.x(), event.y());
	}

	// Drag events keep the press's modifiers, so shift is checked live. With an item on the cursor vanilla's drag-split runs instead.
	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void vsbetterqol$quickMoveDragged(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
		if (!ClientConfig.SHIFT_DRAG.on || event.button() != InputConstants.MOUSE_BUTTON_LEFT || !Minecraft.getInstance().hasShiftDown() || !this.menu.getCarried().isEmpty()) {
			return;
		}
		// Every slot between the last drag event and this one.
		int steps = Math.max(Mth.ceil(Math.sqrt(dragX * dragX + dragY * dragY) / STEP), 1);
		for (int i = 0; i <= steps; i++) {
			double back = 1.0 - (double) i / steps;
			Slot slot = this.getHoveredSlot(event.x() - dragX * back, event.y() - dragY * back);
			if (slot == this.vsbetterqol$lastSlot) {
				continue;
			}
			this.vsbetterqol$lastSlot = slot;
			if (slot != null && slot.hasItem() && !this.vsbetterqol$isCreativeList(slot)) {
				this.slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
			}
		}
		cir.setReturnValue(true);
	}

	@Unique
	private boolean vsbetterqol$isCreativeList(Slot slot) {
		return (Object) this instanceof CreativeModeInventoryScreen creative && ((CreativeModeInventoryScreenInvoker) creative).invokeIsCreativeSlot(slot);
	}
}
