package com.boaringpanda.lumberjackmod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

// Tree Falling (data: data/lumberjackmod/enchantment/tree_falling.json): chopping one log of a tree with the enchanted axe chops
// the rest of the tree too, each log through vanilla's own player-break code (drops, durability, Unbreaking, hunger, stats).
// Sneaking chops just the one log, and it does nothing in creative. Player-placed logs never count (PlacedTreeParts), and a group of logs only counts as a tree when
// it touches natural leaves or natural wart blocks / shroomlights (Nether huge fungi). Leaves and caps follow in FallingCanopy.
public final class TreeFalling {
	private TreeFalling() {
	}

	public static final ResourceKey<Enchantment> TREE_FALLING = ResourceKey.create(Registries.ENCHANTMENT, LumberjackMod.id("tree_falling"));
	public static final TagKey<Block> FUNGUS_CAPS = TagKey.create(Registries.BLOCK, LumberjackMod.id("fungus_caps"));

	// Logs only join the tree if they're the same wood as the chopped one (natural and stripped pieces together), so two
	// different trees touching don't fall as one. Logs outside these tags (modded wood) only join the exact same block.
	private static final List<TagKey<Block>> WOODS = Stream.of(
					"oak_logs", "spruce_logs", "birch_logs", "jungle_logs", "acacia_logs", "dark_oak_logs", "pale_oak_logs",
					"mangrove_logs", "cherry_logs", "poplar_logs", "crimson_stems", "warped_stems")
			.map(name -> TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(name)))
			.toList();

	// Biggest tree that falls in one go (a giant jungle tree is ~150 logs).
	private static final int MAX_LOGS = 512;

	// The rest of the tree, found in BEFORE (while the chopped log still exists) and chopped in AFTER.
	private static final Map<UUID, List<BlockPos>> PENDING = new HashMap<>();

	// True while this mod is chopping the rest of a tree, so those breaks don't start felling themselves.
	private static boolean felling;

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!felling) {
				PENDING.remove(player.getUUID());
				if (canFell(level, player, pos, state)) {
					List<BlockPos> rest = findTree(level, pos, state);
					if (!rest.isEmpty()) {
						PENDING.put(player.getUUID(), rest);
					}
				}
			}
			return true;
		});
		PlayerBlockBreakEvents.CANCELED.register((level, player, pos, state, blockEntity) -> {
			if (!felling) {
				PENDING.remove(player.getUUID());
			}
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!felling) {
				List<BlockPos> rest = PENDING.remove(player.getUUID());
				if (rest != null && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
					fell(serverPlayer, serverLevel, pos, rest);
				}
			}
		});
	}

	private static boolean canFell(Level level, Player player, BlockPos pos, BlockState state) {
		// Does nothing in creative, like Efficiency (Dylan's call).
		return player instanceof ServerPlayer
				&& !player.isCreative()
				&& !player.isShiftKeyDown()
				&& state.is(BlockTags.LOGS)
				&& !PlacedTreeParts.isPlaced(level, pos)
				&& hasTreeFalling(level, player.getMainHandItem());
	}

	private static boolean hasTreeFalling(Level level, ItemStack tool) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(TREE_FALLING)
				.map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, tool) > 0)
				.orElse(false);
	}

	// Every natural log of the same wood connected to start (diagonals too, for acacia and cherry branches), nearest first, not
	// counting start itself. Empty if they don't touch a natural canopy, so log builds without leaves on them never fall.
	private static List<BlockPos> findTree(Level level, BlockPos start, BlockState startState) {
		Predicate<BlockState> sameWood = sameWood(startState);
		Set<BlockPos> seen = new HashSet<>();
		seen.add(start);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		List<BlockPos> logs = new ArrayList<>();
		boolean touchesCanopy = false;

		while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
			BlockPos pos = queue.poll();
			for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
				if (seen.contains(next)) {
					continue;
				}

				BlockState state = level.getBlockState(next);
				if (sameWood.test(state) && !PlacedTreeParts.isPlaced(level, next)) {
					BlockPos log = next.immutable();
					seen.add(log);
					queue.add(log);
					logs.add(log);
				} else if (!touchesCanopy && isNaturalCanopy(level, next, state)) {
					touchesCanopy = true;
				}
			}
		}
		return touchesCanopy ? logs : List.of();
	}

	private static Predicate<BlockState> sameWood(BlockState startState) {
		for (TagKey<Block> wood : WOODS) {
			if (startState.is(wood)) {
				return state -> state.is(wood);
			}
		}
		Block block = startState.getBlock();
		return state -> state.is(block);
	}

	// Leaves that grew naturally (player-placed leaves are PERSISTENT), or wart blocks / shroomlights a player didn't place.
	public static boolean isNaturalCanopy(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof LeavesBlock) {
			return !state.getValue(LeavesBlock.PERSISTENT);
		}
		return state.is(FUNGUS_CAPS) && !PlacedTreeParts.isPlaced(level, pos);
	}

	private static void fell(ServerPlayer player, ServerLevel level, BlockPos start, List<BlockPos> rest) {
		List<BlockPos> felled = new ArrayList<>();
		felled.add(start);
		felling = true;
		try {
			for (BlockPos pos : rest) {
				// The axe broke on the last log.
				if (player.getMainHandItem().isEmpty()) {
					break;
				}

				BlockState state = level.getBlockState(pos);
				if (player.gameMode.destroyBlock(pos)) {
					// Vanilla sends the break particles and sound to everyone but the breaker (their client shows its own for the
					// block it mined), so they'd see nothing for the rest of the tree.
					player.connection.send(new ClientboundLevelEventPacket(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(state), false));
					felled.add(pos);
				}
			}
		} finally {
			felling = false;
		}
		FallingCanopy.drop(level, felled);
	}
}
