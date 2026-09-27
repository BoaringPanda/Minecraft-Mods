package com.boaringpanda.bettervanillafeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;

public final class ToolSpeedRules {
	// Glass and glass panes (via the c: convention tags), so other mods' glass counts too if they tag it.
	private static final TagKey<Block> BREAKS_FASTER_WITH_PICKAXE = TagKey.create(Registries.BLOCK, BetterVanillaFeatures.id("breaks_faster_with_pickaxe"));

	// Bamboo, which vanilla gives to swords instead.
	private static final TagKey<Block> AXE_INSTANTLY_MINES = TagKey.create(Registries.BLOCK, BetterVanillaFeatures.id("axe_instantly_mines"));

	// Mining speed on glass (hand = 1). Wooden is 2x and each tier is 1.5x the one before; gold matches netherite.
	private static final Map<Item, Float> PICKAXE_GLASS_SPEEDS = Map.of(
			Items.WOODEN_PICKAXE, 2.0F,
			Items.STONE_PICKAXE, 3.0F,
			Items.COPPER_PICKAXE, 3.5F,
			Items.IRON_PICKAXE, 4.5F,
			Items.DIAMOND_PICKAXE, 6.75F,
			Items.NETHERITE_PICKAXE, 10.125F,
			Items.GOLDEN_PICKAXE, 10.125F);

	// Float.MAX_VALUE is what vanilla's sword uses for bamboo: always an instant break.
	private static final Map<Item, Float> AXE_BAMBOO_SPEEDS = Map.of(
			Items.WOODEN_AXE, Float.MAX_VALUE,
			Items.STONE_AXE, Float.MAX_VALUE,
			Items.COPPER_AXE, Float.MAX_VALUE,
			Items.IRON_AXE, Float.MAX_VALUE,
			Items.DIAMOND_AXE, Float.MAX_VALUE,
			Items.NETHERITE_AXE, Float.MAX_VALUE,
			Items.GOLDEN_AXE, Float.MAX_VALUE);

	private ToolSpeedRules() {
	}

	private static final List<Item> SWORDS = List.of(
			Items.WOODEN_SWORD, Items.STONE_SWORD, Items.COPPER_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.GOLDEN_SWORD);

	public static void register() {
		addSpeedRule(PICKAXE_GLASS_SPEEDS, BREAKS_FASTER_WITH_PICKAXE);
		addSpeedRule(AXE_BAMBOO_SPEEDS, AXE_INSTANTLY_MINES);

		// Vanilla swords can't break anything in creative (canDestroyBlocksInCreative = false). Turning that on lets them break
		// cobwebs there too; PlayerMixin's blockActionRestricted check still stops them breaking anything else, in every mode.
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(SWORDS, (builder, registries, item) -> {
			Tool tool = builder.get(DataComponents.TOOL);
			builder.set(DataComponents.TOOL, new Tool(tool.rules(), tool.defaultMiningSpeed(), tool.damagePerBlock(), true));
		}));
	}

	// Adds a speed-only rule (like vanilla's sword-on-cobweb one) in front of each item's tool rules, so drops are untouched and
	// Efficiency, Haste, underwater and airborne mining all still apply through vanilla's own mining code. Only vanilla's items,
	// because item tags aren't loaded yet when this event runs.
	private static void addSpeedRule(Map<Item, Float> speeds, TagKey<Block> blocks) {
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(speeds.keySet(), (builder, registries, item) -> {
			Tool tool = builder.get(DataComponents.TOOL);
			List<Tool.Rule> rules = new ArrayList<>();
			rules.add(Tool.Rule.overrideSpeed(registries.lookupOrThrow(Registries.BLOCK).getOrThrow(blocks), speeds.get(item)));
			rules.addAll(tool.rules());
			builder.set(DataComponents.TOOL, new Tool(rules, tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
		}));
	}
}
