package com.boaringpanda.bpsbettervanillabuilding.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;

import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlockEntity;

/**
 * Breaking and middle-clicking a pair of heads act on the one the player is aiming at (see {@link StackedHeadsTarget}).
 * <ul>
 *   <li><b>Break</b> removes just that head and drops it. The other head is <b>not touched</b>: it stays exactly where it
 *       is, in the same half, facing the same way. The block stays a pair block holding one head, and only turns to air
 *       when its last head goes. Nothing is swapped or replaced, so there is nothing for the remaining head to blink at.</li>
 *   <li><b>Middle-click</b> gives that head's item; with ctrl in creative it keeps the head's data (a player's skin).</li>
 * </ul>
 * Hooks {@code PlayerBlockBreakEvents.BEFORE} rather than {@code playerWillDestroy}, for the same reason as
 * {@link LilyPadAccessoryBreaking}: returning {@code false} here skips vanilla's whole destroy sequence, which is
 * what is needed to leave the block in place instead of turning it to air. That sequence is skipped, so no
 * {@code playerDestroy} runs for this block; the drops and the break effects are done here.
 */
public class StackedHeadsBreaking {
	public static void initialize() {
		PlayerBlockBreakEvents.BEFORE.register(StackedHeadsBreaking::beforeBreak);
		PlayerPickItemEvents.BLOCK.register(StackedHeadsBreaking::onPickBlock);
	}

	private static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
		if (!(state.getBlock() instanceof StackedHeadsBlock) || !(blockEntity instanceof StackedHeadsBlockEntity heads)) {
			return true;
		}

		boolean top = StackedHeadsTarget.aimedAtTop(heads, player, pos);
		SkullBlockEntity broken = heads.head(top);
		if (broken == null) {
			// An empty block shouldn't exist; let vanilla clear it.
			return true;
		}

		BlockState brokenState = broken.getBlockState();
		if (!player.isCreative()) {
			for (ItemStack drop : heads.drops(top)) {
				Block.popResource(level, pos, drop);
			}
		}

		if (heads.count() <= 1) {
			// That was the last head, so the block goes, leaving whatever fluid was in it.
			level.setBlockAndUpdate(pos, level.getFluidState(pos).createLegacyBlock());
		} else {
			// The other head stays as it is. Only this block entity's data changes, which is sent to clients as an update.
			heads.removeHead(top);
			heads.setChanged();
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
		}

		// The particles and break sound vanilla's own sequence would have sent to other players, from the head that
		// went. The breaker's own client already showed its own when it predicted the break, so they're left out.
		level.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(brokenState));

		return false;
	}

	@Nullable
	private static ItemStack onPickBlock(ServerPlayer player, BlockPos pos, BlockState state, boolean includeData) {
		if (!(state.getBlock() instanceof StackedHeadsBlock) || !(player.level().getBlockEntity(pos) instanceof StackedHeadsBlockEntity heads)) {
			return null;
		}

		SkullBlockEntity head = heads.head(StackedHeadsTarget.aimedAtTop(heads, player, pos));
		if (head == null) {
			return null;
		}

		ItemStack stack = new ItemStack(head.getBlockState().getBlock());
		if (includeData) {
			stack.applyComponents(head.collectComponents());
		}

		return stack;
	}
}
