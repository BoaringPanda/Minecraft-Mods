package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;

/** Landing on a mixed slab block sounds and looks like its top slab ({@link MixedSlabs}). */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@ModifyArg(
			method = "checkFallDamage",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/core/particles/BlockParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/level/block/state/BlockState;)V"))
	private BlockState bettervanillabuilding$landingParticlesOfTopSlab(BlockState onState, @Local(argsOnly = true) BlockPos pos) {
		return MixedSlabs.surface(((LivingEntity) (Object) this).level(), pos, onState);
	}

	@WrapOperation(
			method = "playBlockFallSound",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState bettervanillabuilding$fallSoundOfTopSlab(Level level, BlockPos pos, Operation<BlockState> original) {
		return MixedSlabs.surface(level, pos, original.call(level, pos));
	}
}
