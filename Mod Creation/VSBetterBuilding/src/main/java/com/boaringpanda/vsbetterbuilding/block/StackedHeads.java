package com.boaringpanda.vsbetterbuilding.block;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Two standing heads in one block, one on top of the other. The bottom head is the real vanilla head (its skin, rotation, note block
 * sound, powered animation, lily pad and Builder Stick all work as usual). The top head is extra data on the bottom head's own
 * {@link SkullBlockEntity} ({@link TopHeadHolder}, saved by {@code SkullBlockEntityMixin}), and {@link #TOP} on the bottom head's state
 * says a top head is there and what shape it has.
 *
 * <p>Breaking either head takes only that head ({@link #breakHead}). Without its bottom head, the top head stays where it was, half a
 * block up: it becomes a real vanilla head of its own with {@link #RAISED} set, and a head can be put back under it.
 */
public final class StackedHeads {
	/** Where the top head is saved in the bottom head's block entity data. */
	public static final String TAG = "vsbetterbuilding:top_head";

	/**
	 * The top head's shape, if there is one: piglin heads are wider and the dragon head's outline is taller than every other head. This
	 * is in the state (not only the block entity) so shapes can be worked out from the state, as vanilla caches them.
	 */
	public enum Top implements StringRepresentable {
		// NONE first: blocks take their default state from the first value of each property.
		NONE("none", Shapes.empty(), Shapes.empty()),
		HEAD("head", Block.column(8.0, 8.0, 16.0), Block.column(8.0, 8.0, 16.0)),
		PIGLIN("piglin", Block.column(10.0, 8.0, 16.0), Block.column(10.0, 8.0, 16.0)),
		DRAGON("dragon", Block.column(8.0, 8.0, 16.5), Block.column(8.0, 8.0, 16.0));

		private final String name;
		/** Vanilla's {@code SkullBlock} shapes, 8 px up. */
		private final VoxelShape outline;
		private final VoxelShape collision;

		Top(String name, VoxelShape outline, VoxelShape collision) {
			this.name = name;
			this.outline = outline;
			this.collision = collision;
		}

		/** The value for a top head of {@code block} (a {@link SkullBlock}). */
		public static Top of(Block block) {
			SkullBlock.Type type = ((SkullBlock) block).getType();
			return type == SkullBlock.Types.PIGLIN ? PIGLIN : type == SkullBlock.Types.DRAGON ? DRAGON : HEAD;
		}

		private VoxelShape shape(boolean outlineShape) {
			return outlineShape ? outline : collision;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Top> TOP = EnumProperty.create("top_head", Top.class);
	/**
	 * A single head half a block up, left there when the head under it broke. Never set together with {@link #TOP}. Off by default
	 * ({@code BlockMixin}: a boolean property's first value is true).
	 */
	public static final BooleanProperty RAISED = BooleanProperty.create("raised");

	private record ShapeKey(VoxelShape bottom, Top top, boolean outline) {
	}

	/** The bottom heads' shapes are shared constants, so each combined shape is worked out once. */
	private static final Map<ShapeKey, VoxelShape> STACKED_SHAPES = new ConcurrentHashMap<>();

	/**
	 * One head of a stack (the stored top head, or a broken one on its way to being dropped): which head, which way it faces, and the
	 * three things a {@link SkullBlockEntity} keeps from the item (skin, note block sound, name), so it drops as the item it was placed from.
	 */
	public record Head(Block block, int rotation, Optional<ResolvableProfile> profile, Optional<Identifier> noteBlockSound,
			Optional<Component> customName) {
		/** A head block by id. A head from a mod that has since been removed fails to load, and the stack uses {@link #fallback}. */
		private static final Codec<Block> HEAD = BuiltInRegistries.BLOCK.byNameCodec().validate(
				block -> block instanceof SkullBlock ? DataResult.success(block) : DataResult.error(() -> "Not a standing head: " + block));
		public static final Codec<Head> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				HEAD.fieldOf("block").forGetter(Head::block),
				Codec.intRange(0, SkullBlock.MAX).fieldOf("rotation").forGetter(Head::rotation),
				ResolvableProfile.CODEC.optionalFieldOf("profile").forGetter(Head::profile),
				Identifier.CODEC.optionalFieldOf("note_block_sound").forGetter(Head::noteBlockSound),
				ComponentSerialization.CODEC.optionalFieldOf("custom_name").forGetter(Head::customName)).apply(instance, Head::new));

		/** The standing head {@code state} with the skin, sound and name in {@code components} (a placed item's or a block entity's). */
		static Head of(BlockState state, DataComponentGetter components) {
			return new Head(state.getBlock(), state.getValue(SkullBlock.ROTATION), Optional.ofNullable(components.get(DataComponents.PROFILE)),
					Optional.ofNullable(components.get(DataComponents.NOTE_BLOCK_SOUND)),
					Optional.ofNullable(components.get(DataComponents.CUSTOM_NAME)));
		}

		/** The head {@code state} at {@code pos}, with its block entity's skin, sound and name. */
		static Head at(BlockGetter level, BlockPos pos, BlockState state) {
			BlockEntity entity = level.getBlockEntity(pos);
			return of(state, entity != null ? entity.collectComponents() : DataComponentMap.EMPTY);
		}

		/** Stands in for a stack with no data (made with {@code /setblock}) so the top head is never invisible. */
		static Head fallback(BlockState bottom) {
			Block block = switch (bottom.getValue(TOP)) {
				case PIGLIN -> Blocks.PIGLIN_HEAD;
				case DRAGON -> Blocks.DRAGON_HEAD;
				default -> Blocks.SKELETON_SKULL;
			};
			return new Head(block, bottom.getValue(SkullBlock.ROTATION), Optional.empty(), Optional.empty(), Optional.empty());
		}

		/** The head as a standing head of its own. */
		public BlockState state() {
			return block.defaultBlockState().setValue(SkullBlock.ROTATION, rotation);
		}

		Head withRotation(int newRotation) {
			return new Head(block, newRotation, profile, noteBlockSound, customName);
		}

		/** The head's item, with its skin, sound and name if {@code includeData} (as a picked or dropped head would have). */
		public ItemStack toItem(boolean includeData) {
			ItemStack stack = new ItemStack(block);
			if (includeData) {
				profile.ifPresent(value -> stack.set(DataComponents.PROFILE, value));
				noteBlockSound.ifPresent(value -> stack.set(DataComponents.NOTE_BLOCK_SOUND, value));
				customName.ifPresent(value -> stack.set(DataComponents.CUSTOM_NAME, value));
			}
			return stack;
		}
	}

	private StackedHeads() {
	}

	/** Whether {@code state} is a standing head with another head on top. */
	public static boolean isStacked(BlockState state) {
		return state.hasProperty(TOP) && state.getValue(TOP) != Top.NONE;
	}

	/** Whether {@code state} is a single head half a block up, with nothing under it. */
	public static boolean isRaised(BlockState state) {
		return state.hasProperty(RAISED) && state.getValue(RAISED);
	}

	/** {@code state} with its top head gone. */
	public static BlockState withoutTop(BlockState state) {
		return state.setValue(TOP, Top.NONE);
	}

	/** The top head of the stack {@code state}, whose block entity is {@code entity}. */
	public static Head top(@Nullable BlockEntity entity, BlockState state) {
		Head top = entity instanceof TopHeadHolder holder ? holder.vsbetterbuilding$getTopHead() : null;
		return top != null ? top : Head.fallback(state);
	}

	public static Head top(BlockGetter level, BlockPos pos, BlockState state) {
		return top(level.getBlockEntity(pos), state);
	}

	/** Whether {@code stack} places a standing head (the item of every head places the standing one and picks the wall one itself). */
	private static boolean isHeadItem(ItemStack stack) {
		return LilyPadDecorations.blockFor(stack) instanceof SkullBlock;
	}

	/**
	 * Whether the standing head {@code state} makes room for the head in hand: a single head takes it on top, and a raised head takes it
	 * underneath. Vanilla's slab rule: a click on the free half's face (the head's top face, not the lily pad's, or a raised head's
	 * underside), or into the head's space from a block next to it (e.g. the floor under a raised head). Not while sneaking, as with
	 * candles and sea pickles, so a sneak click still puts a head in the block above.
	 */
	public static boolean takesHead(BlockState state, BlockPlaceContext context) {
		if (!state.hasProperty(TOP) || isStacked(state) || context.isSecondaryUseActive() || !isHeadItem(context.getItemInHand())) {
			return false;
		} else if (!context.replacingClickedOnBlock()) {
			return true;
		}
		double y = context.getClickLocation().y - context.getClickedPos().getY();
		return isRaised(state)
				? context.getClickedFace() == Direction.DOWN && y <= 0.5 + 1.0E-4
				: context.getClickedFace() == Direction.UP && y >= 0.5 - 1.0E-4;
	}

	/** Whether {@code context} places a head into the free half of the head at its position ({@link #takesHead} said yes). */
	public static boolean fillsStack(BlockPlaceContext context) {
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		return state.hasProperty(TOP) && !isStacked(state) && isHeadItem(context.getItemInHand());
	}

	/**
	 * The head {@code block} would be for {@code context}: its own standing placement (facing the player), never the wall form. Null when
	 * something is in the way of the finished stack, e.g. a player standing on the bottom head.
	 */
	public static @Nullable BlockState placementState(BlockPlaceContext context, Block block) {
		BlockState placed = block.getStateForPlacement(context);
		if (placed == null) {
			return null;
		}
		BlockState there = context.getLevel().getBlockState(context.getClickedPos());
		BlockState stack = isRaised(there) ? placed.setValue(TOP, Top.of(there.getBlock())) : there.setValue(TOP, Top.of(block));
		return context.getLevel().isUnobstructed(stack, context.getClickedPos(), CollisionContext.empty()) ? placed : null;
	}

	/**
	 * Puts the head {@code placed}, from {@code stack}, into the free half at {@code pos}. On top of a single head, it becomes the stack's
	 * top head, and vanilla must not then copy the item's skin and name onto the bottom head's block entity. Under a raised head, it
	 * becomes the real bottom head and the raised one becomes the top head, and vanilla gives the new bottom head the item's skin and name
	 * as for any placed head.
	 */
	public static boolean placeHead(Level level, BlockPos pos, BlockState placed, ItemStack stack, @Nullable Player player) {
		BlockState there = level.getBlockState(pos);
		if (isRaised(there)) {
			Head top = Head.at(level, pos, there.setValue(RAISED, false));
			BlockState stacked = placed.setValue(TOP, Top.of(top.block()))
					.setValue(LilyPadDecorations.LILY_PAD, there.getValue(LilyPadDecorations.LILY_PAD));
			if (!level.setBlock(pos, stacked, Block.UPDATE_ALL_IMMEDIATE)) {
				return false;
			}
			setTop(level, pos, top);
			return true;
		}

		if (!level.setBlock(pos, there.setValue(TOP, Top.of(placed.getBlock())), Block.UPDATE_ALL_IMMEDIATE)) {
			return false;
		}
		// Same block, so the bottom head's block entity (and its skin) stays.
		setTop(level, pos, Head.of(placed, stack));
		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
		}
		return true;
	}

	/**
	 * Breaks one head of the stack {@code state} at {@code pos} (the top one if {@code top}) and returns it for dropping, or null if the
	 * block didn't change. The other head stays where it is: the bottom head alone, or the top head alone, raised.
	 */
	public static @Nullable Head breakHead(Level level, BlockPos pos, BlockState state, boolean top, int flags) {
		Head topHead = top(level, pos, state);
		if (top) {
			// Cleared first, so the block entity update the change sends has no top head.
			setTop(level, pos, null);
			return level.setBlock(pos, withoutTop(state), flags) ? topHead : null;
		}

		Head bottom = Head.at(level, pos, withoutTop(state));
		BlockState raised = topHead.state().setValue(RAISED, true)
				.setValue(AbstractSkullBlock.POWERED, state.getValue(AbstractSkullBlock.POWERED))
				.setValue(LilyPadDecorations.LILY_PAD, state.getValue(LilyPadDecorations.LILY_PAD));
		if (!level.setBlock(pos, raised, flags)) {
			return null;
		}
		// The block entity (kept if it's the same kind of head, else a new one) is now the raised head's own.
		if (level.getBlockEntity(pos) instanceof SkullBlockEntity entity) {
			entity.applyComponentsFromItemStack(topHead.toItem(true));
			((TopHeadHolder) entity).vsbetterbuilding$setTopHead(null);
			entity.setChanged();
		}
		return bottom;
	}

	/** Turns the top head of the stack at {@code pos} (the Builder Stick), and tells clients. */
	public static void setTopRotation(Level level, BlockPos pos, BlockState state, int rotation) {
		setTop(level, pos, top(level, pos, state).withRotation(rotation));
		level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
	}

	private static void setTop(Level level, BlockPos pos, @Nullable Head top) {
		if (level.getBlockEntity(pos) instanceof TopHeadHolder holder) {
			holder.vsbetterbuilding$setTopHead(top);
			((BlockEntity) holder).setChanged();
		}
	}

	/** A block entity for one head alone, so vanilla's loot tables drop it with its skin and name. */
	public static SkullBlockEntity tempEntity(BlockPos pos, Head head) {
		SkullBlockEntity entity = new SkullBlockEntity(pos, head.state());
		entity.applyComponentsFromItemStack(head.toItem(true));
		return entity;
	}

	/** What the top head of the stack {@code state} drops when the whole stack breaks: its own loot, as if broken alone. */
	public static List<ItemStack> topDrops(BlockState state, LootParams.Builder params) {
		BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		Head top = top(entity, state);
		params.withParameter(LootContextParams.BLOCK_ENTITY, tempEntity(BlockPos.containing(params.getParameter(LootContextParams.ORIGIN)), top));
		List<ItemStack> drops = top.state().getDrops(params);
		params.withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity);
		return drops;
	}

	/**
	 * {@code shape} (vanilla's, for the head {@code state}) for what you aim at ({@code outline}) or bump into: a raised head's own shape
	 * half a block up, or a stack's two heads.
	 */
	public static VoxelShape shape(VoxelShape shape, BlockState state, boolean outline) {
		if (isRaised(state)) {
			return Top.of(state.getBlock()).shape(outline);
		} else if (!isStacked(state)) {
			return shape;
		}
		return STACKED_SHAPES.computeIfAbsent(new ShapeKey(shape, state.getValue(TOP), outline),
				key -> Shapes.or(key.bottom(), key.top().shape(key.outline())));
	}

	/** Whether {@code location} (a point on the block at {@code pos}) is on the top head of the stack {@code state}. */
	public static boolean hitsTop(BlockState state, BlockPos pos, Vec3 location) {
		if (!isStacked(state)) {
			return false;
		}
		Vec3 point = location.subtract(pos.getX(), pos.getY(), pos.getZ());
		return state.getValue(TOP).outline.toAabbs().stream().anyMatch(box -> box.inflate(1.0E-4).contains(point));
	}

	/**
	 * Whether {@code player} is aiming at the top head of the stack at {@code pos}, rather than the bottom head (or its lily pad). A
	 * raycast, so the client and server agree (as {@code LilyPadDecorations.aimsAtPad}).
	 */
	public static boolean aimsAtTop(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state) {
		if (player == null || !isStacked(state)) {
			return false;
		}
		HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
		return hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(pos)
				&& hitsTop(state, pos, blockHit.getLocation());
	}

	/** One head of a stack, the top one ({@code top}) or the bottom one, for the outline. */
	public static VoxelShape partShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, boolean top) {
		return top ? state.getValue(TOP).outline
				: withoutTop(state).setValue(LilyPadDecorations.LILY_PAD, false).getShape(level, pos, context);
	}
}
