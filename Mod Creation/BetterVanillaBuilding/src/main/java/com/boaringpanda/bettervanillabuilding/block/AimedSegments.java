package com.boaringpanda.bettervanillabuilding.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Leaf litter, pink petals and wildflowers go in the quarter of the block you aim at, instead of vanilla's fixed order.
 *
 * <p>Vanilla stores only {@code facing} and an amount: piece 1 sits in the {@code facing} quarter (north = north-west, east =
 * north-east, south = south-east, west = south-west) and each next piece one quarter counter-clockwise. {@link #ORDER} adds the order
 * the pieces fill in, so the pieces present are the first {@code amount} of that order. Piece 1 is always the first one placed, so a
 * piece never changes model (each quarter has its own) when another is added. The amount stays the real count, so loot, bone meal,
 * worldgen and rotation are vanilla's.
 */
public final class AimedSegments {
	/** Pieces are numbered 1-4 from {@code facing}, counter-clockwise. */
	public enum Order implements StringRepresentable {
		// Vanilla's order first: blocks take their default state from the first value of each property.
		VANILLA("1234", 1, 2, 3, 4),
		ACROSS("1342", 1, 3, 4, 2),
		CLOCKWISE("1423", 1, 4, 2, 3);

		private final String name;
		private final int[] pieces;

		Order(String name, int... pieces) {
			this.name = name;
			this.pieces = pieces;
		}

		/** The pieces present when {@code amount} are placed, as a bit per piece (bit 0 = piece 1). */
		private int pieces(int amount) {
			int bits = 0;
			for (int i = 0; i < amount; i++) {
				bits |= 1 << (pieces[i] - 1);
			}
			return bits;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Order> ORDER = EnumProperty.create("segment_order", Order.class);
	private static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Nudges a click point back into the clicked face, so the side of a piece counts as that piece. */
	private static final double INTO_FACE = 0.001;

	private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

	private AimedSegments() {
	}

	/** Leaf litter, pink petals and wildflowers: every {@link SegmentableBlock} in 26.3. */
	public static boolean has(Block block) {
		return block instanceof LeafLitterBlock || block instanceof FlowerBedBlock;
	}

	/**
	 * What all three piles stand on: any full top face (vanilla leaf litter's own rule) or any leaves, whose support shape is empty
	 * in vanilla so they never count as full.
	 */
	public static boolean supports(BlockState below, BlockGetter level, BlockPos belowPos) {
		return below.isFaceSturdy(level, belowPos, Direction.UP) || below.is(BlockTags.LEAVES);
	}

	/** The quarter the click points at, as the {@code facing} whose piece 1 sits there. Flower clumps aim the same way. */
	static Direction aimedQuarter(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Vec3 hit = context.getClickLocation().subtract(context.getClickedFace().getUnitVec3().scale(INTO_FACE));
		boolean east = hit.x - pos.getX() >= 0.5;
		boolean south = hit.z - pos.getZ() >= 0.5;
		return south ? (east ? Direction.SOUTH : Direction.WEST) : (east ? Direction.EAST : Direction.NORTH);
	}

	/** Which piece (1-4) of {@code state} sits in {@code quarter}. */
	private static int piece(BlockState state, Direction quarter) {
		int piece = 1;
		for (Direction d = state.getValue(FACING); d != quarter; d = d.getCounterClockWise()) {
			piece++;
		}
		return piece;
	}

	private static IntegerProperty amount(BlockState state) {
		return ((SegmentableBlock) state.getBlock()).getSegmentAmountProperty();
	}

	private static int pieces(BlockState state) {
		return state.getValue(ORDER).pieces(state.getValue(amount(state)));
	}

	/** Whether the aimed quarter of {@code state} is still empty. */
	public static boolean isFree(BlockState state, BlockPlaceContext context) {
		return (pieces(state) & 1 << (piece(state, aimedQuarter(context)) - 1)) == 0;
	}

	/**
	 * The state for placing a piece: into the aimed quarter of the pile already there (vanilla's result is that pile with one more
	 * piece, the next counter-clockwise), or a new pile with piece 1 in the aimed quarter.
	 */
	public static BlockState place(BlockPlaceContext context, BlockState vanilla) {
		BlockState old = context.getLevel().getBlockState(context.getClickedPos());
		if (!old.is(vanilla.getBlock())) {
			return vanilla.setValue(FACING, aimedQuarter(context));
		}
		int wanted = pieces(old) | 1 << (piece(old, aimedQuarter(context)) - 1);
		int amount = Integer.bitCount(wanted);
		for (Order order : Order.values()) {
			if (order.pieces(amount) == wanted) {
				return old.setValue(amount(old), amount).setValue(ORDER, order);
			}
		}
		return vanilla;
	}

	/**
	 * Vanilla's shape for a pile in vanilla's order. Any other order gets the same kind of shape (one box around every piece, like
	 * {@code SegmentableBlock.getShapeCalculator}) around the pieces it really has.
	 */
	public static VoxelShape shape(BlockState state, VoxelShape vanilla) {
		if (state.getValue(ORDER) == Order.VANILLA) {
			return vanilla;
		}
		return SHAPES.computeIfAbsent(state, s -> {
			Map<Direction, VoxelShape> quarters = Shapes.rotateHorizontal(
					Block.box(0.0, 0.0, 0.0, 8.0, ((SegmentableBlock) s.getBlock()).getShapeHeight(), 8.0));
			int pieces = pieces(s);
			VoxelShape shape = Shapes.empty();
			Direction quarter = s.getValue(FACING);
			for (int i = 0; i < 4; i++, quarter = quarter.getCounterClockWise()) {
				if ((pieces & 1 << i) != 0) {
					shape = Shapes.or(shape, quarters.get(quarter));
				}
			}
			return shape.singleEncompassing();
		});
	}
}
