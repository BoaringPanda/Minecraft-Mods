package com.boaringpanda.bettervanillabuilding.mixin;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.boaringpanda.bettervanillabuilding.block.MixedSlabBlockEntity;
import com.boaringpanda.bettervanillabuilding.block.MixedSlabCarrier;
import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;

/**
 * A moving block carries a mixed slab block's two slabs ({@code PistonBaseBlockMixin}), saves them with itself (a world saved mid-push
 * keeps them, and players who load the chunk mid-push get them), and puts them into the mixed block it becomes when the push ends.
 */
@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin implements MixedSlabCarrier {
	@Unique
	private MixedSlabs.@Nullable Halves bettervanillabuilding$halves;

	@Override
	public MixedSlabs.@Nullable Halves bettervanillabuilding$getHalves() {
		return bettervanillabuilding$halves;
	}

	@Override
	public void bettervanillabuilding$setHalves(MixedSlabs.@Nullable Halves halves) {
		bettervanillabuilding$halves = halves;
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void bettervanillabuilding$saveSlabs(ValueOutput output, CallbackInfo ci) {
		if (bettervanillabuilding$halves != null) {
			output.store("bettervanillabuilding:mixed_slab", MixedSlabs.Halves.CODEC, bettervanillabuilding$halves);
		}
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void bettervanillabuilding$loadSlabs(ValueInput input, CallbackInfo ci) {
		bettervanillabuilding$halves = input.read("bettervanillabuilding:mixed_slab", MixedSlabs.Halves.CODEC).orElse(null);
	}

	/** The push ended normally: the moving block became its block again. */
	@WrapOperation(
			method = "tick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private static boolean bettervanillabuilding$landSlabs(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original,
			@Local(argsOnly = true) PistonMovingBlockEntity entity) {
		boolean changed = original.call(level, pos, state, flags);
		giveSlabs(level, pos, entity);
		return changed;
	}

	/** The push was cut short (a piston retracting early, or the moving block being removed): it became its block at once. */
	@WrapOperation(
			method = "finalTick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean bettervanillabuilding$landSlabsEarly(Level level, BlockPos pos, BlockState state, Operation<Boolean> original) {
		boolean changed = original.call(level, pos, state);
		giveSlabs(level, pos, (PistonMovingBlockEntity) (Object) this);
		return changed;
	}

	@Unique
	private static void giveSlabs(Level level, BlockPos pos, PistonMovingBlockEntity entity) {
		MixedSlabs.Halves halves = ((MixedSlabCarrier) entity).bettervanillabuilding$getHalves();
		if (halves != null && level.getBlockEntity(pos) instanceof MixedSlabBlockEntity mixed) {
			mixed.setHalves(halves);
		}
	}
}
