package com.boaringpanda.bettervanillabuilding.block;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bettervanillabuilding.BetterVanillaBuilding;

/**
 * Up to four of the same flower in one block, added like candles: click the flower with another one.
 *
 * <p>A flower is still vanilla's own block with a {@link #FLOWERS} count, as a candle has {@code candles}. One is vanilla's single
 * flower (random offset included). More fill the quarters in a fixed pattern, diagonal first ({@link #PATTERN}), with no random
 * offset so the quarters line up. Loot tables count the flowers, like the candle's.
 */
public final class FlowerClumps {
	public static final IntegerProperty FLOWERS = IntegerProperty.create("flowers", 1, 4);

	/** The flowers that clump. Every flower and mushroom block has the property, but only these use it. */
	public static final TagKey<Block> CLUMPS = TagKey.create(Registries.BLOCK, BetterVanillaBuilding.id("flower_clumps"));

	/**
	 * Where each flower of a clump stands, as offsets from the block's centre in the order they fill: north-west and south-east
	 * (two flowers), then north-east, then south-west.
	 */
	private static final List<Vec3> PATTERN = List.of(
			new Vec3(-0.25, 0.0, -0.25), new Vec3(0.25, 0.0, 0.25), new Vec3(0.25, 0.0, -0.25), new Vec3(-0.25, 0.0, 0.25));
	/** Nudges a click point back into the clicked face, so it lands in the block that was clicked. */
	private static final double INTO_FACE = 0.001;

	private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

	private FlowerClumps() {
	}

	/** Blocks get their properties before they have an id, so this goes by class; {@link #CLUMPS} picks which ones clump. */
	public static boolean has(Block block) {
		return block instanceof FlowerBlock || block instanceof MushroomBlock;
	}

	/** Whether {@code state} is two or more flowers, rather than vanilla's single flower. */
	public static boolean isClump(BlockState state) {
		return state.hasProperty(FLOWERS) && state.getValue(FLOWERS) > 1;
	}

	/** Vanilla's single flower. */
	public static BlockState single(BlockState state) {
		return state.setValue(FLOWERS, 1);
	}

	/** {@code to} with as many flowers as {@code from}, if {@code from} is a clump; otherwise {@code to} unchanged. */
	public static BlockState keepFlowers(BlockState from, BlockState to) {
		return isClump(from) && to.hasProperty(FLOWERS) ? to.setValue(FLOWERS, from.getValue(FLOWERS)) : to;
	}

	/** Where each flower of a clump stands, as offsets from the block's centre (none for a single flower). */
	public static List<Vec3> offsets(BlockState state) {
		return isClump(state) ? PATTERN.subList(0, state.getValue(FLOWERS)) : List.of();
	}

	/**
	 * A clumping flower with room left makes room for the same flower clicked onto it, as a candle does for another candle (sneaking
	 * doesn't add one, as with candles).
	 *
	 * <p>Vanilla also asks this of the block <em>next to</em> the clicked one (clicking the ground beside a flower asks the flower),
	 * and only a click on the flower itself should count. The clicked block isn't known yet when vanilla first asks, so the click point
	 * decides: it has to be in a block with this very state. Two neighbouring flowers in the same state give the same answer anyway.
	 */
	public static boolean takesFlower(BlockState state, BlockPlaceContext context) {
		if (!state.is(CLUMPS) || !state.hasProperty(FLOWERS) || state.getValue(FLOWERS) == 4 || context.isSecondaryUseActive()
				|| !context.getItemInHand().is(state.getBlock().asItem())) {
			return false;
		}
		Vec3 hit = context.getClickLocation().subtract(context.getClickedFace().getUnitVec3().scale(INTO_FACE));
		return context.getLevel().getBlockState(BlockPos.containing(hit)) == state;
	}

	/** The state for placing a flower: the flower already there with one more, or vanilla's single flower when there's none. */
	public static @Nullable BlockState place(BlockPlaceContext context, @Nullable BlockState vanilla) {
		if (vanilla == null || !vanilla.hasProperty(FLOWERS) || !vanilla.is(CLUMPS)) {
			return vanilla;
		}
		BlockState old = context.getLevel().getBlockState(context.getClickedPos());
		return old.is(vanilla.getBlock()) ? old.setValue(FLOWERS, Math.min(old.getValue(FLOWERS) + 1, 4)) : vanilla;
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
