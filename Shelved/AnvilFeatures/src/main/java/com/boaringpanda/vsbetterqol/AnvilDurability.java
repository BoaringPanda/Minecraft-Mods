package com.boaringpanda.vsbetterqol;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

// Anvils last a fixed number of uses instead of vanilla's 12% chance per use (see AnvilMenuMixin): USES_PER_STAGE uses per stage
// (anvil, chipped, damaged). Each chunk saves how many uses its anvils have taken in their current stage. Missing = none.
public final class AnvilDurability {
	public static final int USES_PER_STAGE = 8;
	public static final int MAX_USES = 3 * USES_PER_STAGE;

	private AnvilDurability() {
	}

	// The map is never changed in place, always replaced, so Fabric notices and saves the chunk. Saved keys must be strings.
	private static final AttachmentType<Map<Long, Integer>> ANVIL_WEAR = AttachmentRegistry.create(
			VSBetterQOL.id("anvil_wear"),
			builder -> builder.persistent(Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.INT)));

	// Loads the class, which registers the attachment before any world loads.
	public static void register() {
	}

	// Called for every anvil use outside creative. Returns true when the stage is used up (vanilla then damages the anvil, and the last stage
	// turns it into a broken anvil, see AnvilMenuMixin).
	// Wear left over past the end of a stage (from addWear) carries into the next one.
	public static boolean use(Level level, BlockPos pos) {
		ChunkAccess chunk = level.getChunk(pos);
		int used = chunk.getAttachedOrElse(ANVIL_WEAR, Map.<Long, Integer>of()).getOrDefault(pos.asLong(), 0) + 1;
		boolean stageUsedUp = used >= USES_PER_STAGE;
		setWear(chunk, pos, stageUsedUp ? used - USES_PER_STAGE : used);
		return stageUsedUp;
	}

	// An extra use with no stage check, for jobs that cost more than one use (repairs, see AnvilMenuMixin). Always followed by use().
	public static void addWear(Level level, BlockPos pos) {
		ChunkAccess chunk = level.getChunk(pos);
		setWear(chunk, pos, chunk.getAttachedOrElse(ANVIL_WEAR, Map.<Long, Integer>of()).getOrDefault(pos.asLong(), 0) + 1);
	}

	private static void setWear(ChunkAccess chunk, BlockPos pos, int used) {
		Map<Long, Integer> updated = new HashMap<>(chunk.getAttachedOrElse(ANVIL_WEAR, Map.of()));
		if (used > 0) {
			updated.put(pos.asLong(), used);
		} else {
			updated.remove(pos.asLong());
		}
		if (updated.isEmpty()) {
			chunk.removeAttached(ANVIL_WEAR);
		} else {
			chunk.setAttached(ANVIL_WEAR, Map.copyOf(updated));
		}
	}

	// Called when the anvil at pos is gone for any reason (LevelChunkMixin), so a new anvil placed there starts fresh.
	public static void forget(ChunkAccess chunk, BlockPos pos) {
		Map<Long, Integer> wear = chunk.getAttached(ANVIL_WEAR);
		if (wear != null && wear.containsKey(pos.asLong())) {
			setWear(chunk, pos, 0);
		}
	}

	// Uses left before the anvil turns broken, or -1 if there's no anvil at pos. Server only.
	public static int usesLeft(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(BlockTags.ANVIL)) {
			return -1;
		}

		// Anvil 3, chipped 2, damaged 1. Other mods' anvils that vanilla's damage() doesn't know count as one stage.
		int stagesLeft = 1;
		for (BlockState next = AnvilBlock.damage(state); next != null; next = AnvilBlock.damage(next)) {
			stagesLeft++;
		}
		int used = level.getChunk(pos).getAttachedOrElse(ANVIL_WEAR, Map.<Long, Integer>of()).getOrDefault(pos.asLong(), 0);
		return stagesLeft * USES_PER_STAGE - used;
	}

	// Synced to the client with the anvil menu. The server works the value out from the anvil; the client (whose access is NULL)
	// keeps what the server sent. -1 until then, or with no anvil, hides the bar.
	public static DataSlot usesLeftSlot(ContainerLevelAccess access) {
		return new DataSlot() {
			private int value = -1;

			@Override
			public int get() {
				return access.evaluate(AnvilDurability::usesLeft).orElse(this.value);
			}

			@Override
			public void set(int value) {
				this.value = value;
			}
		};
	}

	// Implemented by AnvilMenu (AnvilMenuMixin), so the anvil screen can read the synced value.
	public interface View {
		int vsbetterqol$usesLeft();
	}
}
