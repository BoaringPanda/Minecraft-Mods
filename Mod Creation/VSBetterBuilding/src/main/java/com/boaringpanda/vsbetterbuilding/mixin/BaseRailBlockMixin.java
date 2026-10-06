package com.boaringpanda.vsbetterbuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterbuilding.block.LockedBlocks;

/**
 * A locked rail ({@link LockedBlocks}) keeps its shape: no reshaping from a lever at a T-junction, a piston moving it, or its own
 * neighbour updates. Power (powered, activator and detector rails) is a separate path and still works.
 */
@Mixin(BaseRailBlock.class)
public class BaseRailBlockMixin {
	@Inject(method = "updateDir", at = @At("HEAD"), cancellable = true)
	private void vsbetterbuilding$keepLockedShape(Level level, BlockPos pos, BlockState state, boolean first,
			CallbackInfoReturnable<BlockState> cir) {
		if (LockedBlocks.isLocked(state)) {
			cir.setReturnValue(state);
		}
	}
}
