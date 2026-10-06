package com.boaringpanda.vsbetterbuilding.block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import com.boaringpanda.vsbetterbuilding.VSBetterBuilding;

/**
 * Stairs and slabs for blocks vanilla has none for: smooth stone (stairs only, vanilla has the slab), deepslate, moss, pale moss, snow,
 * packed ice, blue ice, calcite, obsidian, amethyst, and terracotta in every colour; and walls for all of those too. They're plain vanilla {@link StairBlock}s, {@link SlabBlock}s and {@link WallBlock}s
 * made the way vanilla makes its own ({@code Blocks.registerStair} / {@code registerSlab} / {@code registerWall}), copying the base
 * block's properties, so the Builder Stick, locked blocks and mixed slabs pick them up by class. Models, loot, recipes and tags are data.
 */
public class ExtraStairsAndSlabs {
	/** A copy of vanilla's private {@code Blocks.NEAR_PLANE_INTERSECTS_OUTLINE}, which every vanilla stair and slab uses. */
	private static final BlockBehaviour.StateArgumentPredicate<AABB> NEAR_PLANE_INTERSECTS_OUTLINE = (state, level, pos, nearPlaneBox) -> {
		for (AABB outlineBox : state.getOcclusionShape().toAabbs()) {
			if (outlineBox.move(pos).intersects(nearPlaneBox)) {
				return true;
			}
		}
		return false;
	};

	public static final Block SMOOTH_STONE_STAIRS = stairs("smooth_stone", Blocks.SMOOTH_STONE);
	public static final Block DEEPSLATE_STAIRS = stairs("deepslate", Blocks.DEEPSLATE);
	public static final Block DEEPSLATE_SLAB = slab("deepslate", Blocks.DEEPSLATE);
	public static final Block MOSS_STAIRS = stairs("moss", Blocks.MOSS_BLOCK);
	public static final Block MOSS_SLAB = slab("moss", Blocks.MOSS_BLOCK);
	public static final Block PALE_MOSS_STAIRS = stairs("pale_moss", Blocks.PALE_MOSS_BLOCK);
	public static final Block PALE_MOSS_SLAB = slab("pale_moss", Blocks.PALE_MOSS_BLOCK);
	public static final Block SNOW_STAIRS = stairs("snow", Blocks.SNOW_BLOCK);
	public static final Block SNOW_SLAB = slab("snow", Blocks.SNOW_BLOCK);
	public static final Block PACKED_ICE_STAIRS = stairs("packed_ice", Blocks.PACKED_ICE);
	public static final Block PACKED_ICE_SLAB = slab("packed_ice", Blocks.PACKED_ICE);
	public static final Block BLUE_ICE_STAIRS = stairs("blue_ice", Blocks.BLUE_ICE);
	public static final Block BLUE_ICE_SLAB = slab("blue_ice", Blocks.BLUE_ICE);
	public static final Block CALCITE_STAIRS = stairs("calcite", Blocks.CALCITE);
	public static final Block CALCITE_SLAB = slab("calcite", Blocks.CALCITE);
	public static final Block OBSIDIAN_STAIRS = stairs("obsidian", Blocks.OBSIDIAN);
	public static final Block OBSIDIAN_SLAB = slab("obsidian", Blocks.OBSIDIAN);
	public static final Block AMETHYST_STAIRS = stairs("amethyst", Blocks.AMETHYST_BLOCK);
	public static final Block AMETHYST_SLAB = slab("amethyst", Blocks.AMETHYST_BLOCK);
	public static final Block TERRACOTTA_STAIRS = stairs("terracotta", Blocks.TERRACOTTA);
	public static final Block TERRACOTTA_SLAB = slab("terracotta", Blocks.TERRACOTTA);
	public static final Map<DyeColor, Block> DYED_TERRACOTTA_STAIRS = byColor(color -> stairs(color.getName() + "_terracotta",
			Blocks.DYED_TERRACOTTA.pick(color)));
	public static final Map<DyeColor, Block> DYED_TERRACOTTA_SLABS = byColor(color -> slab(color.getName() + "_terracotta",
			Blocks.DYED_TERRACOTTA.pick(color)));

