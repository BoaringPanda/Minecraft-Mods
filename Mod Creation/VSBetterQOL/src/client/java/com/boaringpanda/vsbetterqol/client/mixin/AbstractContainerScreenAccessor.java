package com.boaringpanda.vsbetterqol.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

// Where the GUI is on screen, for placing the sort buttons (SortButtons).
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor
	int getLeftPos();

	@Accessor
	int getTopPos();
}
