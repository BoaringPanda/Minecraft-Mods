package com.boaringpanda.vsbetterbuilding.client.blockentity;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.SkullBlock;

/**
 * A head's render state, which also carries the head stacked on top of it ({@code StackedHeads}). Added to vanilla's
 * {@code SkullBlockRenderState} by {@code SkullBlockRenderStateMixin} and filled and drawn by {@code SkullBlockRendererMixin}.
 */
public interface TopHeadRenderState {
	/** What's needed to draw the top head: which head, its skin, which way it faces, and its mining cracks (if it's being mined). */
	record TopHead(SkullBlock.Type type, RenderType renderType, int rotation, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
	}

	@Nullable TopHead vsbetterbuilding$getTopHead();

	void vsbetterbuilding$setTopHead(@Nullable TopHead top);
}
