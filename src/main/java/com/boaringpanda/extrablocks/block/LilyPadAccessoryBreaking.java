package com.boaringpanda.extrablocks.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import com.boaringpanda.extrablocks.block.custom.LilyPadCombo;

/**
 * Makes "breaking" a lily pad accessory a two-stage thing, like it would be
 * if the accessory were a real separate block sitting on top of something:
 * the first break removes just the accessory (dropping it - or nothing, if
 * the tool wasn't the right one for that accessory, same as the accessory's
 * own real vanilla drop rule) and leaves a plain lily pad behind; only a
 * *second* break (now on an ordinary lily pad, no code of ours involved)
 * removes that too.
 * <p>
 * Hooks {@code PlayerBlockBreakEvents.BEFORE} rather than overriding
 * {@code playerWillDestroy} - checked by disassembling the game: that
 * method's return value is captured but never actually written back to the
 * world (the caller unconditionally removes the block to air right
 * afterwards), so it can't be used to make the block become something else
 * instead. Returning {@code false} here, by contrast, is confirmed (again by
 * disassembly, via the Fabric API mixin that implements this event) to skip
 * vanilla's entire destroy sequence - no removal, no drops, no XP - which is
 * exactly the clean slate needed to substitute our own outcome.
 * <p>
 * Mining *speed* (how long it takes, and whether an axe/pickaxe is faster or
 * required) is unrelated to this class - that's governed entirely by each
 * combo block's own {@code strength()}/{@code requiresCorrectToolForDrops()}/
 * mineable tag, set in {@link LilyPadAccessories} to match the real
 * accessory's own vanilla values. This class only fires once mining has
 * already finished, to decide what happens next.
 */
public class LilyPadAccessoryBreaking {
	public static void initialize() {
		PlayerBlockBreakEvents.BEFORE.register(LilyPadAccessoryBreaking::beforeBreak);
	}

	private static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
		if (!(state.getBlock() instanceof LilyPadCombo combo)) {
			return true;
		}

		ItemStack tool = player.getMainHandItem();
		boolean correctTool = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);

		if (!player.isCreative() && correctTool) {
			for (ItemStack drop : combo.accessoryDrops(state)) {
				Block.popResource(level, pos, drop);
			}
		}

		// Same particles + break sound vanilla's own destroy sequence would have
		// played, using the accessory combo's own state (its sound type is
		// LILY_PAD's, matching the other lily pad accessory sounds) - fired
		// manually since we're skipping that whole sequence below.
		level.globalLevelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(state));

		level.setBlockAndUpdate(pos, Blocks.LILY_PAD.defaultBlockState());

		// Skip vanilla's own destroy sequence entirely - we've already done
		// everything it would have (drops, new block state, particles, sound).
		return false;
	}
}
