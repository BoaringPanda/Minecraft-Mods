package com.boaringpanda.bettervanillafeatures.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.ai.goal.TemptGoal;

// Goal-based animals (chicken, cow, pig, sheep, horse, ...): no 5 second wait before following food again, except after a shy
// animal (wild ocelot, stray cat) got spooked, which keeps vanilla's wait so taming them still works.
@Mixin(TemptGoal.class)
public class TemptGoalMixin {
	// True when the last canContinueToUse bailed out before reaching canUse, which only the spook checks do.
	@Unique
	private boolean bettervanillafeatures$spooked;

	@Inject(method = "canContinueToUse", at = @At("HEAD"))
	private void bettervanillafeatures$assumeSpooked(CallbackInfoReturnable<Boolean> cir) {
		bettervanillafeatures$spooked = true;
	}

	@WrapOperation(method = "canContinueToUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/TemptGoal;canUse()Z"))
	private boolean bettervanillafeatures$notSpooked(TemptGoal goal, Operation<Boolean> original) {
		bettervanillafeatures$spooked = false;
		return original.call(goal);
	}

	@ModifyExpressionValue(method = "stop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/TemptGoal;reducedTickDelay(I)I"))
	private int bettervanillafeatures$noCalmDown(int calmDown) {
		boolean spooked = bettervanillafeatures$spooked;
		bettervanillafeatures$spooked = false;
		return spooked ? calmDown : 0;
	}
}
