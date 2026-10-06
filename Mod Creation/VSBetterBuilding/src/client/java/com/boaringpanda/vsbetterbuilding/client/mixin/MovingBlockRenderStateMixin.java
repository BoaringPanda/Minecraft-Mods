package com.boaringpanda.vsbetterbuilding.client.mixin;

import org.jspecify.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;

import com.boaringpanda.vsbetterbuilding.block.MixedSlabCarrier;
import com.boaringpanda.vsbetterbuilding.block.MixedSlabs;

/**
 * The render state of a block being pushed stands in for the world while it's drawn. For a mixed slab block it carries the two slabs
 * ({@code PistonHeadRendererMixin}) and answers the model's {@code getBlockEntityRenderData} with them, as the real block entity would.
 */
@Mixin(MovingBlockRenderState.class)
public class MovingBlockRenderStateMixin implements MixedSlabCarrier {
	@Shadow
	public BlockPos blockPos;

	@Unique
	private MixedSlabs.@Nullable Halves vsbetterbuilding$halves;

	@Override
	public MixedSlabs.@Nullable Halves vsbetterbuilding$getHalves() {
		return vsbetterbuilding$halves;
	}

	@Override
	public void vsbetterbuilding$setHalves(MixedSlabs.@Nullable Halves halves) {
		vsbetterbuilding$halves = halves;
	}

	/** Overrides Fabric's {@code FabricBlockGetter.getBlockEntityRenderData}, which returns nothing here by default. */
	public @Nullable Object getBlockEntityRenderData(BlockPos pos) {
		return pos.equals(blockPos) ? vsbetterbuilding$halves : null;
	}
}
