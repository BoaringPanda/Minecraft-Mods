package com.boaringpanda.bettervanillafeatures;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;

public final class CropHarvesting {
	private CropHarvesting() {
	}

	// One right-click harvest. pos/state is where the drops come from (the lower half of a pitcher), replant is the freshly planted
	// crop. The seed comes out of the drops, or out of the inventory for pitcher and torchflower, which drop no seed when grown.
	private record Harvest(BlockPos pos, BlockState state, BlockState replant, Item seed, boolean seedFromDrops) {
	}

	// Called from vanilla's "use block with empty hand" step (see BlockBehaviourMixin), the same place sweet berry bushes are picked.
	// True means a grown crop was clicked: the client just swings the arm, the server harvests and replants.
	public static boolean harvest(BlockState state, Level level, BlockPos pos, Player player) {
		Harvest harvest = find(state, level, pos);
		if (harvest == null) {
			return false;
		}

		if (level instanceof ServerLevel serverLevel) {
			harvest(serverLevel, player, harvest);
		}
		return true;
	}

	private static @Nullable Harvest find(BlockState state, Level level, BlockPos pos) {
		// CropBlock covers wheat, carrots, potatoes, beetroots and other mods' crops. The torchflower crop never reaches max age here,
		// it turns into a torchflower instead (handled below).
		if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
			return new Harvest(pos, state, crop.getStateForAge(0), seedOf(state, level, pos), true);
		}
		if (state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE) {
			return new Harvest(pos, state, state.setValue(NetherWartBlock.AGE, 0), seedOf(state, level, pos), true);
		}
		if (state.getBlock() instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE) {
			return new Harvest(pos, state, state.setValue(CocoaBlock.AGE, 0), seedOf(state, level, pos), true);
		}
		// Only where a torchflower seed could be planted (farmland), so decorative torchflowers aren't picked.
		if (state.is(Blocks.TORCHFLOWER)) {
			BlockState crop = Blocks.TORCHFLOWER_CROP.defaultBlockState();
			return crop.canSurvive(level, pos) ? new Harvest(pos, state, crop, Items.TORCHFLOWER_SEEDS, false) : null;
		}
		// Either half can be clicked; the drops come from the lower one.
		if (state.is(Blocks.PITCHER_CROP) && state.getValue(PitcherCropBlock.AGE) == PitcherCropBlock.MAX_AGE) {
			BlockPos lowerPos = state.getValue(PitcherCropBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
			BlockState lower = level.getBlockState(lowerPos);
			if (lower.is(Blocks.PITCHER_CROP) && lower.getValue(PitcherCropBlock.HALF) == DoubleBlockHalf.LOWER) {
				return new Harvest(lowerPos, lower, Blocks.PITCHER_CROP.defaultBlockState(), Items.PITCHER_POD, false);
			}
		}
		return null;
	}

	// Pick-block item, which vanilla makes the crop's seed (wheat seeds, carrot, nether wart, cocoa beans, ...).
	private static Item seedOf(BlockState state, Level level, BlockPos pos) {
		return state.getCloneItemStack(level, pos, false).getItem();
	}

	private static void harvest(ServerLevel level, Player player, Harvest harvest) {
		// Same drops as breaking it with the held item, so Fortune counts.
		List<ItemStack> drops = Block.getDrops(harvest.state(), level, harvest.pos(), null, player, player.getMainHandItem());
		boolean replant = harvest.seedFromDrops() ? takeSeed(drops, harvest.seed()) : takeSeed(player, harvest.seed());
		drops.forEach(stack -> Block.popResource(level, harvest.pos(), stack));

		// Vanilla's break sound and particles, for everyone nearby.
		harvest.state().getBlock().spawnDestroyParticles(level, harvest.pos(), harvest.state());
		BlockState newState = replant ? harvest.replant() : Blocks.AIR.defaultBlockState();
		level.setBlock(harvest.pos(), newState, Block.UPDATE_ALL);

		// The lower half has to change first: vanilla breaks a grown lower half whose top goes missing.
		if (harvest.state().is(Blocks.PITCHER_CROP)) {
			BlockPos upperPos = harvest.pos().above();
			BlockState upper = level.getBlockState(upperPos);
			if (upper.is(Blocks.PITCHER_CROP)) {
				upper.getBlock().spawnDestroyParticles(level, upperPos, upper);
				level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
		}

		level.gameEvent(GameEvent.BLOCK_CHANGE, harvest.pos(), GameEvent.Context.of(player, newState));
	}

	private static boolean takeSeed(List<ItemStack> drops, Item seed) {
		for (ItemStack stack : drops) {
			if (stack.is(seed)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	// Creative needs no seed; survival uses one from anywhere in the inventory.
	private static boolean takeSeed(Player player, Item seed) {
		if (player.hasInfiniteMaterials()) {
			return true;
		}
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(seed)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}
}
