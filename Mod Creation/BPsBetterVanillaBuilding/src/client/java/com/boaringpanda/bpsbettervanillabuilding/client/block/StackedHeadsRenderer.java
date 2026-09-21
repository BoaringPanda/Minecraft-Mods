package com.boaringpanda.bpsbettervanillabuilding.client.block;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;

import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlockEntity;

/**
 * Draws the two heads of a stacked pair with vanilla's own {@link SkullBlockRenderer}, twice, so a head looks exactly
 * like a placed one: the same models, player skins, and dragon and piglin animation. Nothing about a head is
 * reimplemented here.
 * <p>
 * That works because the renderer only reads what a plain skull block entity has: its head type and rotation (from
 * its block state), its skin and its animation. Each of the two heads is a real {@code SkullBlockEntity} kept by the
 * {@link StackedHeadsBlockEntity}, at its own position and level so it is lit like any other. The top head is drawn
 * half a block up, the height of the head under it.
 */
public class StackedHeadsRenderer implements BlockEntityRenderer<StackedHeadsBlockEntity, StackedHeadsRenderer.State> {
	private final SkullBlockRenderer skullRenderer;

	public StackedHeadsRenderer(BlockEntityRendererProvider.Context context) {
		this.skullRenderer = new SkullBlockRenderer(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(StackedHeadsBlockEntity blockEntity, State state, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, crumbling);

		// Either half can be empty (the other head was broken), and an empty half is simply not drawn.
		SkullBlockEntity bottom = blockEntity.head(false);
		SkullBlockEntity top = blockEntity.head(true);
		state.hasBottom = bottom != null;
		state.hasTop = top != null;
		if (bottom != null) {
			this.skullRenderer.extractRenderState(bottom, state.bottom, partialTick, cameraPosition, crumbling);
		}
		if (top != null) {
			this.skullRenderer.extractRenderState(top, state.top, partialTick, cameraPosition, crumbling);
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.hasBottom) {
			this.skullRenderer.submit(state.bottom, poseStack, collector, camera);
		}

		if (state.hasTop) {
			poseStack.pushPose();
			poseStack.translate(0.0, 0.5, 0.0);
			this.skullRenderer.submit(state.top, poseStack, collector, camera);
			poseStack.popPose();
		}
	}

	/** The pair's own render state, holding one of vanilla's skull render states per head. */
	public static class State extends BlockEntityRenderState {
		final SkullBlockRenderState bottom = new SkullBlockRenderState();
		final SkullBlockRenderState top = new SkullBlockRenderState();
		boolean hasBottom;
		boolean hasTop;
	}
}
