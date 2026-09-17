package com.boaringpanda.extrablocks.block;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import com.boaringpanda.extrablocks.ExtraBlocks;

/**
 * Registers this mod's blocks (and their {@link BlockItem}s).
 * <p>
 * Registration itself happens in the static field initializers below, so this
 * class needs to be loaded before anything can rely on the fields being set.
 * {@link #initialize()} exists purely to force that class-loading; call it
 * once from the mod's {@code onInitialize()}.
 */
public class ExtraBlocksBlocks {
	/**
	 * A plain test block with no texture yet - it'll render with the
	 * "missing texture" checkerboard until a real texture is added at
	 * assets/extra_blocks/textures/block/testblock1.png.
	 */
	public static final Block TESTBLOCK1 = register(
			create("testblock1"),
			Block::new,
			BlockBehaviour.Properties.of()
					.sound(SoundType.STONE)
					.strength(1.5f, 6.0f)
	);

	private static BlockItemId create(String name) {
		Identifier id = ExtraBlocks.id(name);
		return BlockItemId.create(id, id);
	}

	private static Block register(BlockItemId id, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
		Block block = blockFactory.apply(properties.setId(id.block()));
		Registry.register(BuiltInRegistries.BLOCK, id.block(), block);

		BlockItem blockItem = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(id.item()));
		Registry.register(BuiltInRegistries.ITEM, id.item(), blockItem);

		return block;
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register((tab) -> {
			tab.accept(TESTBLOCK1.asItem());
		});
	}
}
