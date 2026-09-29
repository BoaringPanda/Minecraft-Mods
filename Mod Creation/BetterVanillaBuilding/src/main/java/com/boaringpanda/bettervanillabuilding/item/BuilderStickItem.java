package com.boaringpanda.bettervanillabuilding.item;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import com.boaringpanda.bettervanillabuilding.block.LockedBlocks;
import com.boaringpanda.bettervanillabuilding.block.MixedSlabBlockEntity;
import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;
import com.boaringpanda.bettervanillabuilding.block.PlacedRodBlock;
import com.boaringpanda.bettervanillabuilding.block.Rainbow;
import com.boaringpanda.bettervanillabuilding.block.StackedHeads;
import com.boaringpanda.bettervanillabuilding.entity.RainbowCushions;

/**
 * Works like vanilla's {@code DebugStickItem}, limited to a few blocks and a few {@link Option}s on each ({@link #optionsFor}): the sides
 * of fences, walls, glass panes and bars; a fence gate's facing and height; a door's facing and hinge; a trapdoor's facing, and an iron
 * trapdoor's open state; a stair's facing, shape and half; a slab's half (never double); the axis of chains, logs and a few pillar blocks; a
 * rod's direction, and an upright placed rod's arms; the facing of pistons (retracted), dispensers, droppers, observers, comparators,
 * repeaters, buttons, signs, banners, heads, stonecutters, grindstones and bells; a hopper's spout; a rail's shape and rotation; a copper
 * golem statue's facing and pose; and wool, wool stairs and slabs, carpets, stained glass and panes, beds and banners fading through every
 * colour like a jeb_ sheep (as well as their other options; a wool slab inside a mixed slab fades on its own, the half under the cursor).
 * Left click selects the block's next option (sneak: the previous one), right
 * click changes it. Both show an action-bar message worded like the debug stick's. It never breaks a block, and on any other block both
 * clicks only show "Can not be used on this block"; a right click there doesn't open doors, chests etc. either.
 * <p>
 * A changed fence, pane, bars, wall, stair, fence gate, placed rod or rail is {@link LockedBlocks locked}, so its neighbours never reshape
 * it afterwards.
 * <p>
 * The entities are the armour stand, the cushion and the item frame ({@link #onAttackEntity}, {@link #onUseEntity}): a stand's facing,
 * turned like a sign, a cushion's fade, and whether a frame shows, without ever hitting them. Every other entity is left to vanilla.
 */
public class BuilderStickItem extends Item {
	private static final String KEY = "item.bettervanillabuilding.builder_stick";
	/** The debug stick's flags: tell clients, but don't make neighbours (or this block) recompute their shape. */
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	/**
	 * Holding left click attacks again every tick in survival (every block "breaks" instantly) and every 5 ticks in creative, so a left
	 * click only acts this many ticks after the last one. Holding the button then steps through options at the creative debug stick's pace.
	 */
	private static final int REPEAT_TICKS = 5;
	private static final Map<Player, Long> LAST_LEFT_CLICK = new WeakHashMap<>();

	/** A right click's change to the clicked block (server side). */
	@FunctionalInterface
	private interface Change {
		BlockState apply(Level level, BlockPos pos, BlockState state, Player player);
	}

	/** One thing the stick changes on a block: its name in messages, its current value as shown, and what a right click does to it. */
	private record Option(String name, Function<BlockState, Component> value, Change change) {
	}

	/**
	 * Gates, doors, trapdoors, stairs, comparators, repeaters, buttons, wall signs, wall hanging signs, copper golem statues, stonecutters,
	 * grindstones and bells share vanilla's horizontal {@code FACING}. Turns clockwise (sneak: counter-clockwise).
	 */
	private static final Option FACING = new Option("facing",
			state -> Component.literal(state.getValue(HorizontalDirectionalBlock.FACING).getSerializedName()),
			(level, pos, state, player) -> {
				Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
				return state.setValue(HorizontalDirectionalBlock.FACING,
						player.isSecondaryUseActive() ? facing.getCounterClockWise() : facing.getClockWise());
			});

