package com.boaringpanda.bpsbettervanillabuilding.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import com.boaringpanda.bpsbettervanillabuilding.BPsBetterVanillaBuilding;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.TerracottaStairBlock;

/**
 * Stairs and slabs for plain terracotta and the 16 dyed terracottas (vanilla has none), as ordinary placeable
 * blocks with their own items: {@code <material>_stairs} and {@code <material>_slab}.
 * <p>
 * Each block copies every property (hardness, sound, map colour, pickaxe requirement) from the vanilla block it
 * is made of, looked up by registry ID because the dyed ones aren't individual {@code Blocks} fields. The slabs
 * are also materials in {@link MixedSlabBlocks#MATERIALS}, so they combine with every other slab. All the assets
 * reuse vanilla's terracotta textures; see CLAUDE.md, "Terracotta stairs and slabs", for the per-block files.
 */
public class TerracottaBlocks {
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bpsbettervanillabuilding/terracotta");

	/**
	 * Each is also the vanilla block's own ID (in {@code minecraft:}) that the stairs and slab copy from. The order
	 * is the Colored Blocks tab's: plain terracotta, then the dyed ones in vanilla's colour order (white, light
	 * gray, gray, black, brown, red, orange, yellow, lime, green, cyan, light blue, blue, purple, magenta, pink),
	 * so the creative tab can list the stairs and the slabs in this order.
	 */
	public static final List<String> MATERIALS = List.of(
			"terracotta",
			"white_terracotta",
			"light_gray_terracotta",
			"gray_terracotta",
			"black_terracotta",
			"brown_terracotta",
			"red_terracotta",
			"orange_terracotta",
			"yellow_terracotta",
			"lime_terracotta",
			"green_terracotta",
			"cyan_terracotta",
			"light_blue_terracotta",
			"blue_terracotta",
			"purple_terracotta",
			"magenta_terracotta",
			"pink_terracotta"
	);

	public static void initialize() {
		List<Item> stairsItems = new ArrayList<>();
		List<Item> slabItems = new ArrayList<>();

		for (String material : MATERIALS) {
			Identifier baseId = Identifier.fromNamespaceAndPath("minecraft", material);
			Block base = BuiltInRegistries.BLOCK.getOptional(baseId).orElse(null);
			if (base == null) {
				LOGGER.warn("No such block: {} - skipping its stairs and slab", baseId);
				continue;
			}

			stairsItems.add(register(material + "_stairs", key -> new TerracottaStairBlock(
					base.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(base).setId(key))));
			slabItems.add(register(material + "_slab", key -> new SlabBlock(
					BlockBehaviour.Properties.ofFullCopy(base).setId(key))));
		}

		// Vanilla's own constant for the tab's key is private. Its layout lists each block type for every colour in
		// one run (wool, wool stairs, wool slabs, carpet, terracotta, dyed terracotta, concrete, ...), so the
		// terracotta stairs run and then the slabs run go straight after the last dyed terracotta (pink).
		ResourceKey<CreativeModeTab> coloredBlocks = ResourceKey.create(
				Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("colored_blocks"));
		Item lastDyedTerracotta = BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace("pink_terracotta")).orElse(null);
		ItemLike[] tabOrder = Stream.concat(stairsItems.stream(), slabItems.stream()).toArray(ItemLike[]::new);
		CreativeModeTabEvents.modifyOutputEvent(coloredBlocks).register(output -> {
			if (lastDyedTerracotta == null) {
				// Shouldn't happen, but better to still be in the tab (at the end) than to crash on opening it.
				for (ItemLike item : tabOrder) {
					output.accept(item);
				}
				return;
			}
			output.insertAfter(lastDyedTerracotta, tabOrder);
		});
	}

	/** Registers the block and its {@link BlockItem} under the same ID, and returns the item. */
	private static Item register(String name, Function<ResourceKey<Block>, Block> factory) {
		Identifier id = BPsBetterVanillaBuilding.id(name);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(blockKey));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id,
				new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
	}
}
