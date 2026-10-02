package com.boaringpanda.lumberjackmod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

// After Tree Falling fells a tree, its leaves and fungus caps go in a quick random wave (0.4-1.5 s) instead of vanilla's ~1 minute.
// Leaves use vanilla's own decay (LeavesBlock.randomTick), so leaves still held up by another tree's log stay, and drops are
// vanilla's. Wart blocks / shroomlights go unless a natural stem is still within reach through the cap (a neighbouring fungus).
// In memory only: if the server stops mid-wave, leftover leaves just decay at vanilla speed.
public final class FallingCanopy {
	private FallingCanopy() {
	}

	// How far from the felled logs the canopy is searched, through leaves / caps (vanilla leaves decay at distance 7).
	private static final int REACH = 7;
	private static final int MIN_DELAY = 8;
	private static final int MAX_DELAY = 30;
	// Vanilla works out a leaf's new distance one block per tick. Where the canopy touches another tree's leaves, the leaves first
	// take support from those and only climb to 7 step by step, which can take several seconds. So a natural leaf that isn't
	// decaying yet is checked again every RETRY_DELAY ticks until WINDOW ticks after the tree fell. Leaves still not at 7 by then
	// are held up by another tree's log and vanilla would never decay them either.
	private static final int RETRY_DELAY = 5;
	private static final int WINDOW = 200;

	private static final Map<ServerLevel, Map<BlockPos, Waiting>> WAITING = new WeakHashMap<>();

	private static final class Waiting {
		int ticks;
		int windowLeft = WINDOW;

		Waiting(int ticks) {
			this.ticks = ticks;
		}
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(FallingCanopy::tick);
	}

	// Queues the natural canopy around the felled logs.
	public static void drop(ServerLevel level, List<BlockPos> felled) {
		Map<BlockPos, Waiting> waiting = WAITING.computeIfAbsent(level, key -> new HashMap<>());
		RandomSource random = level.getRandom();
		Set<BlockPos> seen = new HashSet<>(felled);
		List<BlockPos> queue = felled;

		for (int step = 0; step < REACH && !queue.isEmpty(); step++) {
			List<BlockPos> next = new ArrayList<>();
			for (BlockPos pos : queue) {
				for (Direction direction : Direction.values()) {
					BlockPos neighbour = pos.relative(direction);
					if (seen.add(neighbour) && TreeFalling.isNaturalCanopy(level, neighbour, level.getBlockState(neighbour))) {
						waiting.putIfAbsent(neighbour, new Waiting(random.nextIntBetweenInclusive(MIN_DELAY, MAX_DELAY)));
						next.add(neighbour);
					}
				}
			}
			queue = next;
		}
	}

	private static void tick(ServerLevel level) {
		Map<BlockPos, Waiting> waiting = WAITING.get(level);
		if (waiting == null || waiting.isEmpty()) {
			return;
		}

		List<BlockPos> due = new ArrayList<>();
		for (Map.Entry<BlockPos, Waiting> entry : waiting.entrySet()) {
			Waiting wait = entry.getValue();
			wait.windowLeft--;
			if (--wait.ticks <= 0) {
				due.add(entry.getKey());
			}
		}

		for (BlockPos pos : due) {
			Waiting wait = waiting.get(pos);
			if (!level.isLoaded(pos) || !fall(level, pos) || wait.windowLeft <= 0) {
				waiting.remove(pos);
			} else {
				wait.ticks = RETRY_DELAY;
			}
		}
	}

	// Drops the block if it should go. True if it's a natural leaf that isn't decaying yet, so worth checking again.
	private static boolean fall(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof LeavesBlock) {
			if (state.getValue(LeavesBlock.PERSISTENT)) {
				return false;
			}
			if (state.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE) {
				return true;
			}
			state.randomTick(level, pos, level.getRandom());
		} else if (state.is(TreeFalling.FUNGUS_CAPS) && !PlacedTreeParts.isPlaced(level, pos) && !nearNaturalStem(level, pos)) {
			level.destroyBlock(pos, true);
		}
		return false;
	}

	// Whether a natural log or stem is within REACH - 1 steps of this cap block, through natural caps.
	private static boolean nearNaturalStem(ServerLevel level, BlockPos start) {
		Set<BlockPos> seen = new HashSet<>();
		seen.add(start);
		List<BlockPos> queue = List.of(start);

		for (int step = 0; step < REACH - 1 && !queue.isEmpty(); step++) {
			List<BlockPos> next = new ArrayList<>();
			for (BlockPos pos : queue) {
				for (Direction direction : Direction.values()) {
					BlockPos neighbour = pos.relative(direction);
					if (!seen.add(neighbour)) {
						continue;
					}

					BlockState state = level.getBlockState(neighbour);
					if (state.is(BlockTags.LOGS) && !PlacedTreeParts.isPlaced(level, neighbour)) {
						return true;
					}
					if (state.is(TreeFalling.FUNGUS_CAPS) && !PlacedTreeParts.isPlaced(level, neighbour)) {
						next.add(neighbour);
					}
				}
			}
			queue = next;
		}
		return false;
	}
}
