package com.boaringpanda.vsbetterqol;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// When a player's item drops to 7 durability or less, a red "Your axe is about to break" shows above the hotbar with a bass note.
// Once per drop: it only warns when crossing from above 7, so again only after Mending (or an anvil) takes it back above.
public final class DurabilityWarning {
	private static final int WARN_AT = 7;

	// First match wins. Tags first, so other mods' tools and armour count too. The English is the fallback for players without the mod.
	private static final List<Type> TYPES = List.of(
			Type.tag(ItemTags.AXES, "axe", "Your axe is about to break"),
			Type.tag(ItemTags.PICKAXES, "pickaxe", "Your pickaxe is about to break"),
			Type.tag(ItemTags.SHOVELS, "shovel", "Your shovel is about to break"),
			Type.tag(ItemTags.HOES, "hoe", "Your hoe is about to break"),
			Type.tag(ItemTags.SWORDS, "sword", "Your sword is about to break"),
			Type.tag(ItemTags.SPEARS, "spear", "Your spear is about to break"),
			Type.tag(ItemTags.HEAD_ARMOR, "helmet", "Your helmet is about to break"),
			Type.tag(ItemTags.CHEST_ARMOR, "chestplate", "Your chestplate is about to break"),
			Type.tag(ItemTags.LEG_ARMOR, "leggings", "Your leggings are about to break"),
			Type.tag(ItemTags.FOOT_ARMOR, "boots", "Your boots are about to break"),
			Type.item(Items.BOW, "bow", "Your bow is about to break"),
			Type.item(Items.CROSSBOW, "crossbow", "Your crossbow is about to break"),
			Type.item(Items.TRIDENT, "trident", "Your trident is about to break"),
			Type.item(Items.MACE, "mace", "Your mace is about to break"),
			Type.item(Items.SHIELD, "shield", "Your shield is about to break"),
			Type.item(Items.ELYTRA, "elytra", "Your elytra is about to break"),
			Type.item(Items.FISHING_ROD, "fishing_rod", "Your fishing rod is about to break"),
			Type.item(Items.SHEARS, "shears", "Your shears are about to break"),
			Type.item(Items.FLINT_AND_STEEL, "flint_and_steel", "Your flint and steel is about to break"),
			Type.item(Items.BRUSH, "brush", "Your brush is about to break"),
			Type.item(Items.CARROT_ON_A_STICK, "carrot_on_a_stick", "Your carrot on a stick is about to break"),
			Type.item(Items.WARPED_FUNGUS_ON_A_STICK, "warped_fungus_on_a_stick", "Your warped fungus on a stick is about to break"));

	private DurabilityWarning() {
	}

	// Called with the damage the stack is about to be set to (after Unbreaking).
	public static void onDamage(ItemStack stack, ServerPlayer player, int newDamage) {
		int max = stack.getMaxDamage();
		int leftBefore = max - stack.getDamageValue();
		int leftAfter = max - newDamage;
		if (leftBefore <= WARN_AT || leftAfter > WARN_AT || leftAfter <= 0) {
			return;
		}
		player.sendOverlayMessage(message(stack).withStyle(ChatFormatting.RED));
		// Player.playSound skips the player it's for, so it goes straight to them.
		player.connection.send(new ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_BASS, SoundSource.PLAYERS, player.getX(), player.getY(),
				player.getZ(), 0.6F, 0.5F, player.getRandom().nextLong()));
	}

	private static MutableComponent message(ItemStack stack) {
		for (Type type : TYPES) {
			if (type.matches().test(stack)) {
				return Component.translatableWithFallback("message.vsbetterqol.about_to_break." + type.key(), type.english());
			}
		}
		return Component.translatableWithFallback("message.vsbetterqol.about_to_break", "Your %s is about to break", stack.getItemName());
	}

	private record Type(Predicate<ItemStack> matches, String key, String english) {
		static Type tag(TagKey<Item> tag, String key, String english) {
			return new Type(stack -> stack.is(tag), key, english);
		}

		static Type item(Item item, String key, String english) {
			return new Type(stack -> stack.is(item), key, english);
		}
	}
}
