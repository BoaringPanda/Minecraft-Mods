package com.boaringpanda.bettervanillabuilding.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

import net.minecraft.client.renderer.entity.EntityRenderers;

import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;
import com.boaringpanda.bettervanillabuilding.client.entity.RopeKnotRenderer;
import com.boaringpanda.bettervanillabuilding.client.model.LilyPadDecorationModel;
import com.boaringpanda.bettervanillabuilding.client.model.MixedSlabModel;
import com.boaringpanda.bettervanillabuilding.entity.RopeKnots;

public class BetterVanillaBuildingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// A rope knot is a fence knot: its own renderer draws vanilla's knot model, and draws the rope itself (vanilla's is a
		// straight line at the wrong height, so the mod draws a hanging one).
		EntityRenderers.register(RopeKnots.ROPE_KNOT, RopeKnotRenderer::new);

		// A mixed slab block's model is made in code (it draws the two vanilla slab models), so it has no blockstate file.
		// A decoration on a lily pad uses its own vanilla model with the lily pad's drawn under it.
		ModelLoadingPlugin.register(context -> {
			context.registerBlockStateResolver(MixedSlabs.MIXED_SLAB,
					resolver -> resolver.setModel(MixedSlabs.MIXED_SLAB.defaultBlockState(), new MixedSlabModel.Unbaked()));
			context.modifyBlockModelAfterBake().register((model, modelContext) -> LilyPadDecorationModel.wrapIfOnPad(model, modelContext.state()));
		});
	}
}
