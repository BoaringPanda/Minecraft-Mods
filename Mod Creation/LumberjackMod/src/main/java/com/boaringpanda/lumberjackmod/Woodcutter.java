package com.boaringpanda.lumberjackmod;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

// The woodcutter: a stonecutter for wood (placeholder look: the stonecutter model with vanilla wood textures). It reuses vanilla's
// stonecutter menu, screen and recipes: its recipes are plain minecraft:stonecutting JSONs (data/lumberjackmod/recipe/woodcutting/),
// and mixin/StonecutterMenuMixin keeps the two apart by input item (#lumberjackmod:woodcutter_inputs only go in the woodcutter).
public final class Woodcutter {
	private Woodcutter() {
	}

	public static final TagKey<Item> INPUTS = TagKey.create(Registries.ITEM, LumberjackMod.id("woodcutter_inputs"));

	public static final Block BLOCK = registerBlock();

	public static final MenuType<WoodcutterMenu> MENU = Registry.register(BuiltInRegistries.MENU, LumberjackMod.id("woodcutter"),
			new MenuType<>(WoodcutterMenu::new, FeatureFlags.VANILLA_SET));

	// Loads the class, which registers everything above.
	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.insertAfter(Items.STONECUTTER, BLOCK));
	}

	// Like the crafting table: wooden, burns, no tool needed for drops (an axe is fastest, see mineable/axe).
	private static Block registerBlock() {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, LumberjackMod.id("woodcutter"));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, new WoodcutterBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(3.5F).sound(SoundType.WOOD).ignitedByLava()
				.setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, LumberjackMod.id("woodcutter"));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}
}
