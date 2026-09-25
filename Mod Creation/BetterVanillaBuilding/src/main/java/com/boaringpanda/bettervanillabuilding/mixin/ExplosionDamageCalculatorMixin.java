package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;

/** A mixed slab block resists explosions like its tougher slab, the same one that decides how it mines ({@link MixedSlabs}). */
@Mixin(ExplosionDamageCalculator.class)
public class ExplosionDamageCalculatorMixin {
	@WrapOperation(
			method = "getBlockExplosionResistance",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getExplosionResistance()F"))
	private float bettervanillabuilding$toughestSlabResists(Block block, Operation<Float> original, @Local(argsOnly = true) BlockGetter level,
			@Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState state) {
		return original.call(MixedSlabs.is(state) ? MixedSlabs.halves(level, pos).toughest().getBlock() : block);
	}
}
