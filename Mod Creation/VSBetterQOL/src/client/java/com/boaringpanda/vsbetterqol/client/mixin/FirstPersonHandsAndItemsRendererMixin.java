package com.boaringpanda.vsbetterqol.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

import com.boaringpanda.vsbetterqol.Carrying;

// First person: while carrying a container, it fills the bottom of the screen instead of the hands and held items.
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
	// Block size and where its centre sits from the camera, in blocks.
	@Unique
	private static final float SIZE = 0.8F;
	@Unique
	private static final float DOWN = 0.65F;
	@Unique
	private static final float FORWARD = 1.0F;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private final ItemStackRenderState vsbetterqol$carried = new ItemStackRenderState();

	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void vsbetterqol$submitCarried(float partialTicks, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
			PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
		LocalPlayer player = this.minecraft.player;
		if (player == null || !Carrying.isCarrying(player) || playerState.avatarRenderState == null) {
			return;
		}
		ci.cancel();
		this.minecraft.getItemModelResolver().updateForTopItem(this.vsbetterqol$carried, Carrying.carried(player), ItemDisplayContext.NONE,
				this.minecraft.level, player, 0);
		poseStack.pushPose();
		poseStack.translate(0.0F, -DOWN, -FORWARD);
		poseStack.scale(SIZE, SIZE, SIZE);
		this.vsbetterqol$carried.submit(poseStack, submitNodeCollector, playerState.avatarRenderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}
}
