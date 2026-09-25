package com.boaringpanda.bettervanillabuilding.block;

import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Blocks the Builder Stick has changed keep the shape it gave them: a locked fence, pane, bars, wall, stair, fence gate or placed rod ignores
 * its neighbours ({@code BlockStateBaseMixin.updateShape}), and a locked rail never bends to meet other rails or redstone
 * ({@code BaseRailBlockMixin}, {@code RailStateMixin}). {@code BlockMixin} adds {@link #LOCKED} to every block {@link #canLock} accepts,
 * off by default, so placing one always starts unlocked.
 * <p>
 * These are the stick's blocks whose neighbours would otherwise undo it. Every other one only checks its neighbours for water or for
 * support, except doors (each half copies the other, which opening needs) and wall bells (they re-hang to stay on a wall), which are
 * left to vanilla.
 */
public final class LockedBlocks {
	public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

	private LockedBlocks() {
	}

	/**
	 * Fences, glass and stained glass panes, iron and copper bars and walls (their sides), stairs (their shape), fence gates (their
	 * height), this mod's placed rods (an upright rod's arms), and all four rails.
	 */
	public static boolean canLock(Block block) {
		return block instanceof FenceBlock || block instanceof IronBarsBlock || block instanceof WallBlock || block instanceof StairBlock
				|| block instanceof FenceGateBlock || block instanceof PlacedRodBlock || block instanceof BaseRailBlock;
	}

	public static boolean isLocked(BlockState state) {
		return state.hasProperty(LOCKED) && state.getValue(LOCKED);
	}
}
