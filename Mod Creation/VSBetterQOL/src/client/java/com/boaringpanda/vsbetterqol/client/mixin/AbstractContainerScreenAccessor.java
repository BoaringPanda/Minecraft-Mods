package com.boaringpanda.vsbetterqol.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

// imageHeight is final in vanilla; the stonecutter screen grows to fit its storage row (StonecutterScreenMixin).
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Mutable
	@Accessor("imageHeight")
	void vsbetterqol$setImageHeight(int imageHeight);
}
