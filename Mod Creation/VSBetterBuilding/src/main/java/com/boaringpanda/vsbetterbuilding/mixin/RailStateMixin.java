package com.boaringpanda.vsbetterbuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterbuilding.block.LockedBlocks;

/**
 * A rail placed or reshaped next to a locked rail ({@link LockedBlocks}) can't bend the locked one to meet it. It only joins the locked
 * rail if that already points at it.
 */
@Mixin(RailState.class)
public class RailStateMixin {
	@Shadow
	private BlockState state;

	@Shadow
	private boolean connectsTo(RailState rail) {
		throw new AssertionError();
	}

	/** Vanilla: "already joined, or has a free end". A locked rail's ends are never free. */
	@Inject(method = "canConnectTo", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$onlyExistingEnds(RailState rail, CallbackInfoReturnable<Boolean> cir) {
		if (LockedBlocks.isLocked(this.state)) {
			cir.setReturnValue(this.connectsTo(rail));
		}
	}

	/** Joining would recompute (and could change) the locked rail's shape. */
	@Inject(method = "connectTo", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$keepLockedShape(RailState rail, CallbackInfo ci) {
		if (LockedBlocks.isLocked(this.state)) {
			ci.cancel();
		}
	}
}
