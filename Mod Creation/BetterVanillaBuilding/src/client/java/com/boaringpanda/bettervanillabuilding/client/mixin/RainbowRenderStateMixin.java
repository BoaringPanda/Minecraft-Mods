package com.boaringpanda.bettervanillabuilding.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.entity.state.CushionRenderState;

import com.boaringpanda.bettervanillabuilding.client.RainbowRenderState;

/** Lets a banner's or cushion's render state say whether it fades ({@link RainbowRenderState}). */
@Mixin({BannerRenderState.class, CushionRenderState.class})
public class RainbowRenderStateMixin implements RainbowRenderState {
	@Unique
	private boolean bettervanillabuilding$rainbow;

	@Override
	public boolean bettervanillabuilding$isRainbow() {
		return bettervanillabuilding$rainbow;
	}

	@Override
	public void bettervanillabuilding$setRainbow(boolean rainbow) {
		bettervanillabuilding$rainbow = rainbow;
	}
}
