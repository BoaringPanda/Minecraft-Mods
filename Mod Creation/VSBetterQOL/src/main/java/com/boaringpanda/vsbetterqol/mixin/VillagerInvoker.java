package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

// Vanilla's trade generation, for the re-roll button (VillagerReroll).
@Mixin(Villager.class)
public interface VillagerInvoker {
	@Invoker("updateTrades")
	void vsbetterqol$updateTrades(ServerLevel level);
}
