package com.boaringpanda.extrablocks.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.extrablocks.block.LilyPadTarget;
import com.boaringpanda.extrablocks.block.custom.LilyPadCombo;

/**
 * The cracks and the hit sound while a block is being mined all come from
 * {@code ClientLevel.addBreakingBlockEffects}, which takes them from whatever state is at the position -
 * for a lily pad combo, the lily pad's texture, green tint and sound. This swaps in the real vanilla state
 * of the part being mined ({@link LilyPadTarget#partState}), so vanilla draws that part's own cracks, on
 * its own shape, and plays its own hit sound.
 * <p>
 * The server sends these effects to everyone nearby each mining tick, but not who is mining, so the aim
 * used is this client's own player: exact for your own mining, and for someone else's it's the accessory
 * unless you happen to be aiming at the same block. No Fabric API hook covers this - see CLAUDE.md.
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	@ModifyExpressionValue(
			method = "addBreakingBlockEffects",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
			)
	)
	private BlockState useMinedPartState(BlockState state, @Local(argsOnly = true) BlockPos pos) {
		if (state.getBlock() instanceof LilyPadCombo combo) {
			return LilyPadTarget.partState(combo, state, Minecraft.getInstance().player, pos);
		}

		return state;
	}
}
