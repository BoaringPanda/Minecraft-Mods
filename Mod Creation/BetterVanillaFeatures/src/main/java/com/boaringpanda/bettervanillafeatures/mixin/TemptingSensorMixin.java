package com.boaringpanda.bettervanillafeatures.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.ai.sensing.TemptingSensor;

// Brain-based animals look for food every tick instead of once a second, so they notice it straight away.
// Every TemptingSensor (food, frog and nautilus temptations) is made through this private constructor.
@Mixin(TemptingSensor.class)
public class TemptingSensorMixin {
	@Inject(method = "<init>(Ljava/util/function/BiPredicate;)V", at = @At("TAIL"))
	private void bettervanillafeatures$scanEveryTick(CallbackInfo ci) {
		((SensorAccessor) this).bettervanillafeatures$setScanRate(1);
	}
}
