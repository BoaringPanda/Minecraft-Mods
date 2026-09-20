package com.boaringpanda.bpsbettervanillabuilding.client;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import net.minecraft.client.renderer.blockentity.state.StandingSignRenderState;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadSignBlockEntities;
import com.boaringpanda.bpsbettervanillabuilding.client.block.LilyPadAccessoryColors;
import com.boaringpanda.bpsbettervanillabuilding.client.block.LilyPadBreakPrediction;

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
	}
}