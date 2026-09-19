package com.boaringpanda.extrablocks.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

/**
 * A lily pad with a redstone torch on it that gives off the same signal a lit redstone torch
 * standing on a solid block would. The signal rules are copied from vanilla's
 * {@code RedstoneTorchBlock} (checked by disassembly):
 * <ul>
 *   <li>a weak signal of 15 to every side except the one it stands on - so it powers adjacent
 *       redstone dust, repeaters, lamps and the like, but not the block below it;</li>
 *   <li>a strong signal of 15 to the block directly above it, which then powers that block's own
 *       neighbours the way any strongly powered block does.</li>
 * </ul>
 * It is always lit. A real torch turns off when the block it stands on is powered, but here that
 * block is water, which can't be, so there is no state to track. Neighbours are told when it appears
 * or disappears, the same way vanilla does it, so nothing needs to wait for some other update to
 * notice the change.
 */
public class LilyPadRedstoneTorchBlock extends LilyPadAccessoryBlock {
	private static final int POWER = 15;

	public LilyPadRedstoneTorchBlock(Properties properties, Block accessory) {
		super(properties, accessory);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	/** {@code direction} points from the block asking towards this one, so UP means "asked by the block below". */
	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return direction == Direction.UP ? 0 : POWER;
	}

	/** Asked by the block above, which sees this one in the DOWN direction. */
	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return direction == Direction.DOWN ? getSignal(state, level, pos, direction) : 0;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		notifyNeighbors(level, pos);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		if (!movedByPiston) {
			notifyNeighbors(level, pos);
		}
	}

	private void notifyNeighbors(Level level, BlockPos pos) {
		Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, null, Direction.UP);
		for (Direction direction : Direction.values()) {
			level.updateNeighborsAt(
					pos.relative(direction),
					this,
					ExperimentalRedstoneUtils.withFront(orientation, direction)
			);
		}
	}
}
