package com.boaringpanda.bettervanillabuilding.client.mixin;

import org.jspecify.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;

import com.boaringpanda.bettervanillabuilding.block.MixedSlabCarrier;
import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;

/**
 * The render state of a block being pushed stands in for the world while it's drawn. For a mixed slab block it carries the two slabs
 * ({@code PistonHeadRendererMixin}) and answers the model's {@code getBlockEntityRenderData} with them, as the real block entity would.
 */
@Mixin(MovingBlockRenderState.class)
public class MovingBlockRenderStateMixin implements MixedSlabCarrier {
	@Shadow
	public BlockPos blockPos;

	@Unique
	private MixedSlabs.@Nullable Halves bettervanillabuilding$halves;

	@Override
	public MixedSlabs.@Nullable Halves bettervanillabuilding$getHalves() {
		return bettervanillabuilding$halves;
	}

	@Override
	public void bettervanillabuilding$setHalves(MixedSlabs.@Nullable Halves halves) {
		bettervanillabuilding$halves = halves;
	}

	/** Overrides Fabric's {@code FabricBlockGetter.getBlockEntityRenderData}, which returns nothing here by default. */
	public @Nullable Object getBlockEntityRenderData(BlockPos pos) {
		return pos.equals(blockPos) ? bettervanillabuilding$halves : null;
	}
}
