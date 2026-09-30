package com.boaringpanda.bettervanillaqol;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

// Remembers which logs players placed, so they don't hold up natural leaves (see LeavesBlockMixin). Saved with each chunk as a list
// of positions. "Log" is vanilla's #minecraft:prevents_nearby_leaf_decay, the same blocks leaves count.
public final class PlacedLogs {
	private PlacedLogs() {
	}

	// The set is never changed in place, always replaced, so Fabric notices and saves the chunk.
	private static final AttachmentType<Set<Long>> PLACED_LOGS = AttachmentRegistry.create(
			BetterVanillaQOL.id("placed_logs"),
			builder -> builder.persistent(Codec.LONG.listOf().xmap(Set::copyOf, List::copyOf)));

	// Loads the class, which registers the attachment before any world loads.
	public static void register() {
	}

	// Called when a player places a log (BlockItemMixin).
	public static void remember(Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return;
		}

		ChunkAccess chunk = level.getChunk(pos);
		Set<Long> placed = chunk.getAttachedOrElse(PLACED_LOGS, Set.of());
		if (!placed.contains(pos.asLong())) {
			Set<Long> updated = new HashSet<>(placed);
			updated.add(pos.asLong());
			chunk.setAttached(PLACED_LOGS, Set.copyOf(updated));
		}
	}

	// Called when the log at pos is gone for any reason (LevelChunkMixin), so a natural log later growing there counts as natural.
	public static void forget(ChunkAccess chunk, BlockPos pos) {
		Set<Long> placed = chunk.getAttached(PLACED_LOGS);
		if (placed == null || !placed.contains(pos.asLong())) {
			return;
		}

		Set<Long> updated = new HashSet<>(placed);
		updated.remove(pos.asLong());
		if (updated.isEmpty()) {
			chunk.removeAttached(PLACED_LOGS);
		} else {
			chunk.setAttached(PLACED_LOGS, Set.copyOf(updated));
		}
	}

	// Server only (the client doesn't have the list). Never loads a chunk.
	public static boolean isPlaced(LevelAccessor level, BlockPos pos) {
		if (level.isClientSide()) {
			return false;
		}

		ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
		if (chunk == null) {
			return false;
		}

		Set<Long> placed = chunk.getAttached(PLACED_LOGS);
		return placed != null && placed.contains(pos.asLong());
	}
}
