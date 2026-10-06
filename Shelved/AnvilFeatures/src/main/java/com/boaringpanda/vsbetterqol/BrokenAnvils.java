package com.boaringpanda.vsbetterqol;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

// The broken anvil block and its item. Same properties as a damaged anvil (strength, sound, needs a pickaxe).
public final class BrokenAnvils {
	private BrokenAnvils() {
	}

	public static final Block BROKEN_ANVIL = registerBlock();

	// Registers the block and its block item, as vanilla's Items.registerBlock does.
	private static Block registerBlock() {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, VSBetterQOL.id("broken_anvil"));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				new BrokenAnvilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DAMAGED_ANVIL).setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, VSBetterQOL.id("broken_anvil"));
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		item.registerBlocks(Item.BY_BLOCK, item);
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return block;
	}

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(output -> output.insertAfter(Items.DAMAGED_ANVIL, BROKEN_ANVIL));
	}
}
