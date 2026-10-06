package com.boaringpanda.vsbetterbuilding.block;

import org.jspecify.annotations.Nullable;

/**
 * Something that carries a mixed slab block's two slabs while it isn't a {@link MixedSlabBlockEntity}: vanilla's moving-piston block
 * entity during a push, and the client's render state for drawing it mid-push. Added to those classes by mixins.
 */
public interface MixedSlabCarrier {
	MixedSlabs.@Nullable Halves vsbetterbuilding$getHalves();

	void vsbetterbuilding$setHalves(MixedSlabs.@Nullable Halves halves);
}
