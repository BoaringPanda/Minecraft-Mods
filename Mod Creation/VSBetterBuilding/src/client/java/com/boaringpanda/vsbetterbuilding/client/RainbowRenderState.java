package com.boaringpanda.vsbetterbuilding.client;

/**
 * A banner's or cushion's render state, which also says whether it fades like a jeb_ sheep ({@code Rainbow}, {@code RainbowCushions}).
 * Added to vanilla's {@code BannerRenderState} and {@code CushionRenderState} by {@code RainbowRenderStateMixin}, and filled and drawn by
 * {@code BannerRendererMixin} and {@code CushionRendererMixin}.
 */
public interface RainbowRenderState {
	boolean vsbetterbuilding$isRainbow();

	void vsbetterbuilding$setRainbow(boolean rainbow);
}
