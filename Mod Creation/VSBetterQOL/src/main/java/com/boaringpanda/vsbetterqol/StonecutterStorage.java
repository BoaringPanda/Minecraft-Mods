package com.boaringpanda.vsbetterqol;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

// Stonecutters get 9 storage slots for blocks waiting to be cut (see StonecutterMenuMixin). Stonecutters have no block entity, so the
// items are saved with the chunk, by position, like PlacedLogs. Whoever opens the stonecutter takes them into their menu and they go
// back on close, and only one player can use a stonecutter at a time, so nothing can be duplicated.
public final class StonecutterStorage {
	public static final int SIZE = 9;

	// Position (BlockPos.asLong, as text) → the stored items. Never changed in place, always replaced, so Fabric saves the chunk.
	private static final AttachmentType<Map<String, ItemContainerContents>> STORAGE = AttachmentRegistry.create(
			VSBetterQOL.id("stonecutter_storage"),
			builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, ItemContainerContents.CODEC).xmap(Map::copyOf, map -> map)));

	private StonecutterStorage() {
	}

	// Loads the class, which registers the attachment before any world loads.
	public static void register() {
	}

	// The storage slots need the mod on both sides: a vanilla client's stonecutter menu has no room for them. Server: the player's game
	// has the mod (it accepts our settings packet). Client: the server sent its settings.
	public static boolean enabledFor(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			return ServerPlayNetworking.canSend(serverPlayer, ServerConfig.SyncPayload.TYPE);
		}
		return player.level().isClientSide() && ServerConfig.clientServerHasMod();
	}

	// Removes and returns the items stored at pos (empty if none). Server only.
	public static ItemContainerContents take(Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return ItemContainerContents.EMPTY;
		}
		ChunkAccess chunk = level.getChunk(pos);
		Map<String, ItemContainerContents> stored = chunk.getAttached(STORAGE);
		String key = Long.toString(pos.asLong());
		if (stored == null || !stored.containsKey(key)) {
			return ItemContainerContents.EMPTY;
		}
		Map<String, ItemContainerContents> updated = new HashMap<>(stored);
		ItemContainerContents items = updated.remove(key);
		if (updated.isEmpty()) {
			chunk.removeAttached(STORAGE);
		} else {
			chunk.setAttached(STORAGE, Map.copyOf(updated));
		}
		return items;
	}

	// Saves items at pos. Anything already there (shouldn't happen) is dropped rather than lost. Server only.
	public static void store(Level level, BlockPos pos, ItemContainerContents items) {
		if (level.isClientSide()) {
			return;
		}
		dropAll(level, pos, take(level, pos));
		if (!items.nonEmptyItems().iterator().hasNext()) {
			return;
		}
		ChunkAccess chunk = level.getChunk(pos);
		Map<String, ItemContainerContents> updated = new HashMap<>(chunk.getAttachedOrElse(STORAGE, Map.of()));
		updated.put(Long.toString(pos.asLong()), items);
		chunk.setAttached(STORAGE, Map.copyOf(updated));
	}

	public static void store(Level level, BlockPos pos, Container container) {
		NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		for (int i = 0; i < SIZE; i++) {
			items.set(i, container.removeItemNoUpdate(i));
		}
		store(level, pos, ItemContainerContents.fromItems(items));
	}

	// Fills a menu's storage with the items saved at pos, taking them out of the world.
	public static void takeInto(Level level, BlockPos pos, Container container) {
		NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		take(level, pos).copyInto(items);
		for (int i = 0; i < SIZE; i++) {
			container.setItem(i, items.get(i));
		}
	}

	// The stonecutter at pos is gone (LevelChunkMixin): its stored items drop where it was, like a container's.
	public static void dropRemoved(LevelChunk chunk, BlockPos pos) {
		Level level = chunk.getLevel();
		if (!level.isClientSide() && chunk.hasAttached(STORAGE)) {
			dropAll(level, pos, take(level, pos));
		}
	}

	private static void dropAll(Level level, BlockPos pos, ItemContainerContents items) {
		items.nonEmptyItemCopyStream().forEach(item -> Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item));
	}

	// Another player has this stonecutter open (only one at a time).
	public static boolean inUse(Level level, BlockPos pos, Player except) {
		for (Player other : level.players()) {
			if (other != except && other.containerMenu instanceof StonecutterMenuStorage menu && pos.equals(menu.vsbetterqol$pos())) {
				return true;
			}
		}
		return false;
	}

	public static void sendInUse(Player player) {
		player.sendOverlayMessage(Component.translatableWithFallback("message.vsbetterqol.stonecutter_in_use", "The stonecutter is currently being used"));
	}

	// A storage slot: only things the stonecutter can cut (vanilla's own check, the same one its input slot's shift-click uses).
	public static class StorageSlot extends Slot {
		private final Level level;

		public StorageSlot(Container container, int index, int x, int y, Level level) {
			super(container, index, x, y);
			this.level = level;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return this.level.recipeAccess().stonecutterRecipes().acceptsInput(stack);
		}
	}
}
