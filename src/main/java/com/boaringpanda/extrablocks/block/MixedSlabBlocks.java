package com.boaringpanda.extrablocks.block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.boaringpanda.extrablocks.ExtraBlocks;
import com.boaringpanda.extrablocks.block.custom.MixedSlabBlock;

/**
 * Registers one {@link MixedSlabBlock} for every ordered pair of materials in
 * {@link #MATERIALS} (skipping a material paired with itself - vanilla
 * already merges two identical slabs into its own double slab, no help
 * needed there).
 * <p>
 * To support another slab, add a {@code Blocks.*_SLAB} constant and a short
 * id below and rebuild - every new pairing (with every existing material,
 * both ways round) is generated automatically. The matching
 * {@code assets/extra_blocks/blockstates/mixed_slab_*.json} files and the
 * {@code pickaxe.json} tag need regenerating too - see CLAUDE.md.
 */
public class MixedSlabBlocks {
	private record Material(String id, Block slab) {
	}

	private static final List<Material> MATERIALS = List.of(
			new Material("oak", Blocks.OAK_SLAB),
			new Material("spruce", Blocks.SPRUCE_SLAB),
			new Material("birch", Blocks.BIRCH_SLAB),
			new Material("jungle", Blocks.JUNGLE_SLAB),
			new Material("acacia", Blocks.ACACIA_SLAB),
			new Material("dark_oak", Blocks.DARK_OAK_SLAB),
			new Material("mangrove", Blocks.MANGROVE_SLAB),
			new Material("cherry", Blocks.CHERRY_SLAB),
			new Material("crimson", Blocks.CRIMSON_SLAB),
			new Material("warped", Blocks.WARPED_SLAB),
			new Material("stone", Blocks.STONE_SLAB),
			new Material("cobblestone", Blocks.COBBLESTONE_SLAB),
			new Material("stone_brick", Blocks.STONE_BRICK_SLAB),
			new Material("brick", Blocks.BRICK_SLAB),
			new Material("cobbled_deepslate", Blocks.COBBLED_DEEPSLATE_SLAB),
			new Material("blackstone", Blocks.BLACKSTONE_SLAB)
	);

	/** bottom slab -> top slab -> the combined block for that pairing. */
	private static final Map<Block, Map<Block, MixedSlabBlock>> BY_MATERIALS = new LinkedHashMap<>();

	public static void initialize() {
		for (Material bottom : MATERIALS) {
			for (Material top : MATERIALS) {
				if (bottom == top) {
					continue;
				}

				register(bottom, top);
			}
		}
	}

	private static void register(Material bottom, Material top) {
		String name = "mixed_slab_" + bottom.id() + "_bottom_" + top.id() + "_top";
		Identifier id = ExtraBlocks.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		MixedSlabBlock block = new MixedSlabBlock(
				BlockBehaviour.Properties.of()
						.sound(SoundType.STONE)
						.strength(1.5f, 6.0f)
						.requiresCorrectToolForDrops()
						.setId(key),
				bottom.slab(),
				top.slab()
		);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_MATERIALS.computeIfAbsent(bottom.slab(), b -> new LinkedHashMap<>()).put(top.slab(), block);
	}

	/** The registered combo block for this exact (bottom, top) pairing, or null if that pairing isn't supported. */
	@org.jetbrains.annotations.Nullable
	public static MixedSlabBlock get(Block bottomSlab, Block topSlab) {
		Map<Block, MixedSlabBlock> row = BY_MATERIALS.get(bottomSlab);
		return row == null ? null : row.get(topSlab);
	}
}
