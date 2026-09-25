package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bettervanillabuilding.block.FlowerClumps;

/**
 * An enderman takes a whole clump of flowers ({@link FlowerClumps}), puts it down as a clump and drops every flower when killed.
 * Vanilla carries the block's default state, which would lose all but one flower.
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal")
public class EndermanTakeBlockGoalMixin {
	@WrapOperation(method = "tick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/entity/monster/Enderman;setCarriedBlock(Lnet/minecraft/world/level/block/state/BlockState;)V"))
	private void bettervanillabuilding$takeWholeClump(Enderman enderman, BlockState carried, Operation<Void> original,
			@Local BlockState taken) {
		original.call(enderman, FlowerClumps.keepQuarters(taken, carried));
	}
}
