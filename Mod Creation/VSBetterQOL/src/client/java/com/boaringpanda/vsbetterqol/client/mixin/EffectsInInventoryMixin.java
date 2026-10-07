package com.boaringpanda.vsbetterqol.client.mixin;

import com.boaringpanda.vsbetterqol.client.ClientConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;

// No effect list beside the survival and creative inventories (Dylan finds it messy). The HUD column (EffectHudMixin) shows instead:
// canSeeEffects is what the inventory screens' showsActiveEffects returns, which vanilla's HUD uses to hide its effects. With the
// effect column switched off (ClientConfig) the list is vanilla again.
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {
	@Inject(method = "canSeeEffects", at = @At("HEAD"), cancellable = true)
	private void vsbetterqol$neverSeeEffects(CallbackInfoReturnable<Boolean> cir) {
		if (ClientConfig.EFFECT_COLUMN.on) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void vsbetterqol$noEffectList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (ClientConfig.EFFECT_COLUMN.on) {
			ci.cancel();
		}
	}
}
