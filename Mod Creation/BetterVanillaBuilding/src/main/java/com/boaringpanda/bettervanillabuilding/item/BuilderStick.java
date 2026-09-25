package com.boaringpanda.bettervanillabuilding.item;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import com.boaringpanda.bettervanillabuilding.BetterVanillaBuilding;

/**
 * The Builder Stick: a survival debug stick for a few blocks (and armour stands) only, each with a few options ({@link BuilderStickItem}
 * lists them). Crafted from an amethyst shard above a stick.
 */
public class BuilderStick {
	/** The name of the option a Builder Stick has selected (left click picks the next one), shared by every block. */
	public static final DataComponentType<String> SELECTED_OPTION = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			BetterVanillaBuilding.id("builder_stick_option"),
			DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

	public static final Item BUILDER_STICK = register();

	private static Item register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BetterVanillaBuilding.id("builder_stick"));
		return Registry.register(BuiltInRegistries.ITEM, key, new BuilderStickItem(new Item.Properties().setId(key).stacksTo(1)));
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.insertAfter(Items.BRUSH, BUILDER_STICK));
		UseBlockCallback.EVENT.register(BuilderStickItem::onUseBlock);
		AttackEntityCallback.EVENT.register(BuilderStickItem::onAttackEntity);
		UseEntityCallback.EVENT.register(BuilderStickItem::onUseEntity);
	}
}
