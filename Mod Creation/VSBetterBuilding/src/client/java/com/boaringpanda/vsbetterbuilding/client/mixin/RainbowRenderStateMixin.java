package com.boaringpanda.vsbetterbuilding.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.entity.state.CushionRenderState;

import com.boaringpanda.vsbetterbuilding.client.RainbowRenderState;

/** Lets a banner's or cushion's render state say whether it fades ({@link RainbowRenderState}). */
@Mixin({BannerRenderState.class, CushionRenderState.class})
public class RainbowRenderStateMixin implements RainbowRenderState {
	@Unique
	private boolean vsbetterbuilding$rainbow;

	@Override
	public boolean vsbetterbuilding$isRainbow() {
		return vsbetterbuilding$rainbow;
	}

	@Override
	public void vsbetterbuilding$setRainbow(boolean rainbow) {
		vsbetterbuilding$rainbow = rainbow;
	}
}
