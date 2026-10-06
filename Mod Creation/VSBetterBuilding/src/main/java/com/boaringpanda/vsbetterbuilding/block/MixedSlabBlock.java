package com.boaringpanda.vsbetterbuilding.block;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Two different slabs sharing one block (see {@link MixedSlabs}). It has no item: it only exists by stacking one slab on another.
 * Placing is in {@code SlabBlockMixin} and {@code BlockItemMixin}.
 */
public class MixedSlabBlock extends BaseEntityBlock {
	public MixedSlabBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MixedSlabBlockEntity(pos, state);
	}

	/** Mines exactly like the tougher slab: its hardness, its tool, its "needs the right tool" slow-down. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return MixedSlabs.halves(level, pos).toughest().getDestroyProgress(player, level, pos);
	}

	/**
	 * Each slab drops by its own rule, as if broken on its own with the same tool: breaking oak and stone by hand drops only the oak
	 * slab. The block itself doesn't need a tool (so vanilla always calls this), because the check is made here per slab.
	 */
	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
			ItemStack destroyedWith) {
		player.awardStat(Stats.BLOCK_MINED.get(this));
		player.causeFoodExhaustion(0.005F);
		MixedSlabs.Halves halves = blockEntity instanceof MixedSlabBlockEntity entity ? entity.halves() : MixedSlabs.Halves.FALLBACK;
		for (BlockState half : List.of(halves.bottomState(), halves.topState())) {
			if (!half.requiresCorrectToolForDrops() || destroyedWith.isCorrectToolForDrops(half)) {
				dropResources(half, level, pos, null, player, destroyedWith);
			}
		}
	}

	/** Both slabs' own loot, for breaks that aren't a player mining it (explosions, a piston crushing it, {@code /setblock destroy}). */
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		MixedSlabs.Halves halves = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MixedSlabBlockEntity entity
				? entity.halves()
				: MixedSlabs.Halves.FALLBACK;
		List<ItemStack> drops = new ArrayList<>(halves.bottomState().getDrops(params));
		drops.addAll(halves.topState().getDrops(params));
		return drops;
	}

	/**
	 * The break sound and particles (vanilla's level event 2001) are the slab the breaking player is looking at, or the top slab if it
	 * wasn't a player. It's sent as that slab's double state so the particles fill the whole block, which is what's breaking.
	 */
	@Override
	public void spawnDestroyByEntityParticles(Level level, @Nullable Entity entity, BlockPos pos, BlockState state) {
		BlockState half = entity instanceof Player player ? MixedSlabs.targeted(level, pos, player) : MixedSlabs.halves(level, pos).topState();
		super.spawnDestroyByEntityParticles(level, entity, pos, half.setValue(SlabBlock.TYPE, SlabType.DOUBLE));
	}

	/** Middle-click picks the slab under the cursor ({@code ServerGamePacketListenerImplMixin}); anything else asking gets the top. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return MixedSlabs.halves(level, pos).topState().getCloneItemStack(level, pos, false);
	}
}
