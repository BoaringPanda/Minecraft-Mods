package com.boaringpanda.bettervanillabuilding.block;

import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RodBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A vanilla item (a stick, blaze rod or breeze rod) placed as a block, the way an end rod is: pointing out from the face it was placed
 * on. It is vanilla's {@link RodBlock} (the end rod's parent: facing, the rod-shaped hitbox, rotating and mirroring) with the end rod's
 * placement copied, and it gives back its item when picked. It has no item of its own; {@link PlacedRods} places it from the vanilla one.
 * <p>
 * An upright rod (facing up or down) reaches an arm out at half height to a rod of the same kind lying beside it and pointing at it, from
 * its middle to the edge of its block, where the lying rod's end is. The arms are the fence's {@code north/east/south/west} properties, so
 * the Builder Stick's side options work on them. Like a fence side, an arm is only worked out again when the block on that side changes:
 * one the Builder Stick took away stays away, and a stub it added (toward air) stays until something is placed there.
 */
public class PlacedRodBlock extends RodBlock {
	public static final Map<Direction, BooleanProperty> ARMS = CrossCollisionBlock.PROPERTY_BY_DIRECTION;
	/** The same as {@code RodBlock}'s upright hitbox (4 px across, a little wider than the 2 px rod, as the end rod's is). */
	private static final VoxelShape UPRIGHT = Block.column(4.0, 0.0, 16.0);
	/** Each arm's hitbox, as wide as the rod's, at half height, out to the block's edge. */
	private static final Map<Direction, VoxelShape> ARM_SHAPES = Shapes.rotateHorizontal(Block.boxZ(4.0, 6.0, 10.0, 0.0, 8.0));

	private final Item item;
	private final Function<BlockState, VoxelShape> uprightShapes;

	public PlacedRodBlock(Properties properties, Item item) {
		super(properties);
		this.item = item;
		BlockState state = this.stateDefinition.any().setValue(FACING, Direction.UP);
		for (BooleanProperty arm : ARMS.values()) {
			state = state.setValue(arm, false);
		}
		this.registerDefaultState(state);
		this.uprightShapes = this.getShapeForEachState(PlacedRodBlock::uprightShape);
	}

	private static VoxelShape uprightShape(BlockState state) {
		VoxelShape shape = UPRIGHT;
		for (Map.Entry<Direction, BooleanProperty> arm : ARMS.entrySet()) {
			if (state.getValue(arm.getValue())) {
				shape = Shapes.or(shape, ARM_SHAPES.get(arm.getKey()));
			}
		}
		return shape;
	}

	private static boolean isUpright(BlockState state) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Y;
	}

	/** Whether upright {@code rod} reaches out to {@code neighbour} on {@code side}: the same kind of rod, lying along that side's axis. */
	private static boolean reachesTo(BlockState rod, BlockState neighbour, Direction side) {
		return neighbour.is(rod.getBlock()) && neighbour.getValue(FACING).getAxis() == side.getAxis();
	}

	/** Whether the Builder Stick may give upright {@code rod} an arm toward {@code neighbour}: only toward air or the same kind of rod. */
	public static boolean canHaveArm(BlockState rod, BlockState neighbour) {
		return neighbour.isAir() || neighbour.is(rod.getBlock());
	}

	/** {@code state} with each arm worked out from its neighbours if it's upright, or with none if it isn't. */
	public static BlockState withArms(LevelReader level, BlockPos pos, BlockState state) {
		boolean upright = isUpright(state);
		for (Map.Entry<Direction, BooleanProperty> arm : ARMS.entrySet()) {
			Direction side = arm.getKey();
			state = state.setValue(arm.getValue(), upright && reachesTo(state, level.getBlockState(pos.relative(side)), side));
		}
		return state;
	}

	/** Copied from {@code EndRodBlock}: out from the clicked face, flipped when placed against the same rod pointing the same way. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clickedFace = context.getClickedFace();
		BlockState against = context.getLevel().getBlockState(context.getClickedPos().relative(clickedFace.getOpposite()));
		BlockState state = against.is(this) && against.getValue(FACING) == clickedFace
				? this.defaultBlockState().setValue(FACING, clickedFace.getOpposite())
				: this.defaultBlockState().setValue(FACING, clickedFace);
		return withArms(context.getLevel(), context.getClickedPos(), state);
	}

	/** Only the arm on the side that changed is worked out again, as a fence does. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		BlockState updated = super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
		if (updated.is(this) && isUpright(updated) && directionToNeighbour.getAxis().isHorizontal()) {
			return updated.setValue(ARMS.get(directionToNeighbour), reachesTo(updated, neighbourState, directionToNeighbour));
		}
		return updated;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return isUpright(state) ? this.uprightShapes.apply(state) : super.getShape(state, level, pos, context);
	}

	/** Turns the arms with the rod (structures), as {@code CrossCollisionBlock} turns a fence's sides. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return moveArms(super.rotate(state, rotation), state, rotation::rotate);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return moveArms(super.mirror(state, mirror), state, mirror::mirror);
	}

	private static BlockState moveArms(BlockState moved, BlockState original, UnaryOperator<Direction> move) {
		for (Map.Entry<Direction, BooleanProperty> arm : ARMS.entrySet()) {
			moved = moved.setValue(ARMS.get(move.apply(arm.getKey())), original.getValue(arm.getValue()));
		}
		return moved;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.item);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		ARMS.values().forEach(builder::add);
	}
}
