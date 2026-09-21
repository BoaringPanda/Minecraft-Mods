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
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.boaringpanda.bpsbettervanillabuilding.BPsBetterVanillaBuilding;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.StackedFlowerBlock;

/**
 * Registers one {@link StackedFlowerBlock} for each small flower that can stack (up to four in one block, like candles and
 * sea pickles): {@code stacked_<flower>}, e.g. {@code stacked_poppy}. Each takes every property of the real flower (sound, no
 * collision, breaks instantly) and is looked up from the real flower by {@link #of}.
 * <p>
 * <b>Except the random position offset.</b> A single flower keeps vanilla's (up to a quarter block off centre either way), but a
 * stack has none: its flowers sit in the four quadrants of the block (see the models), so they stay inside their own block. With
 * the offset as well, a group could sit far enough off centre to spill into the next block's group and clash with it.
 * <p>
 * The list is the small flowers the user asked for: the ones in the creative menu picture plus the golden dandelion, and
 * not the torchflower. To add another, add its id below and rebuild, then add its blockstate, three models, loot table and tag entries
 * (see CLAUDE.md, "Stacked flowers").
 */
public class StackedFlowers {
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bpsbettervanillabuilding/stacked_flowers");

	public static final List<String> FLOWERS = List.of(
			"dandelion",
			"golden_dandelion",
			"poppy",
			"blue_orchid",
			"allium",
			"azure_bluet",
			"red_tulip",
			"orange_tulip",
			"white_tulip",
			"pink_tulip",
			"oxeye_daisy",
			"cornflower",
			"lily_of_the_valley",
			"closed_eyeblossom",
			"open_eyeblossom",
			"wither_rose"
	);

	/** Real flower -> its stack. */
	private static final Map<Block, StackedFlowerBlock> BY_FLOWER = new LinkedHashMap<>();

	public static void initialize() {
		for (String id : FLOWERS) {
			Identifier flowerId = Identifier.withDefaultNamespace(id);
			Block block = BuiltInRegistries.BLOCK.getOptional(flowerId).orElse(null);
			if (!(block instanceof FlowerBlock flower)) {
				// Shouldn't happen for this list, but a renamed flower shouldn't crash the whole mod on startup.
				LOGGER.warn("No such flower block: {} - it can't be stacked", flowerId);
				continue;
			}

			Identifier stackedId = BPsBetterVanillaBuilding.id("stacked_" + id);
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, stackedId);
			StackedFlowerBlock stacked = new StackedFlowerBlock(
					BlockBehaviour.Properties.ofFullCopy(flower).offsetType(BlockBehaviour.OffsetType.NONE).setId(key), flower);
			Registry.register(BuiltInRegistries.BLOCK, stackedId, stacked);
			BY_FLOWER.put(flower, stacked);
		}
	}

	/** The stack for a real flower block, or null if that flower doesn't stack. */
	@Nullable
	public static StackedFlowerBlock of(Block flower) {
		return BY_FLOWER.get(flower);
	}
}
