package com.boaringpanda.vsbetterqol;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;

// Added to vanilla's StonecutterMenu by StonecutterMenuMixin, so the result slot (StonecutterResultSlotMixin) and StonecutterStorage
// can talk to it.
public interface StonecutterMenuStorage {
	// The stonecutter this menu is for, on the server (null on the client).
	@Nullable BlockPos vsbetterqol$pos();

	// True while the result slot is taking the cost out of the input, so the input refills from storage only for crafting.
	void vsbetterqol$setCrafting(boolean crafting);
}
