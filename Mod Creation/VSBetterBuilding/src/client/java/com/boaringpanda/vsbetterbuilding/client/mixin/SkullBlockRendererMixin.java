package com.boaringpanda.vsbetterbuilding.client.mixin;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.boaringpanda.vsbetterbuilding.block.StackedHeads;
import com.boaringpanda.vsbetterbuilding.client.blockentity.TopHeadRenderState;

/**
 * Draws the head stacked on top of another ({@link StackedHeads}) with vanilla's own head model, skin and animation, half a block up and
 * turned its own way. While the player mines one of the two heads, the cracks show on that head only. A raised head (left behind when
 * the head under it broke) is drawn half a block up too.
 */
@Mixin(SkullBlockRenderer.class)
public class SkullBlockRendererMixin {
	@Unique
	private static final Transformation RAISED = new Transformation(new Matrix4f().translation(0.0F, 0.5F, 0.0F));

	@Shadow
	@Final
	private Function<SkullBlock.Type, SkullModelBase> modelByType;

	@Shadow
	@Final
	private PlayerSkinRenderCache playerSkinRenderCache;

	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At("TAIL"))
	private void vsbetterbuilding$extractTopHead(SkullBlockEntity entity, SkullBlockRenderState state, float partialTicks, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, CallbackInfo ci) {
		BlockState blockState = entity.getBlockState();
		if (StackedHeads.isRaised(blockState)) {
			// A head left behind when the head under it broke stays where it was, half a block up.
			state.transformation = RAISED.compose(state.transformation);
		}
		TopHeadRenderState holder = (TopHeadRenderState) state;
		// Render states are reused, so a head without a top head clears any left over.
		if (!StackedHeads.isStacked(blockState) || entity.getLevel() == null) {
			holder.vsbetterbuilding$setTopHead(null);
			return;
		}

		StackedHeads.Head top = StackedHeads.top(entity, blockState);
		SkullBlock.Type type = ((SkullBlock) top.block()).getType();
		// Vanilla's private resolveSkullRenderType, for the top head's own skin.
		RenderType renderType = type == SkullBlock.Types.PLAYER && top.profile().isPresent()
				? playerSkinRenderCache.getOrDefault(top.profile().get()).renderType()
				: SkullBlockRenderer.getSkullRenderType(type, null);

		ModelFeatureRenderer.CrumblingOverlay topBreak = null;
		if (state.breakProgress != null
				&& StackedHeads.aimsAtTop(Minecraft.getInstance().player, entity.getLevel(), entity.getBlockPos(), blockState)) {
			topBreak = state.breakProgress;
			state.breakProgress = null;
		}
		holder.vsbetterbuilding$setTopHead(new TopHeadRenderState.TopHead(type, renderType, top.rotation(), topBreak));
	}

	@Inject(
			method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("TAIL"))
	private void vsbetterbuilding$submitTopHead(SkullBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
			CameraRenderState camera, CallbackInfo ci) {
		TopHeadRenderState.TopHead top = ((TopHeadRenderState) state).vsbetterbuilding$getTopHead();
		if (top == null) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.5F, 0.0F);
		poseStack.mulPose(SkullBlockRenderer.TRANSFORMATIONS.freeTransformations(top.rotation()));
		SkullBlockRenderer.submitSkull(state.animationProgress, poseStack, submitNodeCollector, state.lightCoords, modelByType.apply(top.type()),
				top.renderType(), 0, top.breakProgress());
		poseStack.popPose();
	}
}
