package com.boaringpanda.vsbetterqol.mixin;

import com.boaringpanda.vsbetterqol.ElytraRockets;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;

// A rocket held by a swimming player with an elytra on (ElytraRockets.canSwimBoost) pushes them like it would a gliding one: vanilla's
// boost towards where they look, made stronger because water slows them down. Only while they keep swimming.
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
	// How much stronger each push is underwater than vanilla's gliding push (Dylan 2026-10-08: "a little bit stronger").
	@Unique
	private static final double SWIM_BOOST = 1.3;

	@Shadow
	private @Nullable LivingEntity attachedToEntity;

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"))
	private boolean vsbetterqol$swimBoost(boolean fallFlying) {
		return fallFlying || ElytraRockets.canSwimBoost(this.attachedToEntity);
	}

	// Vanilla's push on the player. Not gliding means it's our swim boost, so the change it makes is scaled up.
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
	private void vsbetterqol$strongerSwimBoost(LivingEntity entity, Vec3 movement, Operation<Void> original) {
		if (!entity.isFallFlying()) {
			Vec3 old = entity.getDeltaMovement();
			movement = old.add(movement.subtract(old).scale(SWIM_BOOST));
		}
		original.call(entity, movement);
	}
}
