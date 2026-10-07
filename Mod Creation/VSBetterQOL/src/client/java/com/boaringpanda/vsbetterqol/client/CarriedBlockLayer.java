package com.boaringpanda.vsbetterqol.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

// Draws the container a player is carrying (Carrying) in front of their chest, held in both arms (client/mixin/HumanoidModelMixin poses
// the arms). The item is filled in each frame by client/mixin/AvatarRendererMixin.
public class CarriedBlockLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public static final RenderStateDataKey<ItemStackRenderState> CARRIED = RenderStateDataKey.create(() -> "vsbetterqol:carried");

	// Block size (1 = a full block) and where its centre sits from the top of the body, in blocks: down, and forwards.
	private static final float SIZE = 0.6F;
	private static final float DOWN = 0.45F;
	private static final float FORWARD = 0.45F;

	public CarriedBlockLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	// The render state's carried item, created the first time it's needed. Empty when nothing is carried.
	public static ItemStackRenderState carried(EntityRenderState state) {
		FabricRenderState data = (FabricRenderState) state;
		ItemStackRenderState carried = data.getData(CARRIED);
		if (carried == null) {
			carried = new ItemStackRenderState();
			data.setData(CARRIED, carried);
		}
		return carried;
	}

	public static boolean isCarrying(EntityRenderState state) {
		ItemStackRenderState carried = ((FabricRenderState) state).getData(CARRIED);
		return carried != null && !carried.isEmpty();
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot,
			float xRot) {
		if (!isCarrying(state)) {
			return;
		}
		poseStack.pushPose();
		// Follows the body, so it leans forward with it when crouching.
		this.getParentModel().body.translateAndRotate(poseStack);
		poseStack.translate(0.0F, DOWN, -FORWARD);
		// Model space is upside down and back to front; this turns the block the right way up.
		poseStack.scale(SIZE, -SIZE, -SIZE);
		carried(state).submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
