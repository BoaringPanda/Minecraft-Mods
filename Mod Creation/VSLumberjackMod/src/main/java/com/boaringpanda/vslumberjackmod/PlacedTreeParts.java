package com.boaringpanda.vslumberjackmod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

// Remembers which tree parts (#vslumberjackmod:tree_parts: logs, wart blocks, shroomlights) players placed, so Tree Falling never
// fells or strips a build. Saved with each chunk as a list of positions.
public final class PlacedTreeParts {
	private PlacedTreeParts() {
	}

	public static final TagKey<Block> TREE_PARTS = TagKey.create(Registries.BLOCK, VSLumberjackMod.id("tree_parts"));

	// The set is never changed in place, always replaced, so Fabric notices and saves the chunk.
	private static final AttachmentType<Set<Long>> PLACED = AttachmentRegistry.create(
			VSLumberjackMod.id("placed_tree_parts"),
			builder -> builder.persistent(Codec.LONG.listOf().xmap(Set::copyOf, List::copyOf)));

	// Loads the class, which registers the attachment before any world loads.
	public static void register() {
	}

	// Called when a player places a tree part (BlockItemMixin).
	public static void remember(Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return;
		}

		ChunkAccess chunk = level.getChunk(pos);
		Set<Long> placed = chunk.getAttachedOrElse(PLACED, Set.of());
		if (!placed.contains(pos.asLong())) {
			Set<Long> updated = new HashSet<>(placed);
			updated.add(pos.asLong());
			chunk.setAttached(PLACED, Set.copyOf(updated));
		}
	}

	// Called when the tree part at pos is gone for any reason (LevelChunkMixin), so a natural tree later growing there is natural.
	public static void forget(ChunkAccess chunk, BlockPos pos) {
		Set<Long> placed = chunk.getAttached(PLACED);
		if (placed == null || !placed.contains(pos.asLong())) {
			return;
		}

		Set<Long> updated = new HashSet<>(placed);
		updated.remove(pos.asLong());
		if (updated.isEmpty()) {
			chunk.removeAttached(PLACED);
		} else {
			chunk.setAttached(PLACED, Set.copyOf(updated));
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

		Set<Long> placed = chunk.getAttached(PLACED);
		return placed != null && placed.contains(pos.asLong());
	}
}
