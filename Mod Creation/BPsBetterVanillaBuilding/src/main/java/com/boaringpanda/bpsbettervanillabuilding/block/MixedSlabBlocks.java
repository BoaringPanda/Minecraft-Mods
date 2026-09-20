package com.boaringpanda.bpsbettervanillabuilding.block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.boaringpanda.bpsbettervanillabuilding.BPsBetterVanillaBuilding;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.MixedSlabBlock;

/**
 * Registers one {@link MixedSlabBlock} for every ordered pair of materials in
 * {@link #MATERIALS} (skipping a material paired with itself - vanilla
 * already merges two identical slabs into its own double slab, no help
 * needed there).
 * <p>
 * {@link #MATERIALS} is every vanilla slab's own block ID with the trailing
 * {@code _slab} stripped (i.e. every {@code assets/minecraft/blockstates/*_slab.json}
 * in the game). Each material is resolved to its actual {@link Block} via a
 * registry lookup on {@code minecraft:<material>_slab} rather than a
 * {@code Blocks.*} constant - wool, concrete and copper slabs aren't
 * individual {@code Blocks} fields (they're {@code ColorCollection}/
 * {@code WeatheringCopperCollection} entries), so a registry lookup by ID is
 * what handles every material uniformly without needing to know which.
 * <p>
 * To support another slab (a future vanilla one, or a modded one), add its
 * material id below and rebuild - every new pairing (with every existing
 * material, both ways round) is generated automatically. The matching
 * {@code assets/bpsbettervanillabuilding/blockstates/mixed_slab_*.json} files and the
 * {@code pickaxe.json} tag need regenerating too - see CLAUDE.md, especially
 * the warning about not assuming a slab's model is named
 * {@code <material>_slab}/{@code <material>_slab_top} - some aren't (e.g.
 * waxed copper slabs reuse their unwaxed model).
 */
public class MixedSlabBlocks {
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bpsbettervanillabuilding/mixed_slabs");

	private static final List<String> MATERIALS = List.of(
			"acacia",
			"andesite",
			"bamboo",
			"bamboo_mosaic",
			"birch",
			"black_concrete",
			"black_wool",
			"blackstone",
			"blue_concrete",
			"blue_wool",
			"brick",
			"brown_concrete",
			"brown_wool",
			"cherry",
			"cinnabar",
			"cinnabar_brick",
			"cobbled_deepslate",
			"cobblestone",
			"crimson",
			"cut_copper",
			"cut_red_sandstone",
			"cut_sandstone",
			"cyan_concrete",
			"cyan_wool",
			"dark_oak",
			"dark_prismarine",
			"deepslate_brick",
			"deepslate_tile",
			"diorite",
			"end_stone_brick",
			"exposed_cut_copper",
			"granite",
			"gray_concrete",
			"gray_wool",
			"green_concrete",
			"green_wool",
			"jungle",
			"light_blue_concrete",
			"light_blue_wool",
			"light_gray_concrete",
			"light_gray_wool",
			"lime_concrete",
			"lime_wool",
			"magenta_concrete",
			"magenta_wool",
			"mangrove",
			"mossy_cobblestone",
			"mossy_stone_brick",
			"mud_brick",
			"nether_brick",
			"oak",
			"orange_concrete",
			"orange_wool",
			"oxidized_cut_copper",
			"pale_oak",
			"petrified_oak",
			"pink_concrete",
			"pink_wool",
			"polished_andesite",
			"polished_blackstone",
			"polished_blackstone_brick",
			"polished_cinnabar",
			"polished_deepslate",
			"polished_diorite",
			"polished_granite",
			"polished_sulfur",
			"polished_tuff",
			"poplar",
			"prismarine",
			"prismarine_brick",
			"purple_concrete",
			"purple_wool",
			"purpur",
			"quartz",
			"red_concrete",
			"red_nether_brick",
			"red_sandstone",
			"red_wool",
			"resin_brick",
			"sandstone",
			"smooth_quartz",
			"smooth_red_sandstone",
			"smooth_sandstone",
			"smooth_stone",
			"spruce",
			"stone",
			"stone_brick",
			"sulfur",
			"sulfur_brick",
			"tuff",
			"tuff_brick",
			"warped",
			"waxed_cut_copper",
			"waxed_exposed_cut_copper",
			"waxed_oxidized_cut_copper",
			"waxed_weathered_cut_copper",
			"weathered_cut_copper",
			"white_concrete",
			"white_wool",
			"yellow_concrete",
			"yellow_wool"
	);

	/** bottom slab -> top slab -> the combined block for that pairing. */
	private static final Map<Block, Map<Block, MixedSlabBlock>> BY_MATERIALS = new LinkedHashMap<>();

	public static void initialize() {
		Map<String, Block> resolved = new LinkedHashMap<>();
		for (String material : MATERIALS) {
			Identifier slabId = Identifier.fromNamespaceAndPath("minecraft", material + "_slab");
			Block slab = BuiltInRegistries.BLOCK.getOptional(slabId).orElse(null);
			if (slab == null) {
				// Shouldn't happen for the vanilla list above, but a future
				// material added here with a typo'd or renamed id should be
				// skipped rather than crash the whole mod on startup.
				LOGGER.warn("No such slab block: {} - skipping it as a mixed-slab material", slabId);
				continue;
			}
			resolved.put(material, slab);
		}

		for (Map.Entry<String, Block> bottom : resolved.entrySet()) {
			for (Map.Entry<String, Block> top : resolved.entrySet()) {
				if (bottom.getKey().equals(top.getKey())) {
					continue;
				}

				register(bottom.getKey(), bottom.getValue(), top.getKey(), top.getValue());
			}
		}
	}

	private static void register(String bottomId, Block bottomSlab, String topId, Block topSlab) {
		String name = "mixed_slab_" + bottomId + "_bottom_" + topId + "_top";
		Identifier id = BPsBetterVanillaBuilding.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		MixedSlabBlock block = new MixedSlabBlock(
				BlockBehaviour.Properties.of()
						.sound(SoundType.STONE)
						.strength(1.5f, 6.0f)
						.requiresCorrectToolForDrops()
						.setId(key),
				bottomSlab,
				topSlab
		);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_MATERIALS.computeIfAbsent(bottomSlab, b -> new LinkedHashMap<>()).put(topSlab, block);
	}

	/** The registered combo block for this exact (bottom, top) pairing, or null if that pairing isn't supported. */
	@Nullable
	public static MixedSlabBlock get(Block bottomSlab, Block topSlab) {
		Map<Block, MixedSlabBlock> row = BY_MATERIALS.get(bottomSlab);
		return row == null ? null : row.get(topSlab);
	}
}
