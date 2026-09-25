package com.boaringpanda.bettervanillabuilding.block;

import org.jspecify.annotations.Nullable;

/** A head's block entity, which also holds the head stacked on top of it ({@link StackedHeads}). Added by {@code SkullBlockEntityMixin}. */
public interface TopHeadHolder {
	StackedHeads.@Nullable Head bettervanillabuilding$getTopHead();

	void bettervanillabuilding$setTopHead(StackedHeads.@Nullable Head top);
}
