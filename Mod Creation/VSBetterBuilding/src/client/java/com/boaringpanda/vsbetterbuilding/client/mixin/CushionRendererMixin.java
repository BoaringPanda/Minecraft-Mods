package com.boaringpanda.vsbetterbuilding.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.cushion.CushionModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CushionRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CushionRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.entity.decoration.Cushion;

import com.boaringpanda.vsbetterbuilding.VSBetterBuilding;
import com.boaringpanda.vsbetterbuilding.client.RainbowRenderState;
import com.boaringpanda.vsbetterbuilding.entity.RainbowCushions;

/**
 * Draws a rainbow cushion ({@link RainbowCushions}) with {@code block/rainbow_cushion}: the 16 vanilla cushion textures as the frames of an
 * animated texture. Vanilla's cushion textures aren't in an atlas (and only atlas textures animate), so this one is in the block atlas,
 * which also keeps it in step with every rainbow block.
 */
@Mixin(CushionRenderer.class)
public class CushionRendererMixin {
	@Unique
	private static final SpriteId RAINBOW_CUSHION = Sheets.BLOCKS_MAPPER.apply(VSBetterBuilding.id("rainbow_cushion"));

	@Shadow
	@Final
	private CushionModel model;

	@Unique
	private SpriteGetter vsbetterbuilding$sprites;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void vsbetterbuilding$keepSprites(EntityRendererProvider.Context context, CallbackInfo ci) {
		vsbetterbuilding$sprites = context.getSprites();
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/Cushion;Lnet/minecraft/client/renderer/entity/state/CushionRenderState;F)V",
			at = @At("TAIL"))
	private void vsbetterbuilding$extractRainbow(Cushion cushion, CushionRenderState state, float partialTicks, CallbackInfo ci) {
		((RainbowRenderState) state).vsbetterbuilding$setRainbow(RainbowCushions.isRainbow(cushion));
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/CushionRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;III)V"))
	private void vsbetterbuilding$drawRainbow(SubmitNodeCollector submitNodeCollector, Model<?> model, Object state, PoseStack poseStack,
			RenderType renderType, int lightCoords, int overlayCoords, int outlineColor, Operation<Void> original) {
		if (state instanceof CushionRenderState cushion && ((RainbowRenderState) cushion).vsbetterbuilding$isRainbow()) {
			submitNodeCollector.submitModel(this.model, cushion, poseStack, lightCoords, overlayCoords, -1, RAINBOW_CUSHION, vsbetterbuilding$sprites,
					outlineColor);
		} else {
			original.call(submitNodeCollector, model, state, poseStack, renderType, lightCoords, overlayCoords, outlineColor);
		}
	}
}
