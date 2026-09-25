package com.boaringpanda.bettervanillabuilding.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;

/**
 * F3+B shows the hitboxes of hidden item frames (the Builder Stick hides them with vanilla's {@code Invisible} flag), which vanilla skips
 * for every invisible entity. A hidden frame, empty or not, is still an entity, so this keeps them easy to find. Other invisible entities
 * stay hidden, as in vanilla.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
	@WrapOperation(method = "emitGizmos", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInvisible()Z"))
	private boolean bettervanillabuilding$showHiddenFrames(Entity entity, Operation<Boolean> original) {
		return !(entity instanceof ItemFrame) && original.call(entity);
	}
}
