package com.boaringpanda.extrablocks.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;

import com.boaringpanda.extrablocks.block.custom.LilyPadCombo;

/**
 * Middle-click (pick block) on a combo gives the part the player is aiming at, by the same aim test as
 * breaking (see {@link LilyPadTarget}): a lily pad for the pad, the accessory's own item for the rest.
 * <p>
 * Only the pad is answered here. Returning null for everything else lets vanilla carry on: it asks the
 * combo's own {@code getCloneItemStack} (every combo returns its accessory's item), then for ctrl+pick in
 * creative copies the block entity's data onto it - which is how a player head keeps its skin. Fabric
 * skips that copy for a stack this event returns, so the accessory isn't returned from here too.
 */
public class LilyPadAccessoryPicking {
	public static void initialize() {
		PlayerPickItemEvents.BLOCK.register(LilyPadAccessoryPicking::onPickBlock);
	}

	@Nullable
	private static ItemStack onPickBlock(ServerPlayer player, BlockPos pos, BlockState state, boolean includeData) {
		if (state.getBlock() instanceof LilyPadCombo combo && LilyPadTarget.aimedPart(combo, state, player, pos) == LilyPadTarget.Part.PAD) {
			return new ItemStack(Blocks.LILY_PAD);
		}

		return null;
	}
}
