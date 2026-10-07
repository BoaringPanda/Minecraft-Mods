package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.boaringpanda.vsbetterqol.Carrying;

// No sprinting while carrying a container. Both the client (sprint key) and the server (sprint packet) go through setSprinting, and
// LivingEntity's version is where the sprint speed boost is added, so changing it here stops both the flag and the boost.
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "setSprinting", at = @At("HEAD"), argsOnly = true)
	private boolean vsbetterqol$noSprintWhileCarrying(boolean sprinting) {
		return sprinting && !((Object) this instanceof Player player && Carrying.isCarrying(player));
	}
}
