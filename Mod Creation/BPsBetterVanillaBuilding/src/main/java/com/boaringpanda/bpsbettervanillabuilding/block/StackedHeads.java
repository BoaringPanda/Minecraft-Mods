package com.boaringpanda.bpsbettervanillabuilding.block;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.boaringpanda.bpsbettervanillabuilding.BPsBetterVanillaBuilding;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedHeadsBlockEntity;

/**
 * Registers the block that holds two heads in one block space, and its block entity type. There is no
 * {@code BlockItem}: it is never held, only made by putting a head on top of another head
 * ({@link StackedHeadsInteraction}).
 */
public class StackedHeads {
	public static final StackedHeadsBlock BLOCK = registerBlock();
	public static final BlockEntityType<StackedHeadsBlockEntity> BLOCK_ENTITY = registerBlockEntity();

	private static StackedHeadsBlock registerBlock() {
		Identifier id = BPsBetterVanillaBuilding.id("stacked_heads");
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
		// Everything a real head has: hardness, sound, and being destroyed by pistons. Any head would do as the source,
		// they share these.
		StackedHeadsBlock block = new StackedHeadsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SKELETON_SKULL).setId(key));
		return Registry.register(BuiltInRegistries.BLOCK, id, block);
	}

	private static BlockEntityType<StackedHeadsBlockEntity> registerBlockEntity() {
		BlockEntityType<StackedHeadsBlockEntity> type = new BlockEntityType<>(StackedHeadsBlockEntity::new, Set.of(BLOCK));
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BPsBetterVanillaBuilding.id("stacked_heads"), type);
	}

	/** Loading the class is what registers everything; this just makes the call site say so. */
	public static void initialize() {
	}
}
