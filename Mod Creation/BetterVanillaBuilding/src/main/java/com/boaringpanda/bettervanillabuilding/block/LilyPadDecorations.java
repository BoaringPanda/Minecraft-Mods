package com.boaringpanda.bettervanillabuilding.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.RodBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decorations standing on a lily pad, in the pad's own block space. A decoration on a pad is still the real vanilla block (a real
 * torch, sign, candle...) with {@link #LILY_PAD} set to true, so everything the block does on its own keeps working. The pad is drawn
 * under it ({@code LilyPadDecorationModel}), supports it, adds to its shape, and drops when it goes. {@code BlockMixin} adds the property
 * to every block {@link #canSitOnPad} accepts.
 */
public final class LilyPadDecorations {
	public static final BooleanProperty LILY_PAD = BooleanProperty.create("lily_pad");

	/** What you stand on: vanilla's lily pad shape. */
	private static final VoxelShape PAD_COLLISION = Block.column(14.0, 0.0, 1.5);
	/** What you aim at: the whole drawn pad (it's a full 16 wide), so a pad under a wide pot still has an edge to aim at. */
	private static final VoxelShape PAD_OUTLINE = Block.column(16.0, 0.0, 1.5);
	/** The decorations' shapes are shared constants, so the combined shape is worked out once per shape. */
	private static final Map<VoxelShape, VoxelShape> WITH_PAD_COLLISION = new ConcurrentHashMap<>();
	private static final Map<VoxelShape, VoxelShape> WITH_PAD_OUTLINE = new ConcurrentHashMap<>();

	private LilyPadDecorations() {
	}

	/**
	 * Whether {@code block} can stand on a lily pad. Classes, so every variant counts (stained panes, candle colours, wood signs, heads,
	 * copper stages). Wall torches, wall signs, wall banners and wall heads are their own classes and never stand on anything.
	 */
	public static boolean canSitOnPad(Block block) {
		return block instanceof TorchBlock && !(block instanceof WallTorchBlock)
				|| block instanceof RedstoneTorchBlock && !(block instanceof RedstoneWallTorchBlock)
				|| block instanceof IronBarsBlock // iron and copper bars, glass and stained glass panes
				|| block instanceof ChainBlock
				|| block instanceof LanternBlock
				|| block instanceof CandleBlock
				|| block instanceof BannerBlock
				|| block instanceof AmethystClusterBlock // the cluster and all three buds
				|| block instanceof SeaPickleBlock
				|| block instanceof ButtonBlock
				|| block instanceof StandingSignBlock
				|| block instanceof RodBlock // end rod, lightning rods, placed stick / blaze rod / breeze rod
				|| block instanceof SkullBlock
				|| block instanceof DecoratedPotBlock
				|| block instanceof FlowerPotBlock // the empty pot and every potted plant
				|| block instanceof TurtleEggBlock
				|| block instanceof CopperGolemStatueBlock; // every copper stage, waxed or not
	}

	/** {@code to} standing on a pad if {@code from} was: for vanilla swaps that build the new block from its default state. */
	public static BlockState keepPad(BlockState from, BlockState to) {
		return onPad(from) && to.hasProperty(LILY_PAD) ? to.setValue(LILY_PAD, true) : to;
	}

	/** Whether {@code state} is a decoration standing on a lily pad. */
	public static boolean onPad(BlockState state) {
		return state.hasProperty(LILY_PAD) && state.getValue(LILY_PAD);
	}

	/** The block {@code stack} places, or null: a block item's block, or the mod's placed rods. */
	public static @Nullable Block blockFor(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : PlacedRods.blockFor(stack.getItem());
	}

	/** Whether {@code stack} places something that can stand on a lily pad. */
	public static boolean goesOnPad(ItemStack stack) {
		Block block = blockFor(stack);
		return block != null && block.defaultBlockState().hasProperty(LILY_PAD);
	}

	/** Whether a lily pad could stay at {@code pos}: vanilla's own lily pad rule (water or ice below, nothing wet at {@code pos}). */
	public static boolean padSurvives(LevelReader level, BlockPos pos) {
		return Blocks.LILY_PAD.defaultBlockState().canSurvive(level, pos);
	}

	/**
	 * The context to place with when {@code context} would replace a lily pad, or null if it wouldn't. The new block goes in the pad's
	 * space and stands on it: {@link LilyPadPlaceContext} makes every block choose its standing or floor form.
	 */
	public static @Nullable BlockPlaceContext padContext(BlockPlaceContext context) {
		return context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.LILY_PAD) ? new LilyPadPlaceContext(context) : null;
	}

	/** The state to place for {@code context}: {@code state} on the pad for a {@link LilyPadPlaceContext}, otherwise unchanged. */
	public static @Nullable BlockState placedOnPad(BlockPlaceContext context, @Nullable BlockState state) {
		if (!(context instanceof LilyPadPlaceContext) || state == null) {
			return state;
		}
		return state.hasProperty(LILY_PAD) ? state.setValue(LILY_PAD, true) : null;
	}

	/**
	 * Whether an item frame at {@code pos} facing {@code direction} lies on a bare lily pad in that space. Only a loaded chunk is looked
	 * at, because a frame works out its box while it's being loaded or generated too.
	 */
	public static boolean frameOnPad(Level level, BlockPos pos, Direction direction) {
		return direction == Direction.UP && level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.LILY_PAD);
	}

	/** A floor item frame's box ({@code frame}) raised onto a lily pad's top, where vanilla stands an armour stand. */
	public static AABB onPadTop(AABB frame) {
		return frame.move(0.0, PAD_COLLISION.max(Direction.Axis.Y), 0.0);
	}

	/** {@code shape} with the lily pad added: {@code collision} for what you bump into, otherwise for what you aim at. */
	public static VoxelShape withPad(VoxelShape shape, boolean collision) {
		return collision
				? WITH_PAD_COLLISION.computeIfAbsent(shape, s -> Shapes.or(s, PAD_COLLISION))
				: WITH_PAD_OUTLINE.computeIfAbsent(shape, s -> Shapes.or(s, PAD_OUTLINE));
	}

	/**
	 * Whether {@code player} is aiming at the bare lily pad of the decoration at {@code pos}, rather than at the decoration. A raycast,
	 * so the client and server agree (as {@code MixedSlabs.targeted}). Anything not on a pad is never "the pad".
	 */
	public static boolean aimsAtPad(Player player, BlockGetter level, BlockPos pos, BlockState state) {
		if (!onPad(state)) {
			return false;
		}
		HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK || !blockHit.getBlockPos().equals(pos)) {
			return false;
		}
		Vec3 point = blockHit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		VoxelShape decoration = partShape(state, level, pos, CollisionContext.of(player), false);
		return decoration.toAabbs().stream().noneMatch(box -> box.inflate(1.0E-4).contains(point));
	}

	/** One part of a decoration on a pad: the bare pad ({@code pad}) or the decoration alone. Used to aim and to draw the outline. */
	public static VoxelShape partShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, boolean pad) {
		return pad ? PAD_OUTLINE : state.setValue(LILY_PAD, false).getShape(level, pos, context);
	}
}
