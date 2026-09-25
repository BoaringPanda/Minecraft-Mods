package com.boaringpanda.bettervanillabuilding.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;

import com.boaringpanda.bettervanillabuilding.client.blockentity.TopHeadRenderState;

/** Lets a head's render state carry the head stacked on top of it ({@link TopHeadRenderState}). */
@Mixin(SkullBlockRenderState.class)
public class SkullBlockRenderStateMixin implements TopHeadRenderState {
	@Unique
	private TopHeadRenderState.@Nullable TopHead bettervanillabuilding$topHead;

	@Override
	public TopHeadRenderState.@Nullable TopHead bettervanillabuilding$getTopHead() {
		return bettervanillabuilding$topHead;
	}

	@Override
	public void bettervanillabuilding$setTopHead(TopHeadRenderState.@Nullable TopHead top) {
		bettervanillabuilding$topHead = top;
	}
}
