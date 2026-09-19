package com.boaringpanda.extrablocks.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets {@link com.boaringpanda.extrablocks.block.LilyPadAccessories} count more block states as an existing
 * vanilla point of interest. Lightning finds rods by searching for the {@code minecraft:lightning_rod} POI,
 * and vanilla's private {@code registerBlockStates} is what decides which states are one. Fabric API's
 * {@code PoiHelper} can only create new POI types, not add to a vanilla one.
 */
@Mixin(PoiTypes.class)
public interface PoiTypesInvoker {
	@Invoker("registerBlockStates")
	static void invokeRegisterBlockStates(Holder<PoiType> type, Set<BlockState> states) {
		throw new AssertionError();
	}
}