	/** An iron trapdoor can't be opened by hand, so the stick does it, with vanilla's sound (its {@code playSound} is protected). */
	private static final Option IRON_TRAPDOOR_STATE = new Option("state",
			state -> word(state.getValue(TrapDoorBlock.OPEN) ? "open" : "closed"),
			(level, pos, state, player) -> {
				boolean open = !state.getValue(TrapDoorBlock.OPEN);
				level.playSound(null, pos, open ? BlockSetType.IRON.trapdoorOpen() : BlockSetType.IRON.trapdoorClose(), SoundSource.BLOCKS,
						1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
				level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
				return state.setValue(TrapDoorBlock.OPEN, open);
			});

	private static final List<Option> SIDES = Direction.Plane.HORIZONTAL.stream()
			.map(side -> flag(side.getSerializedName(), CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(side), "on", "off"))
			.toList();
	private static final List<Option> WALL_SIDES = Direction.Plane.HORIZONTAL.stream().map(BuilderStickItem::wallSide).toList();
	private static final List<Option> FENCE_GATE = List.of(FACING, flag("height", FenceGateBlock.IN_WALL, "down", "up"));
	private static final List<Option> DOOR = List.of(FACING, cycle("hinge", DoorBlock.HINGE, BuilderStickItem::word));
	/** Trapdoors, comparators, repeaters, stonecutters, grindstones (which float, on a wall too) and bells on the floor or ceiling. */
	private static final List<Option> TURNS = List.of(FACING);
	private static final List<Option> IRON_TRAPDOOR = List.of(FACING, IRON_TRAPDOOR_STATE);
	private static final List<Option> STAIRS = List.of(FACING, cycle("shape", StairBlock.SHAPE, BuilderStickItem::word),
			cycle("half", StairBlock.HALF, BuilderStickItem::word));
	/** Top and bottom only: the stick never makes a double slab. */
	private static final List<Option> SLAB = List.of(new Option("half", state -> word(SlabBlock.TYPE.getName(state.getValue(SlabBlock.TYPE))),
			(level, pos, state, player) -> state.setValue(SlabBlock.TYPE,
					state.getValue(SlabBlock.TYPE) == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP)));
	/** Chains and {@link #isPillar pillars}. */
	private static final List<Option> AXIS = List.of(cycle("axis", RotatedPillarBlock.AXIS, Component::literal));
	/**
	 * All six directions: end rods, lightning rods, pistons, dispensers, droppers and observers (all share vanilla's
	 * {@code DirectionalBlock.FACING}).
	 */
	private static final Option ALL_DIRECTIONS_FACING = cycle("facing", DirectionalBlock.FACING, Component::literal);
	private static final List<Option> ALL_DIRECTIONS = List.of(ALL_DIRECTIONS_FACING);
	/** This mod's placed sticks, blaze rods and breeze rods: all six directions, then its arms worked out again (none unless upright). */
	private static final Option PLACED_ROD_FACING = new Option("facing", ALL_DIRECTIONS_FACING.value(), (level, pos, state, player) ->
			PlacedRodBlock.withArms(level, pos, ALL_DIRECTIONS_FACING.change().apply(level, pos, state, player)));
	private static final List<Option> PLACED_ROD = List.of(PLACED_ROD_FACING);
	/** An upright placed rod also has its four arms. */
	private static final List<Option> UPRIGHT_PLACED_ROD = List.of(PLACED_ROD_FACING, rodArm(Direction.NORTH), rodArm(Direction.EAST),
			rodArm(Direction.SOUTH), rodArm(Direction.WEST));
	/** Buttons, wall signs and wall hanging signs only turn onto a side that can hold them. */
	private static final List<Option> TURNS_SUPPORTED = List.of(supported(FACING));
	/**
	 * A bell on a wall turns only onto a side with a wall to hang from, then hangs between two walls if the side opposite is solid too, as
	 * placing it there would ({@code BellBlock.getStateForPlacement}).
	 */
	private static final List<Option> WALL_BELL = List.of(new Option("facing", FACING.value(), (level, pos, state, player) -> {
		BlockState turned = TURNS_SUPPORTED.getFirst().change().apply(level, pos, state, player);
		if (turned == state) {
			return state;
		}
		Direction behind = turned.getValue(BellBlock.FACING).getOpposite();
		return turned.setValue(BellBlock.ATTACHMENT, FaceAttachedHorizontalDirectionalBlock.canAttach(level, pos, behind)
				? BellAttachType.DOUBLE_WALL : BellAttachType.SINGLE_WALL);
	}));

	/** A hopper's spout: down, or out of a side (it turns only then). */
	private static final List<Option> HOPPER = List.of(
			new Option("shape", state -> word(state.getValue(HopperBlock.FACING) == Direction.DOWN ? "down" : "side"),
					// Going sideways, the spout points the way the player looks.
					(level, pos, state, player) -> state.setValue(HopperBlock.FACING,
							state.getValue(HopperBlock.FACING) == Direction.DOWN ? player.getDirection() : Direction.DOWN)),
			new Option("facing", state -> Component.literal(state.getValue(HopperBlock.FACING).getSerializedName()),
					(level, pos, state, player) -> {
						Direction facing = state.getValue(HopperBlock.FACING);
						return facing == Direction.DOWN ? state : state.setValue(HopperBlock.FACING,
								player.isSecondaryUseActive() ? facing.getCounterClockWise() : facing.getClockWise());
					}));

	/** Vanilla's own rail rotation (curves and slopes turn too), shown as the rail's shape. Never onto a slope with nothing to rise onto. */
	private static final Option RAIL_ROTATION = supported(new Option("rotation",
			state -> word(state.getValue(((BaseRailBlock) state.getBlock()).getShapeProperty()).getSerializedName()),
			(level, pos, state, player) -> state.rotate(player.isSecondaryUseActive() ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90)));
	private static final List<Option> RAIL = List.of(supported(cycle("shape", RailBlock.SHAPE, BuilderStickItem::word)), RAIL_ROTATION);
	/** Powered, activator and detector rails. */
	private static final List<Option> STRAIGHT_RAIL = List.of(RAIL_ROTATION);

	/**
	 * A {@code ROTATION_16} value as the compass point a sign's front faces: 0 faces south, and each step turns clockwise. Also an
	 * entity's yaw in 22.5° steps (yaw 0 faces south too).
	 */
	private static final String[] COMPASS = {"south", "south-southwest", "southwest", "west-southwest", "west", "west-northwest", "northwest",
			"north-northwest", "north", "north-northeast", "northeast", "east-northeast", "east", "east-southeast", "southeast", "south-southeast"};
	/** Standing signs, ceiling hanging signs and standing banners (all placed with their front towards the player). */
	private static final List<Option> STANDING = List.of(standing(0));
	/** Standing heads. Vanilla places them without a sign's half turn, so rotation 0 faces north. */
	private static final List<Option> STANDING_HEAD = List.of(standing(8));

	/** The option name every armour stand change uses: it only has the one. */
	private static final String ARMOR_STAND_OPTION = "facing";
	/** How far a right click turns an armour stand: vanilla's own placement steps ({@code ArmorStandItem} snaps to 45°). */
	private static final float ARMOR_STAND_STEP = 45.0F;
	/** An item frame's one option: whether the frame itself shows (vanilla's {@code Invisible} flag; the item always shows). */
	private static final String FRAME_OPTION = "frame";

	/** A statue's pose, with the sound and game event of vanilla's own pose change (its {@code updatePose} is protected). */
	private static final List<Option> COPPER_GOLEM_STATUE = List.of(FACING, sounded(cycle("pose", CopperGolemStatueBlock.POSE, BuilderStickItem::word)));

	/**
	 * A dyed block fading through every colour like a sheep named jeb_ ({@link Rainbow}), the last option of every block that has it. A
	 * cushion has it too, as an entity ({@link #onUseEntity}).
	 */
	private static final Option RAINBOW = flag("rainbow", Rainbow.RAINBOW, "on", "off");

	public BuilderStickItem(Item.Properties properties) {
		super(properties);
	}

	/** What the stick can change on {@code state}'s block, in left-click order; empty if it can't be used on it. */
	private static List<Option> optionsFor(BlockState state) {
		List<Option> options = shapeOptionsFor(state);
		return state.hasProperty(Rainbow.RAINBOW) ? Stream.concat(options.stream(), Stream.of(RAINBOW)).toList() : options;
	}

	/** On a mixed slab, a wool half's fade is its only option (flipping one half of a mixed block would make no sense). */
	private static List<Option> mixedHalfOptions(BlockState half) {
		return half.hasProperty(Rainbow.RAINBOW) ? List.of(RAINBOW) : List.of();
	}

	/** {@link #optionsFor} without the rainbow option. */
	private static List<Option> shapeOptionsFor(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof FenceBlock || block instanceof IronBarsBlock) {
			return SIDES;
		} else if (block instanceof WallBlock) {
			return WALL_SIDES;
		} else if (block instanceof FenceGateBlock) {
			return FENCE_GATE;
		} else if (block instanceof DoorBlock) {
			return DOOR;
		} else if (state.is(Blocks.IRON_TRAPDOOR)) {
			return IRON_TRAPDOOR;
		} else if (block instanceof TrapDoorBlock || block instanceof DiodeBlock || block instanceof StonecutterBlock
				|| block instanceof GrindstoneBlock) {
			return TURNS;
		} else if (block instanceof StairBlock) {
			return STAIRS;
		} else if (block instanceof SlabBlock) {
			return state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE ? List.of() : SLAB;
		} else if (block instanceof ChainBlock || isPillar(state)) {
			return AXIS;
		} else if (block instanceof PlacedRodBlock) {
			return state.getValue(PlacedRodBlock.FACING).getAxis() == Direction.Axis.Y ? UPRIGHT_PLACED_ROD : PLACED_ROD;
		} else if (block instanceof RodBlock || block instanceof DispenserBlock || block instanceof ObserverBlock) {
			return ALL_DIRECTIONS;
		} else if (block instanceof PistonBaseBlock) {
			// Turning an extended piston would leave its head behind.
			return state.getValue(PistonBaseBlock.EXTENDED) ? List.of() : ALL_DIRECTIONS;
		} else if (block instanceof HopperBlock) {
			return HOPPER;
		} else if (block instanceof CopperGolemStatueBlock) {
			return COPPER_GOLEM_STATUE;
		} else if (block instanceof RailBlock) {
			return RAIL;
		} else if (block instanceof PoweredRailBlock || block instanceof DetectorRailBlock) {
			return STRAIGHT_RAIL;
		} else if (block instanceof ButtonBlock) {
			return TURNS_SUPPORTED;
		} else if (block instanceof SignBlock || block instanceof AbstractBannerBlock) {
			// Standing signs and banners and ceiling hanging signs have 16 rotations; the wall ones face one of four sides.
			return state.hasProperty(BlockStateProperties.ROTATION_16) ? STANDING : TURNS_SUPPORTED;
		} else if (block instanceof AbstractSkullBlock) {
			return state.hasProperty(BlockStateProperties.ROTATION_16) ? STANDING_HEAD : TURNS_SUPPORTED;
		} else if (block instanceof BellBlock) {
			BellAttachType attachment = state.getValue(BellBlock.ATTACHMENT);
			return attachment == BellAttachType.FLOOR || attachment == BellAttachType.CEILING ? TURNS : WALL_BELL;
		}
		return List.of();
	}

