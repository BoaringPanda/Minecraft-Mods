package com.boaringpanda.bpsbettervanillabuilding.client.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadTarget;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadCombo;

/**
 * Keeps the lily pad on screen when the player breaks just the accessory on top of it.
 * <p>
 * A client doesn't wait for the server when its player breaks a block: it predicts the result straight
 * away, and the prediction is always "the whole block becomes air". For a combo the server leaves a lily
 * pad behind instead (see {@code LilyPadAccessoryBreaking}), which only arrives a tick later, so the pad
 * would blink out and back in. Fabric's event fires right after that predicted removal, while the
 * prediction is still open, and this puts the lily pad straight back. It uses the same aim test as the
 * server, so the prediction matches what the server will do and nothing redraws when its answer arrives.
 */
public class LilyPadBreakPrediction {
	public static void initialize() {
		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
			if (state.getBlock() instanceof LilyPadCombo combo && LilyPadTarget.aimedAtAccessory(combo, state, player, pos)) {
				level.setBlock(pos, Blocks.LILY_PAD.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
			}
		});
	}
}
