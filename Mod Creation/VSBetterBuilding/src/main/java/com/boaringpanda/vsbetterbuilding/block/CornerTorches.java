package com.boaringpanda.vsbetterbuilding.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.vsbetterbuilding.VSBetterBuilding;

/**
 * Crouch to put a torch where the cursor is: in the aimed quarter of a block's top (up to four torches), or the aimed half of its side
 * (two wall torches side by side). Only the same torch joins a group, and a player breaks only the torch they aim at.
 *
 * <p>Torches are still vanilla's own blocks: standing torches get a property per quarter (all off = vanilla's centred torch) and wall
 * torches a {@link Side} ({@code middle} = vanilla's). A block with a menu (a crafting table) has to be crouch-clicked to place
 * anything on it at all, so there aiming at the middle ({@link #aimsAtMiddle}) gives vanilla's centred torch. Loot tables count the
 * torches.
 */
public final class CornerTorches {
	/** Where a wall torch sits along its wall, as seen facing the wall. */
	public enum Side implements StringRepresentable {
		// Vanilla's middle first: blocks take their default state from the first value of each property.
		MIDDLE("middle"),
		LEFT("left"),
		RIGHT("right"),
		BOTH("both");

		private final String name;

		Side(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final BooleanProperty NORTH_WEST = BooleanProperty.create("north_west");
	public static final BooleanProperty NORTH_EAST = BooleanProperty.create("north_east");
	public static final BooleanProperty SOUTH_EAST = BooleanProperty.create("south_east");
	public static final BooleanProperty SOUTH_WEST = BooleanProperty.create("south_west");
	public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);

	/** The torches that spread out. Every torch block has the properties, but only these use them. */
	public static final TagKey<Block> TORCHES = TagKey.create(Registries.BLOCK, VSBetterBuilding.id("corner_torches"));

	/** Quarters as {@link AimedSegments} names them: north = north-west, east = north-east, south = south-east, west = south-west. */
	private static final Map<Direction, BooleanProperty> CORNERS = Map.of(
			Direction.NORTH, NORTH_WEST, Direction.EAST, NORTH_EAST, Direction.SOUTH, SOUTH_EAST, Direction.WEST, SOUTH_WEST);
	private static final Direction[] IN_ORDER = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	/** Each quarter's centre (and each half of a wall's) is this far from the block's centre. */
	private static final double SPREAD = 0.25;
	/** The middle spot of a menu block's face reaches this far from the centre (3 px: the middle 6×6 px). */
	private static final double MIDDLE = 3.0 / 16.0;

	private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

	private CornerTorches() {
	}

	/** Standing torch, soul torch and copper torch. Blocks get their properties before they have an id, so this goes by class. */
	public static boolean hasCorners(Block block) {
		return block instanceof TorchBlock && !(block instanceof WallTorchBlock);
	}

	/** Wall torch, soul wall torch and copper wall torch. */
	public static boolean hasSides(Block block) {
		return block instanceof WallTorchBlock;
	}

	/** Whether {@code state} is a torch moved off the middle (any quarter or side), rather than vanilla's. */
	public static boolean isSpread(BlockState state) {
		if (state.hasProperty(SIDE)) {
			return state.getValue(SIDE) != Side.MIDDLE;
		}
		if (!state.hasProperty(NORTH_WEST)) {
			return false;
		}
		for (BooleanProperty corner : CORNERS.values()) {
			if (state.getValue(corner)) {
				return true;
			}
		}
		return false;
	}

	/** Vanilla's centred torch: every quarter off. */
	public static BlockState centred(BlockState state) {
		for (BooleanProperty corner : CORNERS.values()) {
			state = state.setValue(corner, false);
		}
		return state;
	}

