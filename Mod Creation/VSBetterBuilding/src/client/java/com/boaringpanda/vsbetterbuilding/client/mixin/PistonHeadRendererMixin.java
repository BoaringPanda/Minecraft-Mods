package com.boaringpanda.vsbetterbuilding.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterbuilding.block.MixedSlabCarrier;
import com.boaringpanda.vsbetterbuilding.block.MixedSlabs;

/**
 * A mixed slab block being pushed is drawn from a render state, not the world, so its two slabs are handed from the moving block to
 * that render state ({@code MovingBlockRenderStateMixin}), where the model finds them.
 */
@Mixin(PistonHeadRenderer.class)
public class PistonHeadRendererMixin {
	@WrapOperation(
			method = "extractRenderState",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/blockentity/PistonHeadRenderer;createMovingBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Holder;Lnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/block/MovingBlockRenderState;"))
	private MovingBlockRenderState vsbetterbuilding$drawMovingSlabs(BlockPos pos, BlockState state, Holder<Biome> biome, ClientLevel level,
			Operation<MovingBlockRenderState> original, @Local(argsOnly = true) PistonMovingBlockEntity blockEntity) {
		MovingBlockRenderState renderState = original.call(pos, state, biome, level);
		if (MixedSlabs.is(state)) {
			((MixedSlabCarrier) renderState).vsbetterbuilding$setHalves(((MixedSlabCarrier) blockEntity).vsbetterbuilding$getHalves());
		}
		return renderState;
	}
}
