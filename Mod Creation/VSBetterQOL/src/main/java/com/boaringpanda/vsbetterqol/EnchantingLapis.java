package com.boaringpanda.vsbetterqol;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;

// Lapis left in an enchanting table stays there (see EnchantmentMenuMixin), saved on the table's block entity. Whoever opens the table
// takes it into their menu and it goes back on close, so two players can never both have it.
public final class EnchantingLapis {
	private EnchantingLapis() {
	}

	private static final AttachmentType<ItemStack> LAPIS = AttachmentRegistry.create(
			VSBetterQOL.id("lapis"),
			builder -> builder.persistent(ItemStack.CODEC));

	// Loads the class, which registers the attachment before any world loads.
	public static void register() {
	}

	// Removes the table's lapis and returns it (empty if none).
	public static ItemStack take(EnchantingTableBlockEntity table) {
		ItemStack lapis = table.removeAttached(LAPIS);
		if (lapis == null) {
			return ItemStack.EMPTY;
		}
		table.setChanged();
		return lapis;
	}

	// The table's lapis without removing it (empty if none). Don't change the returned stack.
	public static ItemStack stored(EnchantingTableBlockEntity table) {
		return table.getAttachedOrElse(LAPIS, ItemStack.EMPTY);
	}

	// Puts lapis back into the table, up to a full stack. Whatever doesn't fit stays in the given stack.
	public static void store(EnchantingTableBlockEntity table, ItemStack lapis) {
		if (lapis.isEmpty()) {
			return;
		}

		ItemStack stored = table.getAttachedOrElse(LAPIS, ItemStack.EMPTY);
		int moved = Math.min(lapis.getCount(), lapis.getMaxStackSize() - stored.getCount());
		if (moved <= 0) {
			return;
		}
		table.setAttached(LAPIS, lapis.copyWithCount(stored.getCount() + moved));
		lapis.shrink(moved);
		table.setChanged();
	}
}
