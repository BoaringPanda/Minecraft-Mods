package com.boaringpanda.bettervanillabuilding.block;

import java.util.Comparator;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import com.boaringpanda.bettervanillabuilding.BetterVanillaBuilding;

/**
 * Any two different slabs stack into one block, the way two of the same slab make a vanilla double slab. There is one block for every
 * pair ({@link MixedSlabBlock}); its block entity ({@link MixedSlabBlockEntity}) remembers which slab is the bottom and which the top,
 * and the client model draws the two vanilla slab models. Every {@link SlabBlock} works, including other mods' slabs.
 * <ul>
 *   <li>Walking, landing and sprinting use the top slab (the one you're standing on).</li>
 *   <li>Mining hits and the break sound use the half under the cursor ({@link #targeted}).</li>
 *   <li>Mining speed and the right tool come from the tougher slab ({@link #toughest}). Each slab drops by its own rule.</li>
 * </ul>
 */
public class MixedSlabs {
	public static final Block MIXED_SLAB = registerBlock();
	public static final BlockEntityType<MixedSlabBlockEntity> MIXED_SLAB_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			BetterVanillaBuilding.id("mixed_slab"), new BlockEntityType<>(MixedSlabBlockEntity::new, Set.of(MIXED_SLAB)));

	/**
	 * The two slabs of a mixed block. {@link #FALLBACK} (two smooth stone slabs) stands in when a block has no data, e.g. one made
	 * with {@code /setblock}, so it's never invisible.
	 */
	public record Halves(Block bottom, Block top) {
		public static final Halves FALLBACK = new Halves(Blocks.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE_SLAB);

		/** A slab block by id. A slab from a mod that has since been removed fails to load, and the block falls back to {@link #FALLBACK}. */
		private static final Codec<Block> SLAB = BuiltInRegistries.BLOCK.byNameCodec().validate(
				block -> block instanceof SlabBlock ? DataResult.success(block) : DataResult.error(() -> "Not a slab: " + block));
		public static final Codec<Halves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				SLAB.fieldOf("bottom").forGetter(Halves::bottom),
				SLAB.fieldOf("top").forGetter(Halves::top)).apply(instance, Halves::new));

		public BlockState bottomState() {
			return bottom.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
		}

		public BlockState topState() {
			return top.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
		}

		/** The bottom slab if {@code upper} is false, else the top one (as a half slab). */
		public BlockState half(boolean upper) {
			return upper ? topState() : bottomState();
		}

		/**
		 * The slab that decides how the block mines: one that needs a tool beats one that doesn't, then the harder, then the more blast
		 * resistant. A full tie goes to the bottom slab.
		 */
		public BlockState toughest() {
			Comparator<BlockState> toughness = Comparator.<BlockState, Boolean>comparing(BlockState::requiresCorrectToolForDrops)
					.thenComparingDouble(state -> state.getBlock().defaultDestroyTime())
					.thenComparingDouble(state -> state.getBlock().getExplosionResistance());
			BlockState bottomState = bottomState();
			BlockState topState = topState();
			return toughness.compare(topState, bottomState) > 0 ? topState : bottomState;
		}
	}

	private static Block registerBlock() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, BetterVanillaBuilding.id("mixed_slab"));
		// The real sound, hardness and map colour come from the two slabs (see the class comment); these are only defaults.
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 6.0F)
				.sound(SoundType.STONE).setId(key);
		return Registry.register(BuiltInRegistries.BLOCK, key, new MixedSlabBlock(properties));
	}

	public static void initialize() {
		// Loads the class, which registers the block and block entity.
	}

	/** Whether {@code state} is a mixed slab block. */
	public static boolean is(BlockState state) {
		return state.is(MIXED_SLAB);
	}

	/** Whether {@code stack} places a slab, so it can stack on a different slab. */
	public static boolean isSlabItem(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SlabBlock;
	}

	/** The two slabs of the mixed block at {@code pos}. */
	public static Halves halves(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof MixedSlabBlockEntity entity ? entity.halves() : Halves.FALLBACK;
	}

	/** The slab an entity standing on {@code pos} stands on: the top slab of a mixed block, or {@code state} for anything else. */
	public static BlockState surface(BlockGetter level, BlockPos pos, BlockState state) {
		return is(state) ? halves(level, pos).topState() : state;
	}

	/** The half slab of the mixed block at {@code pos} that {@code hit} points at, or the top one if the hit isn't on this block. */
	public static BlockState targeted(BlockGetter level, BlockPos pos, @Nullable HitResult hit) {
		return halves(level, pos).half(isUpperHalf(pos, hit));
	}

	/** The half slab of the mixed block at {@code pos} that {@code player} is looking at (a raycast, so it works on either side). */
	public static BlockState targeted(BlockGetter level, BlockPos pos, Player player) {
		return targeted(level, pos, player.pick(player.blockInteractionRange(), 1.0F, false));
	}

	private static boolean isUpperHalf(BlockPos pos, @Nullable HitResult hit) {
		if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(pos)) {
			return blockHit.getLocation().y - pos.getY() >= 0.5;
		}
		return true;
	}
}