	/**
	 * The pillar blocks the stick turns: every log, wood, stripped log and stripped wood (vanilla's {@code logs} tag, which counts nether
	 * stems and hyphae as logs too), hay bales, quartz and purpur pillars, polished basalt, deepslate, and ancient debris and reinforced
	 * deepslate (given an axis by {@code BlocksMixin}).
	 */
	private static boolean isPillar(BlockState state) {
		return state.is(BlockTags.LOGS) || state.is(Blocks.HAY_BLOCK) || state.is(Blocks.QUARTZ_PILLAR) || state.is(Blocks.PURPUR_PILLAR)
				|| state.is(Blocks.POLISHED_BASALT) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.ANCIENT_DEBRIS)
				|| state.is(Blocks.REINFORCED_DEEPSLATE);
	}

	/**
	 * Whether a change to {@code block} updates its neighbours as placing it would, rather than the debug stick's quiet way. These are the
	 * redstone parts (and blocks next to them), where a quiet change would leave power, outputs and comparators out of date.
	 */
	private static boolean updatesLikePlacing(Block block) {
		return block instanceof PistonBaseBlock || block instanceof DispenserBlock || block instanceof ObserverBlock || block instanceof HopperBlock
				|| block instanceof DiodeBlock || block instanceof BaseRailBlock || block instanceof ButtonBlock || block instanceof SignBlock
				|| block instanceof AbstractBannerBlock || block instanceof AbstractSkullBlock || block instanceof CopperGolemStatueBlock;
	}

	/** An upright placed rod's arm on {@code side}: on or off, whatever is beside it (a block, air or another rod). */
	private static Option rodArm(Direction side) {
		BooleanProperty arm = PlacedRodBlock.ARMS.get(side);
		return new Option(side.getSerializedName(), state -> word(state.getValue(arm) ? "on" : "off"),
				(level, pos, state, player) -> state.cycle(arm));
	}

	/**
	 * All 16 directions (sneak: backwards), shown as {@code COMPASS[rotation + offset]}. A ceiling hanging sign that isn't
	 * {@code ATTACHED} (hung from two chains) turns a quarter at a time, the only way vanilla places those.
	 */
	private static Option standing(int offset) {
		IntegerProperty rotation = BlockStateProperties.ROTATION_16;
		return new Option("facing", state -> Component.literal(COMPASS[(state.getValue(rotation) + offset) % 16]),
				(level, pos, state, player) -> {
					int step = state.hasProperty(CeilingHangingSignBlock.ATTACHED) && !state.getValue(CeilingHangingSignBlock.ATTACHED) ? 4 : 1;
					return state.setValue(rotation, Math.floorMod(state.getValue(rotation) + (player.isSecondaryUseActive() ? -step : step), 16));
				});
	}

	/** {@code option}'s change, repeated until the block can stay there (or, having come full circle, left as it was). */
	private static Option supported(Option option) {
		return new Option(option.name(), option.value(), (level, pos, state, player) -> {
			BlockState changed = state;
			do {
				changed = option.change().apply(level, pos, changed, player);
			} while (changed != state && !stays(level, pos, changed));
			return changed;
		});
	}

	/**
	 * {@code canSurvive}, plus for rails vanilla's other removal rule (a slope needs a block on its rising side), and for wall heads
	 * (which always survive) vanilla's placement rule: something that can't be replaced behind it.
	 */
	private static boolean stays(Level level, BlockPos pos, BlockState state) {
		if (!state.canSurvive(level, pos)) {
			return false;
		} else if (state.getBlock() instanceof WallSkullBlock) {
			return !level.getBlockState(pos.relative(state.getValue(WallSkullBlock.FACING).getOpposite())).canBeReplaced();
		} else if (state.getBlock() instanceof BaseRailBlock rail) {
			Direction rising = switch (state.getValue(rail.getShapeProperty())) {
				case ASCENDING_EAST -> Direction.EAST;
				case ASCENDING_WEST -> Direction.WEST;
				case ASCENDING_NORTH -> Direction.NORTH;
				case ASCENDING_SOUTH -> Direction.SOUTH;
				default -> null;
			};
			return rising == null || Block.canSupportRigidBlock(level, pos.relative(rising));
		}
		return true;
	}

	/** {@code option} with vanilla's copper golem statue pose-change sound and game event. */
	private static Option sounded(Option option) {
		return new Option(option.name(), option.value(), (level, pos, state, player) -> {
			level.playSound(null, pos, SoundEvents.COPPER_GOLEM_BECOME_STATUE, SoundSource.BLOCKS);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			return option.change().apply(level, pos, state, player);
		});
	}

	/** Steps through every value of {@code property} in vanilla's order (sneak: backwards), as the debug stick does. */
	private static <T extends Comparable<T>> Option cycle(String name, Property<T> property, Function<String, Component> shown) {
		return new Option(name, state -> shown.apply(property.getName(state.getValue(property))), (level, pos, state, player) -> {
			T value = state.getValue(property);
			return state.setValue(property, player.isSecondaryUseActive()
					? Util.findPreviousInIterable(property.getPossibleValues(), value)
					: Util.findNextInIterable(property.getPossibleValues(), value));
		});
	}

	/** A true/false property shown as {@code whenTrue} / {@code whenFalse}. */
	private static Option flag(String name, BooleanProperty property, String whenTrue, String whenFalse) {
		return new Option(name, state -> word(state.getValue(property) ? whenTrue : whenFalse),
				(level, pos, state, player) -> state.cycle(property));
	}

	/** A wall side is none/low/tall, so on/off is "not none". */
	private static Option wallSide(Direction side) {
		EnumProperty<WallSide> property = WallBlock.PROPERTY_BY_DIRECTION.get(side);
		return new Option(side.getSerializedName(), state -> word(state.getValue(property) == WallSide.NONE ? "off" : "on"),
				(level, pos, state, player) -> {
					BlockState toggled = state.setValue(property, state.getValue(property) == WallSide.NONE ? WallSide.LOW : WallSide.NONE)
							.setValue(LockedBlocks.LOCKED, false);
					// Vanilla's own update from the block above decides each side's height (low, or tall under a block) and whether the
					// post shows. Unlocked for this, or the lock would keep the old heights; onUseBlock locks it again.
					BlockPos above = pos.above();
					return toggled.updateShape(level, level, pos, Direction.UP, above, level.getBlockState(above), level.getRandom());
				});
	}

	private static Component word(String word) {
		return Component.translatable(KEY + "." + word);
	}

	/** Where the selected option is in {@code options}, or -1 if none is selected or it belongs to another kind of block. */
	private static int selectedIndex(ItemStack stack, List<Option> options) {
		String selected = stack.get(BuilderStick.SELECTED_OPTION);
		for (int i = 0; i < options.size(); i++) {
			if (options.get(i).name().equals(selected)) {
				return i;
			}
		}
		return -1;
	}

	/** Every block "breaks" at once, so a left click acts right away in survival too ({@link #canDestroyBlock} stops the break). */
	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return Float.MAX_VALUE;
	}

	/**
	 * Left click, as the debug stick: select the next option (or say the block isn't supported) instead of breaking the block. With nothing
	 * selected for this kind of block, the first click selects its first option.
	 */
	@Override
	public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
		if (user instanceof ServerPlayer player) {
			long now = level.getGameTime();
			Long last = LAST_LEFT_CLICK.get(player);
			if (last == null || now - last >= REPEAT_TICKS) {
				LAST_LEFT_CLICK.put(player, now);
				// On a stack of heads, the head under the cursor; on a mixed slab, the half under it.
				boolean mixed = MixedSlabs.is(state);
				if (mixed) {
					state = MixedSlabs.targeted(level, pos, player);
				} else if (StackedHeads.aimsAtTop(player, level, pos, state)) {
					state = StackedHeads.top(level, pos, state).state();
				}
				List<Option> options = mixed ? mixedHalfOptions(state) : optionsFor(state);
				if (options.isEmpty()) {
					player.sendOverlayMessage(Component.translatable(KEY + ".not_allowed"));
				} else {
					int index = selectedIndex(stack, options);
					Option option = options.get(index < 0 ? 0 : Math.floorMod(index + (player.isSecondaryUseActive() ? -1 : 1), options.size()));
					stack.set(BuilderStick.SELECTED_OPTION, option.name());
					player.sendOverlayMessage(Component.translatable(KEY + ".select", option.name(), option.value().apply(state)));
				}
			}
		}

		return false;
	}

	/**
	 * Right click ({@code UseBlockCallback}, which runs before the block's own action, so doors, gates and chests don't open): change the
	 * selected option, or the block's first one if none is selected. On an unsupported block the client shows the message and returns FAIL,
	 * which stops vanilla there (no packet, no local door or lever prediction, no off-hand try). On a supported one the client returns
	 * SUCCESS, which Fabric sends on to the server, where the change happens.
	 */
	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		// The callback runs before vanilla's adventure-mode check, so it's repeated here.
		if (!(stack.getItem() instanceof BuilderStickItem) || player.isSpectator() || !player.mayBuild()) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		// On a stack of heads, the top head is changed as a head of its own when it's the one clicked (StackedHeads); on a mixed slab,
		// the half clicked.
		boolean mixed = MixedSlabs.is(state);
		boolean topHead = !mixed && StackedHeads.hitsTop(state, pos, hit.getLocation());
		BlockState target = mixed ? MixedSlabs.targeted(level, pos, hit) : topHead ? StackedHeads.top(level, pos, state).state() : state;
		List<Option> options = mixed ? mixedHalfOptions(target) : optionsFor(target);
		if (options.isEmpty()) {
			if (level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable(KEY + ".not_allowed"));
			}
			return InteractionResult.FAIL;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		Option option = options.get(Math.max(selectedIndex(stack, options), 0));
		BlockState changed = option.change().apply(level, pos, target, player);
		if (topHead) {
			StackedHeads.setTopRotation(level, pos, state, changed.getValue(BlockStateProperties.ROTATION_16));
			player.sendOverlayMessage(Component.translatable(KEY + ".update", option.name(), option.value().apply(changed)));
			return InteractionResult.SUCCESS;
		}
		if (mixed) {
			// The block entity keeps each half's fade, and sends it on to clients, which redraw the block.
			if (level.getBlockEntity(pos) instanceof MixedSlabBlockEntity entity) {
				entity.setHalves(entity.halves().withRainbow(MixedSlabs.isUpperHalf(pos, hit), changed.getValue(Rainbow.RAINBOW)));
			}
			player.sendOverlayMessage(Component.translatable(KEY + ".update", option.name(), option.value().apply(changed)));
			return InteractionResult.SUCCESS;
		}
		Block block = changed.getBlock();
		if (changed != state && LockedBlocks.canLock(block) && option != RAINBOW) {
			// Keep the new shape: neighbours changing (a barrel opening, a rail placed beside it) no longer reshape it. Fading a stained
			// glass pane leaves its shape alone, so it doesn't lock it.
			changed = changed.setValue(LockedBlocks.LOCKED, true);
		}
		// No change (a hopper pointing down, a wall button with only one side to hang on) needs no updates either.
		if (changed != state && updatesLikePlacing(block)) {
			level.setBlock(pos, changed, Block.UPDATE_ALL);
			// Update the blocks around every neighbour too, so whatever the old way round powered (a repeater's or observer's output, a
			// button's wall) is recomputed, then let the block re-check its own inputs (a repeater's power, a piston extending).
			for (Direction side : Direction.values()) {
				level.updateNeighborsAt(pos.relative(side), block);
			}
			level.neighborChanged(pos, block, null);
		} else {
			level.setBlock(pos, changed, FLAGS);
		}

		if (block instanceof DoorBlock) {
			// The other half gets the same change, as vanilla's DoorBlock.updateShape copies one half onto the other.
			DoubleBlockHalf half = changed.getValue(DoorBlock.HALF);
			BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
			BlockState other = level.getBlockState(otherPos);
			if (other.is(block) && other.getValue(DoorBlock.HALF) != half) {
				level.setBlock(otherPos, changed.setValue(DoorBlock.HALF, other.getValue(DoorBlock.HALF)), FLAGS);
			}
		} else if (block instanceof AbstractBedBlock) {
			// A bed's only option is its fade, which the other half shares.
			BlockPos otherPos = pos.relative(AbstractBedBlock.getConnectedDirection(changed));
			BlockState other = level.getBlockState(otherPos);
			if (other.is(block) && other.getValue(AbstractBedBlock.PART) != changed.getValue(AbstractBedBlock.PART)) {
				level.setBlock(otherPos, other.setValue(Rainbow.RAINBOW, changed.getValue(Rainbow.RAINBOW)), FLAGS);
			}
		}

		player.sendOverlayMessage(Component.translatable(KEY + ".update", option.name(), option.value().apply(changed)));
		return InteractionResult.SUCCESS;
	}

	/**
	 * Whether this is the stick, in {@code hand}, on an armour stand, cushion or item frame (glow ones too) the player may change (as with
	 * blocks, not in adventure mode).
	 */
	private static boolean isStickOnEntity(Player player, InteractionHand hand, Entity entity) {
		return player.getItemInHand(hand).getItem() instanceof BuilderStickItem
				&& (entity instanceof ArmorStand stand && !stand.isMarker() || entity instanceof Cushion || entity instanceof ItemFrame)
				&& !player.isSpectator() && player.mayBuild();
	}

	private static Component rainbow(Cushion cushion) {
		return word(RainbowCushions.isRainbow(cushion) ? "on" : "off");
	}

	private static Component frame(ItemFrame frame) {
		return word(frame.isInvisible() ? "hidden" : "shown");
	}

	/** The compass point an armour stand faces, to the nearest 22.5° (yaw 0 faces south, as a sign's rotation 0 does). */
	private static Component facing(ArmorStand stand) {
		return Component.literal(COMPASS[Math.floorMod(Math.round(stand.getYRot() / 22.5F), 16)]);
	}

	/**
	 * Left click on an armour stand, cushion or item frame ({@code AttackEntityCallback}): select its one option (facing, rainbow, frame)
	 * instead of hitting it. The client's SUCCESS sends the attack on to the server, where SUCCESS cancels {@code Player.attack}, so it never
	 * takes a hit (a frame keeps its item and doesn't break).
	 */
	public static InteractionResult onAttackEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit) {
		if (!isStickOnEntity(player, hand, entity)) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide()) {
			if (entity instanceof Cushion cushion) {
				player.getItemInHand(hand).set(BuilderStick.SELECTED_OPTION, RAINBOW.name());
				player.sendOverlayMessage(Component.translatable(KEY + ".select", RAINBOW.name(), rainbow(cushion)));
			} else if (entity instanceof ItemFrame frame) {
				player.getItemInHand(hand).set(BuilderStick.SELECTED_OPTION, FRAME_OPTION);
				player.sendOverlayMessage(Component.translatable(KEY + ".select", FRAME_OPTION, frame(frame)));
			} else {
				player.getItemInHand(hand).set(BuilderStick.SELECTED_OPTION, ARMOR_STAND_OPTION);
				player.sendOverlayMessage(Component.translatable(KEY + ".select", ARMOR_STAND_OPTION, facing((ArmorStand) entity)));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Right click on an armour stand, cushion or item frame ({@code UseEntityCallback}, which runs before the entity's own {@code interact},
	 * so the stick is never put in a stand's hand or a frame, a frame's item never turns, and nobody sits on the cushion). A cushion's fade
	 * turns on or off ({@link RainbowCushions}). A frame is hidden or shown: vanilla's own {@code Invisible} flag, which it saves, sends to
	 * clients and draws (the item alone, flat to the wall). A stand turns {@value #ARMOR_STAND_STEP}° clockwise (sneak: counter-clockwise),
	 * from its facing rounded to the nearest step. As with blocks, the client's SUCCESS sends the click on and the server makes the change.
	 */
	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit) {
		if (!isStickOnEntity(player, hand, entity)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof Cushion cushion) {
			RainbowCushions.setRainbow(cushion, !RainbowCushions.isRainbow(cushion));
			player.sendOverlayMessage(Component.translatable(KEY + ".update", RAINBOW.name(), rainbow(cushion)));
		} else if (entity instanceof ItemFrame frame) {
			frame.setInvisible(!frame.isInvisible());
			player.sendOverlayMessage(Component.translatable(KEY + ".update", FRAME_OPTION, frame(frame)));
		} else {
			ArmorStand stand = (ArmorStand) entity;
			float step = player.isSecondaryUseActive() ? -ARMOR_STAND_STEP : ARMOR_STAND_STEP;
			float yRot = Mth.wrapDegrees(Math.round(stand.getYRot() / ARMOR_STAND_STEP) * ARMOR_STAND_STEP + step);
			// Vanilla /rotate's way of turning an entity; clients are then sent the new rotation.
			stand.forceSetRotation(yRot, false, stand.getXRot(), false);
			player.sendOverlayMessage(Component.translatable(KEY + ".update", ARMOR_STAND_OPTION, facing(stand)));
		}
		return InteractionResult.SUCCESS;
	}
}
