package com.boaringpanda.bpsbettervanillabuilding.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;

import com.boaringpanda.bpsbettervanillabuilding.block.custom.MixedSlabBlock;

/**
 * Middle-click (pick block) on a combined slab block gives the one slab the player is pointing at, not both and not the
 * block itself (it has no item). The upper half of the block is the top slab and the lower half the bottom slab, judged by
 * where the player's view ray meets the block: the same way {@link LilyPadTarget} works out which part of a lily pad combo
 * is aimed at, since the server is only ever told the block's position.
 * <p>
 * Everything after that is vanilla's own pick: in creative the slab goes into the hotbar, and in survival it is selected
 * if it is already in the inventory (and does nothing if it isn't).
 */
public class MixedSlabPicking {
	public static void initialize() {
		PlayerPickItemEvents.BLOCK.register(MixedSlabPicking::onPickBlock);
	}

	@Nullable
	private static ItemStack onPickBlock(ServerPlayer player, BlockPos pos, BlockState state, boolean includeData) {
		if (!(state.getBlock() instanceof MixedSlabBlock combo)) {
			return null;
		}

		Boolean upperHalf = MixedSlabTarget.upperHalf(player, pos);
		if (upperHalf == null) {
			// Not looking at the block after all; the block's own fallback (the bottom slab) answers.
			return null;
		}

		return new ItemStack(upperHalf ? combo.topSlab() : combo.bottomSlab());
	}
}