	/** Where each torch of a group stands, as offsets from where vanilla's single torch would be (none for vanilla's). */
	public static List<Vec3> offsets(BlockState state) {
		List<Vec3> offsets = new ArrayList<>(4);
		if (state.hasProperty(SIDE)) {
			Direction right = state.getValue(WallTorchBlock.FACING).getCounterClockWise();
			Side side = state.getValue(SIDE);
			if (side == Side.LEFT || side == Side.BOTH) {
				offsets.add(new Vec3(-right.getStepX() * SPREAD, 0.0, -right.getStepZ() * SPREAD));
			}
			if (side == Side.RIGHT || side == Side.BOTH) {
				offsets.add(new Vec3(right.getStepX() * SPREAD, 0.0, right.getStepZ() * SPREAD));
			}
		} else if (state.hasProperty(NORTH_WEST)) {
			for (Direction quarter : IN_ORDER) {
				if (state.getValue(CORNERS.get(quarter))) {
					offsets.add(corner(quarter));
				}
			}
		}
		return offsets;
	}

	/** The centre of a quarter, named as {@link AimedSegments} does. */
	private static Vec3 corner(Direction quarter) {
		return switch (quarter) {
			case NORTH -> new Vec3(-SPREAD, 0.0, -SPREAD);
			case EAST -> new Vec3(SPREAD, 0.0, -SPREAD);
			case SOUTH -> new Vec3(SPREAD, 0.0, SPREAD);
			default -> new Vec3(-SPREAD, 0.0, SPREAD);
		};
	}

	/**
	 * Where a crouch-placed torch goes vanilla's way: onto a face that isn't full, or at the middle of a block with a menu (a crafting
	 * table or furnace can't be clicked without crouching, so crouching is the only way to put any torch on it).
	 */
	private static boolean staysInMiddle(BlockPlaceContext context, BlockPos supportPos, Direction face) {
		Level level = context.getLevel();
		BlockState support = level.getBlockState(supportPos);
		return !support.isFaceSturdy(level, supportPos, face, SupportType.FULL)
				|| support.getMenuProvider(level, supportPos) != null && aimsAtMiddle(context, face);
	}

	/** Whether the click is in the middle 6×6 px of the top face, or the middle 6 px of a wall (left to right). */
	private static boolean aimsAtMiddle(BlockPlaceContext context, Direction face) {
		BlockPos pos = context.getClickedPos();
		Vec3 hit = context.getClickLocation();
		double x = Math.abs(hit.x - pos.getX() - 0.5);
		double z = Math.abs(hit.z - pos.getZ() - 0.5);
		if (face == Direction.UP) {
			return x <= MIDDLE && z <= MIDDLE;
		}
		return (face.getAxis() == Direction.Axis.X ? z : x) <= MIDDLE;
	}

	/** The half of the wall the click points at, as seen facing the wall. */
	private static Side aimedSide(BlockPlaceContext context, Direction facing) {
		BlockPos pos = context.getClickedPos();
		Vec3 hit = context.getClickLocation();
		Direction right = facing.getCounterClockWise();
		double along = (hit.x - pos.getX() - 0.5) * right.getStepX() + (hit.z - pos.getZ() - 0.5) * right.getStepZ();
		return along >= 0.0 ? Side.RIGHT : Side.LEFT;
	}

	/**
	 * A group of torches makes room for the same torch crouch-clicked onto the block it stands on (the top for standing torches, the
	 * wall behind for wall torches), in a free quarter or the free side.
	 *
	 * <p>Only when vanilla asks about the space in front of the clicked block ({@code !replacingClickedOnBlock()}): clicking a torch
	 * itself aims at its own, filled, spot anyway.
	 */
	public static boolean takesTorch(BlockState state, BlockPlaceContext context) {
		if (!state.is(TORCHES) || !isSpread(state) || !context.isSecondaryUseActive() || context.replacingClickedOnBlock()
				|| !context.getItemInHand().is(state.getBlock().asItem())) {
			return false;
		}
		Direction face = context.getClickedFace();
		if (state.hasProperty(SIDE)) {
			Side side = state.getValue(SIDE);
			return face == state.getValue(WallTorchBlock.FACING) && side != Side.BOTH && aimedSide(context, face) != side;
		}
		return face == Direction.UP && !state.getValue(CORNERS.get(AimedSegments.aimedQuarter(context)));
	}

