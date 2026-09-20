package com.boaringpanda.bpsbettervanillabuilding.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadCombo;

/**
 * Lets the player choose what a break removes, by where they aim (see {@link LilyPadTarget}):
 * <ul>
 *   <li><b>Aiming at the accessory</b> removes just the accessory (dropping it - or nothing, if the
 *       tool wasn't the right one for that accessory, same as the accessory's own real vanilla drop
 *       rule) and leaves a plain lily pad behind, as if the accessory were a real separate block
 *       sitting on top.</li>
 *   <li><b>Aiming at the lily pad</b> breaks the pad, which takes the accessory with it: both drop,
 *       no tool needed, like a torch popping off when its block is broken (in creative only the
 *       accessory drops, not the pad). The mining time for this is instant, see
 *       {@link LilyPadTarget#destroyProgress}.</li>
 * </ul>
 * A plain lily pad left behind after the first case is an ordinary block with no code of ours involved.
 * <p>
 * Hooks {@code PlayerBlockBreakEvents.BEFORE} rather than overriding
 * {@code playerWillDestroy} - checked by disassembling the game: that
 * method's return value is captured but never actually written back to the
 * world (the caller unconditionally removes the block to air right
 * afterwards), so it can't be used to make the block become something else
 * instead. Returning {@code false} here, by contrast, is confirmed (again by
 * disassembly, via the Fabric API mixin that implements this event) to skip
 * vanilla's entire destroy sequence - no removal, no drops, no XP - which is
 * exactly the clean slate needed to substitute our own outcome. It also means
 * {@code Block.playerDestroy} is never reached for a combo, so the combo classes
 * don't override it.
 * <p>
 * Mining *speed* for the accessory (how long it takes, and whether an axe/pickaxe is faster or
 * required) is governed by each combo block's own {@code strength()}/{@code requiresCorrectToolForDrops()}/
 * mineable tag, set in {@link LilyPadAccessories} to match the real accessory's own vanilla values.
 * This class only fires once mining has already finished, to decide what happens next.
 */
public class LilyPadAccessoryBreaking {
	public static void initialize() {
		PlayerBlockBreakEvents.BEFORE.register(LilyPadAccessoryBreaking::beforeBreak);
	}

	private static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
		if (!(state.getBlock() instanceof LilyPadCombo combo)) {
			return true;
		}

		if (LilyPadTarget.aimedAtAccessory(combo, state, player, pos)) {
			breakAccessoryOnly(level, player, pos, state, combo, blockEntity);
		} else {
			breakPadAndAccessory(level, player, pos, state, combo, blockEntity);
		}

		// Skip vanilla's own destroy sequence entirely - we've already done
		// everything it would have (drops, new block state, particles, sound).
		return false;
	}

	private static void breakAccessoryOnly(Level level, Player player, BlockPos pos, BlockState state, LilyPadCombo combo, @Nullable BlockEntity blockEntity) {
		ItemStack tool = player.getMainHandItem();
		boolean correctTool = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);

		if (!player.isCreative() && correctTool) {
			for (ItemStack drop : combo.accessoryDrops(state, blockEntity)) {
				Block.popResource(level, pos, drop);
			}
		}

		playBreakEffects(level, player, pos, state);
		level.setBlockAndUpdate(pos, Blocks.LILY_PAD.defaultBlockState());
	}

	private static void breakPadAndAccessory(Level level, Player player, BlockPos pos, BlockState state, LilyPadCombo combo, @Nullable BlockEntity blockEntity) {
		// No tool check: the accessory is knocked off along with the pad rather than mined. That is why
		// it drops even in creative, like a torch popping off when the block under it is broken - only
		// the pad itself is skipped there, as with any block a creative player breaks.
		if (!player.isCreative()) {
			Block.popResource(level, pos, new ItemStack(Blocks.LILY_PAD));
		}
		for (ItemStack drop : combo.accessoryDrops(state, blockEntity)) {
			Block.popResource(level, pos, drop);
		}

		playBreakEffects(level, player, pos, state);
		level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
	}

	/**
	 * The particles and break sound vanilla's own destroy sequence would have sent to other players - fired
	 * by hand since that whole sequence is skipped. The combo's own {@code spawnDestroyByEntityParticles}
	 * picks the part(s) that went (see {@link LilyPadTarget#spawnDestroyParticles}) and leaves the breaker
	 * out: their own client already showed it when it predicted the break.
	 * <p>
	 * Not {@code globalLevelEvent}: clients ignore this event when it arrives as a global one, so nobody
	 * else would see or hear anything.
	 */
	private static void playBreakEffects(Level level, Player player, BlockPos pos, BlockState state) {
		state.getBlock().spawnDestroyByEntityParticles(level, player, pos, state);
	}
}
