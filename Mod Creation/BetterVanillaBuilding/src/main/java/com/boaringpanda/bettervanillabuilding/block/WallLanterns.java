package com.boaringpanda.bettervanillabuilding.block;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bettervanillabuilding.BetterVanillaBuilding;

/**
 * Lanterns on walls: a lantern hanging from a small bracket (drawn in its chain's colours) against the side of a block. Where you
 * click picks the form: a wall's side gives a wall lantern, a ceiling's underside a hanging one, a floor's top a standing one.
 *
 * <p>Lanterns are still vanilla's own blocks with a {@link Wall} property ({@code none} = vanilla's standing or hanging lantern), so
 * light, loot, waterlogging and copper weathering and waxing are unchanged.
 */
public final class WallLanterns {
	/** The side the wall is on, seen from the lantern. */
	public enum Wall implements StringRepresentable {
		// Vanilla's lantern first: blocks take their default state from the first value of each property.
		NONE("none", null),
		NORTH("north", Direction.NORTH),
		EAST("east", Direction.EAST),
		SOUTH("south", Direction.SOUTH),
		WEST("west", Direction.WEST);

		private final String name;
		private final @Nullable Direction direction;

		Wall(String name, @Nullable Direction direction) {
			this.name = name;
			this.direction = direction;
		}

		/** The wall's side, or null for vanilla's lantern. */
		public @Nullable Direction direction() {
			return direction;
		}

		static Wall of(Direction direction) {
			return switch (direction) {
				case NORTH -> NORTH;
				case EAST -> EAST;
				case SOUTH -> SOUTH;
				case WEST -> WEST;
				default -> NONE;
			};
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Wall> WALL = EnumProperty.create("wall", Wall.class);

	/** The lanterns that go on walls. Every lantern block has the property, but only these have a wall model. */
	public static final TagKey<Block> LANTERNS = TagKey.create(Registries.BLOCK, BetterVanillaBuilding.id("wall_lanterns"));

	/**
	 * Wall on the north: vanilla's standing lantern 2 px up, hanging from the tip of a flat bracket: a 3x5 plate on the wall (y 9-14),
	 * a top arm (y 13-14) and a curved brace stepping up from the plate's bottom to the arm. Nothing reaches the top pixel, so there's
	 * a gap under any block above.
	 */
	private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
			Block.column(6.0, 2.0, 9.0),
			Block.column(4.0, 9.0, 11.0),
			Block.box(6.5, 9.0, 0.0, 9.5, 14.0, 1.0),
			Block.box(7.5, 13.0, 1.0, 8.5, 14.0, 9.0),
			Block.box(7.5, 10.0, 1.0, 8.5, 11.0, 2.0),
			Block.box(7.5, 11.0, 2.0, 8.5, 12.0, 4.0),
			Block.box(7.5, 12.0, 4.0, 8.5, 13.0, 6.0)));

	private WallLanterns() {
	}

	/** Every lantern, vanilla's and copper's. Blocks get their properties before they have an id, so this goes by class. */
	public static boolean has(Block block) {
		return block instanceof LanternBlock;
	}

	/** The wall's side for a lantern on a wall, else null (vanilla's lantern, or not a lantern). */
	public static @Nullable Direction wall(BlockState state) {
		return state.hasProperty(WALL) ? state.getValue(WALL).direction() : null;
	}

	/**
	 * The lantern for the face the player clicked: a side gives a wall lantern, the underside a hanging one, the top a standing one.
	 * Null when that one can't stay there, or the click went into a replaceable block (grass, a lily pad), leaving vanilla's choice
	 * (by where the player looks).
	 */
	public static @Nullable BlockState place(BlockPlaceContext context, Block block) {
		BlockState state = block.defaultBlockState();
		if (!state.is(LANTERNS) || context.replacingClickedOnBlock()) {
			return null;
		}
		Direction face = context.getClickedFace();
		state = face.getAxis() == Direction.Axis.Y
				? state.setValue(LanternBlock.HANGING, face == Direction.DOWN)
				: state.setValue(WALL, Wall.of(face.getOpposite()));
		BlockPos pos = context.getClickedPos();
		if (!state.canSurvive(context.getLevel(), pos)) {
			return null;
		}
		return state.setValue(LanternBlock.WATERLOGGED, context.getLevel().getFluidState(pos).is(Fluids.WATER));
	}

	/** The wall torch's rule: the block behind has a full, sturdy face toward the lantern. */
	public static boolean survives(Direction wall, LevelReader level, BlockPos pos) {
		BlockPos behind = pos.relative(wall);
		return level.getBlockState(behind).isFaceSturdy(level, behind, wall.getOpposite());
	}

	public static VoxelShape shape(Direction wall) {
		return SHAPES.get(wall);
	}

	public static BlockState rotate(BlockState state, Rotation rotation) {
		Direction wall = wall(state);
		return wall == null ? state : state.setValue(WALL, Wall.of(rotation.rotate(wall)));
	}

	public static BlockState mirror(BlockState state, Mirror mirror) {
		Direction wall = wall(state);
		return wall == null ? state : state.setValue(WALL, Wall.of(mirror.mirror(wall)));
	}
}
