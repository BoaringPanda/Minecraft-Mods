package com.boaringpanda.bpsbettervanillabuilding.client;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import net.minecraft.client.renderer.blockentity.state.StandingSignRenderState;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadSignBlockEntities;
import com.boaringpanda.bpsbettervanillabuilding.block.StackedHeads;
import com.boaringpanda.bpsbettervanillabuilding.client.block.LilyPadAccessoryColors;
import com.boaringpanda.bpsbettervanillabuilding.client.block.LilyPadBreakPrediction;
import com.boaringpanda.bpsbettervanillabuilding.client.block.StackedHeadsBreakPrediction;
import com.boaringpanda.bpsbettervanillabuilding.client.block.StackedHeadsRenderer;
import com.boaringpanda.bpsbettervanillabuilding.client.entity.RopeKnotRenderer;
import com.boaringpanda.bpsbettervanillabuilding.entity.RopeKnots;

public class BPsBetterVanillaBuildingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		LilyPadAccessoryColors.initialize();
		LilyPadBreakPrediction.initialize();

		// Reuses vanilla's own sign renderer directly - it only draws the dynamic text overlay
		// (SubmitNodeCollector.submitText), never the post/board mesh, which is 100% the ordinary
		// baked block model - so there's nothing lily-pad-specific for a custom renderer to do.
		BlockEntityRendererProvider<SignBlockEntity, StandingSignRenderState> signRenderer = StandingSignRenderer::new;
		BlockEntityRendererRegistry.register(LilyPadSignBlockEntities.LILY_PAD_SIGN, signRenderer);

		// Two heads in one block: vanilla's own skull renderer, run once per head.
		BlockEntityRendererRegistry.register(StackedHeads.BLOCK_ENTITY, StackedHeadsRenderer::new);
		StackedHeadsBreakPrediction.initialize();

		// A rope knot is a fence knot: its own renderer draws vanilla's knot model, and draws the rope itself (vanilla's is a
		// straight line at the wrong height, so the mod draws a hanging one).
		EntityRendererRegistry.register(RopeKnots.ROPE_KNOT, RopeKnotRenderer::new);
	}
}