	/**
	 * The state for placing a torch: the group already there with the aimed spot added, or (crouching, on a full face, and not the
	 * middle of a block with a menu) a new torch in the aimed spot. Anything else is vanilla's.
	 */
	public static @Nullable BlockState place(BlockPlaceContext context, @Nullable BlockState vanilla) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState old = level.getBlockState(pos);
		if (old.is(TORCHES) && isSpread(old) && context.getItemInHand().is(old.getBlock().asItem())) {
			return old.hasProperty(SIDE)
					? old.setValue(SIDE, Side.BOTH)
					: old.setValue(CORNERS.get(AimedSegments.aimedQuarter(context)), true);
		}
		if (vanilla == null || !vanilla.is(TORCHES) || !context.isSecondaryUseActive()) {
			return vanilla;
		}
		Direction face = context.getClickedFace();
		if (staysInMiddle(context, pos.relative(face.getOpposite()), face)) {
			return vanilla;
		}
		if (vanilla.hasProperty(SIDE)) {
			return face == vanilla.getValue(WallTorchBlock.FACING) ? vanilla.setValue(SIDE, aimedSide(context, face)) : vanilla;
		}
		return vanilla.hasProperty(NORTH_WEST) && face == Direction.UP
				? vanilla.setValue(CORNERS.get(AimedSegments.aimedQuarter(context)), true)
				: vanilla;
	}

	/** Each torch of {@code state} on its own, as the state of a group of just that torch (none for vanilla's single torch). */
	public static List<BlockState> torches(BlockState state) {
		List<BlockState> torches = new ArrayList<>(4);
		if (state.hasProperty(SIDE)) {
			Side side = state.getValue(SIDE);
			if (side == Side.BOTH) {
				torches.add(state.setValue(SIDE, Side.LEFT));
				torches.add(state.setValue(SIDE, Side.RIGHT));
			} else if (side != Side.MIDDLE) {
				torches.add(state);
			}
		} else if (state.hasProperty(NORTH_WEST)) {
			BlockState none = centred(state);
			for (Direction quarter : IN_ORDER) {
				BooleanProperty corner = CORNERS.get(quarter);
				if (state.getValue(corner)) {
					torches.add(none.setValue(corner, true));
				}
			}
		}
		return torches;
	}

	/**
	 * The torch of a group of two or more that the player's cursor is on ({@link #torches}): the one whose shape the hit is in, or the
	 * nearest one. Null if it isn't such a group or the cursor isn't on it.
	 */
	public static @Nullable BlockState aimedTorch(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state) {
		List<BlockState> torches = torches(state);
		if (player == null || torches.size() < 2) {
			return null;
		}
		HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK || !blockHit.getBlockPos().equals(pos)) {
			return null;
		}
		Vec3 point = blockHit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		CollisionContext context = CollisionContext.of(player);
		BlockState nearest = torches.getFirst();
		double nearestDistance = Double.MAX_VALUE;
		for (BlockState torch : torches) {
			VoxelShape shape = torch.getShape(level, pos, context);
			double distance = shape.bounds().getCenter().distanceToSqr(point);
			if (shape.toAabbs().stream().anyMatch(box -> box.inflate(1.0E-4).contains(point))) {
				return torch;
			}
			if (distance < nearestDistance) {
				nearest = torch;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	/** Takes {@code torch} (one of {@link #torches}) out of the group at {@code pos}; the others stay. */
	public static boolean breakTorch(Level level, BlockPos pos, BlockState state, BlockState torch, int flags) {
		BlockState rest;
		if (state.hasProperty(SIDE)) {
			rest = state.setValue(SIDE, torch.getValue(SIDE) == Side.LEFT ? Side.RIGHT : Side.LEFT);
		} else {
			rest = state;
			for (BooleanProperty corner : CORNERS.values()) {
				if (torch.getValue(corner)) {
					rest = rest.setValue(corner, false);
				}
			}
		}
		return level.setBlock(pos, rest, flags);
	}

	/** A group's shape: vanilla's torch shape at each torch, kept apart so the block top between them can still be clicked. */
	public static VoxelShape shape(BlockState state, VoxelShape vanilla) {
		if (!isSpread(state)) {
			return vanilla;
		}
		return SHAPES.computeIfAbsent(state, s -> {
			VoxelShape shape = Shapes.empty();
			for (Vec3 offset : offsets(s)) {
				shape = Shapes.or(shape, vanilla.move(offset));
			}
			return shape;
		});
	}
}
