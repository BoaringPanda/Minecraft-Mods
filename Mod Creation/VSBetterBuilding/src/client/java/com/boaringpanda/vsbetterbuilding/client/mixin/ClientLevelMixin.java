package com.boaringpanda.vsbetterbuilding.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterbuilding.block.LilyPadDecorations;
import com.boaringpanda.vsbetterbuilding.block.MixedSlabs;

/**
 * While mining a mixed slab block, each hit's sound and crack particles are the slab under the cursor, and the particles come off that
 * half ({@link MixedSlabs#targeted}). While mining the bare lily pad under a decoration, they're the pad's
 * ({@link LilyPadDecorations#aimsAtPad}).
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	@WrapOperation(
			method = "addBreakingBlockEffects",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState vsbetterbuilding$hitTargetedSlab(ClientLevel level, BlockPos pos, Operation<BlockState> original) {
		BlockState state = original.call(level, pos);
		if (MixedSlabs.is(state)) {
			return MixedSlabs.targeted(level, pos, Minecraft.getInstance().hitResult);
		}
		return LilyPadDecorations.aimsAtPad(Minecraft.getInstance().player, level, pos, state) ? Blocks.LILY_PAD.defaultBlockState() : state;
	}
}
