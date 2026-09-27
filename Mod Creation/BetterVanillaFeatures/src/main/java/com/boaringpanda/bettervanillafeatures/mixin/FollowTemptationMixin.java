package com.boaringpanda.bettervanillafeatures.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

// Brain-based animals (goat, camel, frog, axolotl, sniffer, ...): no 5 second cooldown before following food again.
@Mixin(FollowTemptation.class)
public class FollowTemptationMixin {
	@WrapWithCondition(
			method = "stop",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/ai/Brain;setMemory(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;)V"))
	private boolean bettervanillafeatures$noCooldown(Brain<?> brain, MemoryModuleType<?> type, Object value) {
		return false;
	}
}
