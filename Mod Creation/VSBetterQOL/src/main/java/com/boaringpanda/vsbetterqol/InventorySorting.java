package com.boaringpanda.vsbetterqol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Sort buttons (client/SortButtons) sort A-Z by name: the player's 27 main slots, or a chest/barrel/ender chest/shulker box.
// Same stacks merge first, so partial stacks join up and empty slots end up at the bottom. Hotbar, armour and offhand never move.
public final class InventorySorting {
	// Name A-Z (what the player sees, so renamed items go by their new name), then item id, then bigger stacks first.
	private static final Comparator<ItemStack> ORDER = Comparator
			.comparing((ItemStack stack) -> stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
			.thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
			.thenComparing(ItemStack::getCount, Comparator.reverseOrder());

	private InventorySorting() {
	}

	// Sent by a sort button. playerInventory: the player's main slots, otherwise the open container's slots.
	public record SortPayload(int containerId, boolean playerInventory) implements CustomPacketPayload {
		public static final Type<SortPayload> TYPE = new Type<>(VSBetterQOL.id("sort_inventory"));
		public static final StreamCodec<ByteBuf, SortPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, SortPayload::containerId,
				ByteBufCodecs.BOOL, SortPayload::playerInventory,
				SortPayload::new);

		@Override
		public Type<SortPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(SortPayload.TYPE, SortPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SortPayload.TYPE, (payload, context) -> sort(context.player(), payload));
	}

	// The open menu's slots a button sorts, in slot order (rows top to bottom). Empty if that part can't be sorted. The client uses
	// this too, to put each button above the top-right slot.
	public static List<Slot> sortableSlots(AbstractContainerMenu menu, Inventory inventory, boolean playerInventory) {
		if (playerInventory) {
			return menu.slots.stream()
					.filter(slot -> slot.container == inventory && slot.getContainerSlot() >= Inventory.SELECTION_SIZE
							&& slot.getContainerSlot() < Inventory.INVENTORY_SIZE)
					.toList();
		}
		if (!(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu)) {
			return List.of();
		}
		return menu.slots.stream().filter(slot -> slot.container != inventory).toList();
	}

	private static void sort(ServerPlayer player, SortPayload payload) {
		AbstractContainerMenu menu = player.containerMenu;
		if (menu.containerId != payload.containerId() || player.isSpectator()) {
			return;
		}
		List<Slot> slots = sortableSlots(menu, player.getInventory(), payload.playerInventory());

		List<ItemStack> stacks = new ArrayList<>();
		for (Slot slot : slots) {
			ItemStack rest = slot.getItem().copy();
			for (ItemStack merged : stacks) {
				if (rest.isEmpty()) {
					break;
				}
				if (ItemStack.isSameItemSameComponents(merged, rest)) {
					int moved = Math.min(rest.getCount(), merged.getMaxStackSize() - merged.getCount());
					merged.grow(moved);
					rest.shrink(moved);
				}
			}
			if (!rest.isEmpty()) {
				stacks.add(rest);
			}
		}
		stacks.sort(ORDER);

		for (int i = 0; i < slots.size(); i++) {
			slots.get(i).set(i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
		}
		menu.broadcastChanges();
	}
}