	public static final Block SMOOTH_STONE_WALL = wall("smooth_stone", Blocks.SMOOTH_STONE);
	public static final Block DEEPSLATE_WALL = wall("deepslate", Blocks.DEEPSLATE);
	public static final Block PACKED_ICE_WALL = wall("packed_ice", Blocks.PACKED_ICE);
	public static final Block BLUE_ICE_WALL = wall("blue_ice", Blocks.BLUE_ICE);
	public static final Block CALCITE_WALL = wall("calcite", Blocks.CALCITE);
	public static final Block OBSIDIAN_WALL = wall("obsidian", Blocks.OBSIDIAN);
	public static final Block MOSS_WALL = wall("moss", Blocks.MOSS_BLOCK);
	public static final Block PALE_MOSS_WALL = wall("pale_moss", Blocks.PALE_MOSS_BLOCK);
	public static final Block SNOW_WALL = wall("snow", Blocks.SNOW_BLOCK);
	public static final Block AMETHYST_WALL = wall("amethyst", Blocks.AMETHYST_BLOCK);
	public static final Block TERRACOTTA_WALL = wall("terracotta", Blocks.TERRACOTTA);
	public static final Map<DyeColor, Block> DYED_TERRACOTTA_WALLS = byColor(color -> wall(color.getName() + "_terracotta",
			Blocks.DYED_TERRACOTTA.pick(color)));

	/** Vanilla's colour order in the Colored Blocks tab ({@code CreativeModeTabs}' {@code gameplayColorOrder}, a local there). */
	private static final List<DyeColor> TAB_COLOR_ORDER = List.of(DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK,
			DyeColor.BROWN, DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE,
			DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK);

	private static Block stairs(String material, Block base) {
		return register(material + "_stairs", properties -> new StairBlock(base.defaultBlockState(), properties),
				BlockBehaviour.Properties.ofFullCopy(base).isViewBlocking(NEAR_PLANE_INTERSECTS_OUTLINE));
	}

	@SuppressWarnings("deprecation")
	private static Block slab(String material, Block base) {
		return register(material + "_slab", SlabBlock::new,
				BlockBehaviour.Properties.ofLegacyCopy(base).isViewBlocking(NEAR_PLANE_INTERSECTS_OUTLINE));
	}

	/** As vanilla's {@code Blocks.registerWall}. */
	@SuppressWarnings("deprecation")
	private static Block wall(String material, Block base) {
		return register(material + "_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(base).forceSolidOn());
	}

	private static Map<DyeColor, Block> byColor(Function<DyeColor, Block> factory) {
		Map<DyeColor, Block> blocks = new EnumMap<>(DyeColor.class);
		for (DyeColor color : DyeColor.values()) {
			blocks.put(color, factory.apply(color));
		}
		return blocks;
	}

	/** Registers the block and its block item, as vanilla's {@code Items.registerBlock} does. */
	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, VSBetterBuilding.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, VSBetterBuilding.id(name));
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		item.registerBlocks(Item.BY_BLOCK, item);
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return block;
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			output.insertAfter(Items.SMOOTH_STONE, SMOOTH_STONE_STAIRS);
			output.insertAfter(Items.SMOOTH_STONE_SLAB, SMOOTH_STONE_WALL);
			output.insertAfter(Items.DEEPSLATE, DEEPSLATE_STAIRS, DEEPSLATE_SLAB, DEEPSLATE_WALL);
			output.insertAfter(Items.AMETHYST_BLOCK, AMETHYST_STAIRS, AMETHYST_SLAB, AMETHYST_WALL);
			for (Block block : List.of(CALCITE_STAIRS, CALCITE_SLAB, CALCITE_WALL, OBSIDIAN_STAIRS, OBSIDIAN_SLAB, OBSIDIAN_WALL,
					PACKED_ICE_STAIRS, PACKED_ICE_SLAB, PACKED_ICE_WALL, BLUE_ICE_STAIRS, BLUE_ICE_SLAB, BLUE_ICE_WALL, SNOW_STAIRS, SNOW_SLAB,
					SNOW_WALL, MOSS_STAIRS, MOSS_SLAB, MOSS_WALL, PALE_MOSS_STAIRS, PALE_MOSS_SLAB, PALE_MOSS_WALL)) {
				output.accept(block);
			}
		});
		// Like vanilla's concrete stairs and slabs: every stair, then every slab, then every wall, after the last terracotta.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COLORED_BLOCKS).register(output -> {
			List<Block> terracotta = new ArrayList<>();
			terracotta.add(TERRACOTTA_STAIRS);
			TAB_COLOR_ORDER.forEach(color -> terracotta.add(DYED_TERRACOTTA_STAIRS.get(color)));
			terracotta.add(TERRACOTTA_SLAB);
			TAB_COLOR_ORDER.forEach(color -> terracotta.add(DYED_TERRACOTTA_SLABS.get(color)));
			terracotta.add(TERRACOTTA_WALL);
			TAB_COLOR_ORDER.forEach(color -> terracotta.add(DYED_TERRACOTTA_WALLS.get(color)));
			output.insertAfter(Items.DYED_TERRACOTTA.pick(TAB_COLOR_ORDER.getLast()), terracotta.toArray(Block[]::new));
		});
	}
}
