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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import com.boaringpanda.vsbetterqol.Carrying;
import com.boaringpanda.vsbetterqol.client.ClientConfig;

// First person: while carrying a container, it fills the bottom of the screen instead of the hands and held items. A held shield sits
// lower (ClientConfig.LOWER_SHIELD).
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
	// Block size and where its centre sits from the camera, in blocks.
	@Unique
	private static final float SIZE = 0.8F;
	@Unique
	private static final float DOWN = 0.65F;
	@Unique
	private static final float FORWARD = 1.0F;
	// How far a held shield is moved down, in blocks (about half of it out of view).
	@Unique
	private static final float SHIELD_DROP = 0.2F;

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

	// A shield (either hand, blocking too) is moved straight down the screen before vanilla positions it, so all of vanilla's
	// animations still happen, just lower. Vanilla's own popPose at the end of the method undoes it.
	@Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
	private void vsbetterqol$lowerShield(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks,
			float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
		if (ClientConfig.LOWER_SHIELD.on && itemStack.getItem() instanceof ShieldItem) {
			poseStack.translate(0.0F, -SHIELD_DROP, 0.0F);
		}
	}
}
