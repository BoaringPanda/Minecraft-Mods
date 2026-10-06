package com.boaringpanda.vsbetterbuilding.block;

import org.jspecify.annotations.Nullable;

/** A head's block entity, which also holds the head stacked on top of it ({@link StackedHeads}). Added by {@code SkullBlockEntityMixin}. */
public interface TopHeadHolder {
	StackedHeads.@Nullable Head vsbetterbuilding$getTopHead();

	void vsbetterbuilding$setTopHead(StackedHeads.@Nullable Head top);
}
