package com.boaringpanda.vsbetterqol;

import java.util.Map;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;

// A carried brewing stand (Carrying) keeps brewing: fuel gets used, potions finish (with vanilla's "done" sound where the player is). It
// can't be opened until it's set down (Dylan's pick). Runs vanilla's own brewing tick on a brewing stand that isn't in the world, made
// from the carried item, and writes it back into the item every second and before it's placed, dropped on death or the player leaves.
public final class CarriedBrewing {
	private static final int SAVE_EVERY_TICKS = 20;

	// One per player carrying a brewing stand. Weak, so a player who's gone (or respawned as a new entity) doesn't stay in here.
	private static final Map<ServerPlayer, BrewingStandBlockEntity> STANDS = new WeakHashMap<>();

	private CarriedBrewing() {
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			for (ServerPlayer player : level.players()) {
				tick(level, player);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> flush(handler.player));
	}

	private static void tick(ServerLevel level, ServerPlayer player) {
		ItemStack carried = Carrying.carried(player);
		if (!carried.is(Items.BREWING_STAND)) {
			STANDS.remove(player);
			return;
		}
		BrewingStandBlockEntity stand = STANDS.computeIfAbsent(player, key -> load(level, carried));
		// The stand isn't in the world. Passing air as its block stops vanilla setting a block (the bottles it shows) or updating
		// comparators at the player's feet; the "done" sound and any leftover items (like a water bucket's bucket) still happen there.
		BrewingStandBlockEntity.serverTick(level, player.blockPosition(), Blocks.AIR.defaultBlockState(), stand);
		if (level.getGameTime() % SAVE_EVERY_TICKS == 0) {
			save(player, stand);
		}
	}

	// Writes the latest brewing state into the carried item. Called before anything reads the item (placing, dying, leaving).
	public static void flush(ServerPlayer player) {
		BrewingStandBlockEntity stand = STANDS.get(player);
		if (stand != null) {
			save(player, stand);
		}
	}

	// A new carry starts from its own item.
	public static void forget(ServerPlayer player) {
		STANDS.remove(player);
	}

	// Like placing the item: its saved block data first, then its components (contents, name), the same order BlockItem uses.
	private static BrewingStandBlockEntity load(ServerLevel level, ItemStack stack) {
		BrewingStandBlockEntity stand = new BrewingStandBlockEntity(BlockPos.ZERO, Blocks.BREWING_STAND.defaultBlockState());
		var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (data != null) {
			data.loadInto(stand, level.registryAccess());
		}
		stand.applyComponentsFromItemStack(stack);
		return stand;
	}

	// The same copy vanilla's Ctrl+pick-block makes. Only sent on when something changed, since every change is synced to all players.
	private static void save(ServerPlayer player, BrewingStandBlockEntity stand) {
		ItemStack stack = new ItemStack(Items.BREWING_STAND);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
		stand.saveCustomOnly(output);
		stand.removeComponentsFromTag(output);
		BlockItem.setBlockEntityData(stack, stand.getType(), output);
		stack.applyComponents(stand.collectComponents());
		if (!ItemStack.matches(stack, Carrying.carried(player))) {
			Carrying.replace(player, stack);
		}
	}
}
