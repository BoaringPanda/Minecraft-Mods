package com.boaringpanda.vsbetterbuilding.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;

import com.boaringpanda.vsbetterbuilding.client.blockentity.TopHeadRenderState;

/** Lets a head's render state carry the head stacked on top of it ({@link TopHeadRenderState}). */
@Mixin(SkullBlockRenderState.class)
public class SkullBlockRenderStateMixin implements TopHeadRenderState {
	@Unique
	private TopHeadRenderState.@Nullable TopHead vsbetterbuilding$topHead;

	@Override
	public TopHeadRenderState.@Nullable TopHead vsbetterbuilding$getTopHead() {
		return vsbetterbuilding$topHead;
	}

	@Override
	public void vsbetterbuilding$setTopHead(TopHeadRenderState.@Nullable TopHead top) {
		vsbetterbuilding$topHead = top;
	}
}
