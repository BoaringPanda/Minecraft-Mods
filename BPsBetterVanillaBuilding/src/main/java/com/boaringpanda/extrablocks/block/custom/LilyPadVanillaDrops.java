package com.boaringpanda.extrablocks.block.custom;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * Drops for a combo whose accessory keeps its data in a block entity: a head's skin, a banner's
 * patterns, a custom name. Instead of rebuilding those items by hand, this runs the real vanilla loot
 * tables (the accessory's own, and the lily pad's) against the combo's block entity. Their
 * {@code copy_components} entries then decide exactly what carries over, the same as for the real block.
 */
final class LilyPadVanillaDrops {
	private LilyPadVanillaDrops() {
	}

	/** What the real accessory would drop if it were broken holding this block entity's data. */
	static List<ItemStack> accessory(Block accessory, @Nullable BlockEntity blockEntity) {
		if (blockEntity != null && blockEntity.getLevel() instanceof ServerLevel level) {
			return Block.getDrops(accessory.defaultBlockState(), level, blockEntity.getBlockPos(), blockEntity);
		}

		return List.of(new ItemStack(accessory));
	}

	/**
	 * The lily pad's drops plus the accessory's, for whatever destroys the whole combo without a player
	 * choosing a part: explosions and pistons. Vanilla passes both of those the block entity in
	 * {@code params}, which is where the accessory's table copies its data from. Without this the combo
	 * has no loot table, so an explosion would destroy a player head without dropping anything.
	 */
	static List<ItemStack> padAndAccessory(Block accessory, LootParams.Builder params) {
		List<ItemStack> drops = new ArrayList<>(Blocks.LILY_PAD.defaultBlockState().getDrops(params));
		drops.addAll(accessory.defaultBlockState().getDrops(params));
		return drops;
	}
}
