package com.boaringpanda.bettervanillabuilding.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.boaringpanda.bettervanillabuilding.BetterVanillaBuilding;
import com.boaringpanda.bettervanillabuilding.block.Rainbow;
import com.boaringpanda.bettervanillabuilding.client.RainbowRenderState;

/**
 * Draws a rainbow banner's ({@link Rainbow}) base colour with {@code entity/banner/rainbow_base}: vanilla's base layer tinted with each
 * colour's own banner tint, as the frames of an animated texture, so it fades in step with every other rainbow block. Its patterns are
 * drawn on top in their own colours, as always. Banner items aren't changed.
 */
@Mixin(BannerRenderer.class)
public class BannerRendererMixin {
	/** In the banner pattern atlas with vanilla's base (its {@code entity/banner} folder takes every namespace's). */
	@Unique
	private static final SpriteId RAINBOW_BASE = new SpriteId(Sheets.BANNER_SHEET, BetterVanillaBuilding.id("entity/banner/rainbow_base"));
	/**
	 * Whether the banner being drawn is rainbow: the base layer is drawn from a static method, so {@link #bettervanillabuilding$drawRainbow}
	 * sets this for the length of the draw (all on the render thread).
	 */
	@Unique
	private static boolean bettervanillabuilding$drawingRainbow;

	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BannerBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At("TAIL"))
	private void bettervanillabuilding$extractRainbow(BannerBlockEntity entity, BannerRenderState state, float partialTicks, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, CallbackInfo ci) {
		BlockState blockState = entity.getBlockState();
		((RainbowRenderState) state).bettervanillabuilding$setRainbow(blockState.hasProperty(Rainbow.RAINBOW) && blockState.getValue(Rainbow.RAINBOW));
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("HEAD"))
	private void bettervanillabuilding$drawRainbow(BannerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
			CameraRenderState camera, CallbackInfo ci) {
		bettervanillabuilding$drawingRainbow = ((RainbowRenderState) state).bettervanillabuilding$isRainbow();
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("RETURN"))
	private void bettervanillabuilding$doneDrawing(BannerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
			CameraRenderState camera, CallbackInfo ci) {
		bettervanillabuilding$drawingRainbow = false;
	}

	/** The base layer (vanilla's base sprite tinted with the banner's colour), untinted from the rainbow texture instead. */
	@Inject(method = "submitPatternLayer", at = @At("HEAD"), cancellable = true)
	private static void bettervanillabuilding$rainbowBaseLayer(SpriteGetter sprites, PoseStack poseStack, OrderedSubmitNodeCollector submitNodeCollector,
			int lightCoords, int overlayCoords, Model<Object> model, Object state, SpriteId sprite, DyeColor color, CallbackInfo ci) {
		if (bettervanillabuilding$drawingRainbow && sprite.equals(Sheets.BANNER_PATTERN_BASE)) {
			submitNodeCollector.submitModel(model, state, poseStack, RAINBOW_BASE.renderType(RenderTypes::bannerPattern), lightCoords, overlayCoords,
					-1, sprites.get(RAINBOW_BASE), 0);
			ci.cancel();
		}
	}
}
