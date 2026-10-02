package com.boaringpanda.lumberjackmod;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;

// The woodcutter villager: his job block is the woodcutter. Trades are data (data/lumberjackmod/trade_set/woodcutter/): level_<n> is
// the random pick vanilla makes for each level, and level_<n>_guaranteed (added by mixin/VillagerMixin) is always given on top.
// Placeholder look: the mason's outfit in blue (textures/entity/villager/profession/woodcutter.png).
public final class WoodcutterVillager {
	private WoodcutterVillager() {
	}

	public static final ResourceKey<PoiType> POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, LumberjackMod.id("woodcutter"));

	public static final ResourceKey<VillagerProfession> PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, LumberjackMod.id("woodcutter"));

	// Loads the class and registers the job site and profession. 1 ticket and range 1, like every vanilla job site.
	public static void register() {
		PoiHelper.register(POI.identifier(), 1, 1, Woodcutter.BLOCK);
		Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, PROFESSION, new VillagerProfession(
				Component.translatable("entity.lumberjackmod.villager.woodcutter"),
				holder -> holder.is(POI),
				holder -> holder.is(POI),
				ImmutableSet.of(),
				ImmutableSet.of(),
				SoundEvents.VILLAGER_WORK_MASON,
				Int2ObjectMap.ofEntries(
						Int2ObjectMap.entry(1, tradeSet(1)),
						Int2ObjectMap.entry(2, tradeSet(2)),
						Int2ObjectMap.entry(3, tradeSet(3)),
						Int2ObjectMap.entry(4, tradeSet(4)),
						Int2ObjectMap.entry(5, tradeSet(5)))));
	}

	public static ResourceKey<TradeSet> guaranteedTradeSet(int level) {
		return ResourceKey.create(Registries.TRADE_SET, LumberjackMod.id("woodcutter/level_" + level + "_guaranteed"));
	}

	private static ResourceKey<TradeSet> tradeSet(int level) {
		return ResourceKey.create(Registries.TRADE_SET, LumberjackMod.id("woodcutter/level_" + level));
	}
}
