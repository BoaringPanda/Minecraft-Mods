package com.boaringpanda.vsbetterqol.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import com.boaringpanda.vsbetterqol.client.CarriedBlockLayer;

// A player carrying a container holds both arms out in front, around it. Last, so it wins over walking swing, items and crouching.
// Armour uses the same model setup, so it follows the arms.
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
	// Arms raised forward (radians; -pi/2 would be straight out), turned in a little to hold the sides.
	@Unique
	private static final float ARMS_FORWARD = -1.1F;
	@Unique
	private static final float ARMS_INWARD = 0.3F;
	// Vanilla tips the arms this much further back when crouching, with the body.
	@Unique
	private static final float CROUCH_TILT = 0.4F;

	@Shadow
	@Final
	public ModelPart rightArm;

	@Shadow
	@Final
	public ModelPart leftArm;

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
	private void vsbetterqol$carryPose(HumanoidRenderState state, CallbackInfo ci) {
		if (!CarriedBlockLayer.isCarrying(state)) {
			return;
		}
		float xRot = ARMS_FORWARD + (state.isCrouching ? CROUCH_TILT : 0.0F);
		this.rightArm.xRot = xRot;
		this.leftArm.xRot = xRot;
		this.rightArm.yRot = -ARMS_INWARD;
		this.leftArm.yRot = ARMS_INWARD;
		this.rightArm.zRot = 0.0F;
		this.leftArm.zRot = 0.0F;
	}
}
