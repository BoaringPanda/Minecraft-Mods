package com.boaringpanda.vsbetterqol.client.mixin;

import com.boaringpanda.vsbetterqol.client.ClientConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

// The first-person on-fire overlay sits lower on the screen (ClientConfig.LOWER_FIRE), so it doesn't cover the view.
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
	// How far both flames are moved down, in blocks. They're 1 block tall, 0.5 in front of the camera, and at the default FOV about
	// 0.55 of them is on screen, so this hides about half of that.
	@Unique
	private static final float FIRE_DROP = 0.28F;

	// submitFire copies the pose stack into both flame quads, so moving it moves them together.
	@WrapOperation(method = "submit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;submitFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"))
	private void vsbetterqol$lowerFire(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite,
			Operation<Void> original) {
		if (!ClientConfig.LOWER_FIRE.on) {
			original.call(poseStack, submitNodeCollector, sprite);
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, -FIRE_DROP, 0.0F);
		original.call(poseStack, submitNodeCollector, sprite);
		poseStack.popPose();
	}
}
