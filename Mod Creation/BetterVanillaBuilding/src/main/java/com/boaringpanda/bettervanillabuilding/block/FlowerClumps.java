package com.boaringpanda.bettervanillabuilding.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bettervanillabuilding.BetterVanillaBuilding;

/**
 * Up to four of the same flower in one block, one in each quarter, added like candles.
 *
 * <p>A flower is still vanilla's own block with a property per quarter. With every quarter off it's vanilla's single flower (random
 * offset included). A second flower goes in the quarter you aim at (the same aim as {@link AimedSegments}) and the first one moves to
 * the quarter opposite it; each next flower goes in the aimed quarter. A clump has no random offset, so the quarters line up. Loot
 * tables count the quarters, like the candle's.
 */
public final class FlowerClumps {
	public static final BooleanProperty NORTH_WEST = BooleanProperty.create("north_west");
	public static final BooleanProperty NORTH_EAST = BooleanProperty.create("north_east");
	public static final BooleanProperty SOUTH_EAST = BooleanProperty.create("south_east");
	public static final BooleanProperty SOUTH_WEST = BooleanProperty.create("south_west");

	/** The flowers that clump. Every flower and mushroom block has the properties, but only these use them. */
	public static final TagKey<Block> CLUMPS = TagKey.create(Registries.BLOCK, BetterVanillaBuilding.id("flower_clumps"));

	/** Quarters as {@link AimedSegments} names them: north = north-west, east = north-east, south = south-east, west = south-west. */
	private static final Map<Direction, BooleanProperty> QUARTERS = Map.of(
			Direction.NORTH, NORTH_WEST, Direction.EAST, NORTH_EAST, Direction.SOUTH, SOUTH_EAST, Direction.WEST, SOUTH_WEST);
	/** How far each quarter's centre is from the block's centre, in blocks. */
	private static final Map<Direction, Vec3> OFFSETS = Map.of(
			Direction.NORTH, new Vec3(-0.25, 0.0, -0.25), Direction.EAST, new Vec3(0.25, 0.0, -0.25),
			Direction.SOUTH, new Vec3(0.25, 0.0, 0.25), Direction.WEST, new Vec3(-0.25, 0.0, 0.25));
	private static final Direction[] IN_ORDER = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

	private FlowerClumps() {
	}

	/** Blocks get their properties before they have an id, so this goes by class; {@link #CLUMPS} picks which ones clump. */
	public static boolean has(Block block) {
		return block instanceof FlowerBlock || block instanceof MushroomBlock;
	}

	/** Whether {@code state} is two or more flowers (any quarter set), rather than vanilla's single flower. */
	public static boolean isClump(BlockState state) {
		if (!state.hasProperty(NORTH_WEST)) {
			return false;
		}
		for (BooleanProperty quarter : QUARTERS.values()) {
			if (state.getValue(quarter)) {
				return true;
			}
		}
		return false;
	}

	/** Vanilla's single flower: every quarter off. */
	public static BlockState single(BlockState state) {
		for (BooleanProperty quarter : QUARTERS.values()) {
			state = state.setValue(quarter, false);
		}
		return state;
	}

	/** {@code to} with the quarters of {@code from}, if {@code from} is a clump; otherwise {@code to} unchanged. */
	public static BlockState keepQuarters(BlockState from, BlockState to) {
		if (!isClump(from) || !to.hasProperty(NORTH_WEST)) {
			return to;
		}
		for (BooleanProperty quarter : QUARTERS.values()) {
			to = to.setValue(quarter, from.getValue(quarter));
		}
		return to;
	}

	/** Where each flower of a clump stands, as offsets from the block's centre. */
	public static List<Vec3> offsets(BlockState state) {
		List<Vec3> offsets = new ArrayList<>(4);
		for (Direction quarter : IN_ORDER) {
			if (state.getValue(QUARTERS.get(quarter))) {
				offsets.add(OFFSETS.get(quarter));
			}
		}
		return offsets;
	}

	/**
	 * A clumping flower makes room for the same flower in a free quarter, as a candle makes room for another candle (sneaking doesn't
	 * add one, as with candles). A single flower takes any quarter.
	 */
	public static boolean takesFlower(BlockState state, BlockPlaceContext context) {
		return state.is(CLUMPS) && state.hasProperty(NORTH_WEST) && !context.isSecondaryUseActive()
				&& context.getItemInHand().is(state.getBlock().asItem())
				&& !state.getValue(QUARTERS.get(AimedSegments.aimedQuarter(context)));
	}

	/**
	 * The state for placing a flower: the flower already there with the aimed quarter added (a single one also moves to the opposite
	 * quarter), or vanilla's single flower when there's none.
	 */
	public static @Nullable BlockState place(BlockPlaceContext context, @Nullable BlockState vanilla) {
		if (vanilla == null || !vanilla.hasProperty(NORTH_WEST) || !vanilla.is(CLUMPS)) {
			return vanilla;
		}
		BlockState old = context.getLevel().getBlockState(context.getClickedPos());
		if (!old.is(vanilla.getBlock())) {
			return vanilla;
		}
		Direction aimed = AimedSegments.aimedQuarter(context);
		if (!isClump(old)) {
			old = old.setValue(QUARTERS.get(aimed.getOpposite()), true);
		}
		return old.setValue(QUARTERS.get(aimed), true);
	}

	/** A clump's shape: vanilla's shape of one flower in each filled quarter, as one box around them all (as candles have). */
	public static VoxelShape shape(BlockState state, VoxelShape vanilla) {
		if (!isClump(state)) {
			return vanilla;
		}
		return SHAPES.computeIfAbsent(state, s -> {
			VoxelShape shape = Shapes.empty();
			for (Vec3 offset : offsets(s)) {
				shape = Shapes.or(shape, vanilla.move(offset));
			}
			return shape.singleEncompassing();
		});
	}
}
