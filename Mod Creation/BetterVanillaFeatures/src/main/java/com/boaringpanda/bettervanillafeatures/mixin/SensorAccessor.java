package com.boaringpanda.bettervanillafeatures.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.ai.sensing.Sensor;

@Mixin(Sensor.class)
public interface SensorAccessor {
	@Mutable
	@Accessor("scanRate")
	void bettervanillafeatures$setScanRate(int scanRate);
}
