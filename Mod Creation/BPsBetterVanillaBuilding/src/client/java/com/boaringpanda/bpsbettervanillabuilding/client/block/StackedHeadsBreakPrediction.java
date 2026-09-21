package com.boaringpanda.bpsbettervanillabuilding.client.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;

import com.boaringpanda.bpsbettervanillabuilding.block.StackedHeadsTarget;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlockEntity;

/**
 * Stops the head that stays behind from blinking when the player breaks the other one.
 * <p>
 * A client doesn't wait for the server when its player breaks a block: it predicts the result straight away, and the
 * prediction is always "the whole block becomes air". For a pair of heads the server leaves the block in place with the
 * other head still in it (see {@code StackedHeadsBreaking}), which only arrives a tick later, so that head would blink out
 * and back in. Fabric's event fires right after the predicted removal, while the prediction is still open, and this
 * puts the same block straight back holding the other head, in the same half with the same rotation and skin, using the
 * same aim test as the server so the two agree and nothing changes visibly when the server's answer arrives.
 * <p>
 * By then the block entity is already gone, so the pair the crosshair was on is remembered each tick.
 */
public class StackedHeadsBreakPrediction {
	@Nullable
	private static StackedHeadsBlockEntity aimedPair;

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			aimedPair = null;
			if (client.level != null && client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
					&& client.level.getBlockEntity(hit.getBlockPos()) instanceof StackedHeadsBlockEntity pair) {
				aimedPair = pair;
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> aimedPair = null);

		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
			StackedHeadsBlockEntity pair = aimedPair;
			if (!(state.getBlock() instanceof StackedHeadsBlock) || pair == null || !pair.getBlockPos().equals(pos) || pair.count() < 2) {
				// Breaking the last head really does remove the block, so the prediction of air is already right.
				return;
			}

			boolean keptTop = !StackedHeadsTarget.aimedAtTop(pair, player, pos);
			SkullBlockEntity kept = pair.head(keptTop);
			level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE);
			if (kept != null && level.getBlockEntity(pos) instanceof StackedHeadsBlockEntity restored) {
				restored.replaceHead(keptTop, kept.getBlockState()).applyComponents(kept.collectComponents(), DataComponentPatch.EMPTY);
			}
		});
	}
}
