package com.boaringpanda.bpsbettervanillabuilding.block.custom;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bpsbettervanillabuilding.block.MixedSlabTarget;

/**
 * A full-size block made of two different slabs stacked into the same block
 * space - one material on the bottom half, a different one on top.
 * <p>
 * Purely visual: rendering is handled entirely by the {@code multipart}
 * blockstate JSON layering the two vanilla half-slab models on top of each
 * other, so this class only needs to describe its drops. Its shape is the
 * default full cube.
 * <p>
 * Never placed directly - it has no {@code BlockItem} and never appears in
 * the creative inventory. It only ever appears as the result of combining
 * two different slabs, which happens wherever vanilla would merge two of the same slab, see
 * {@link com.boaringpanda.bpsbettervanillabuilding.mixin.SlabBlockMixin}.
 */
public class MixedSlabBlock extends Block {
	private final Block bottomSlab;
	private final Block topSlab;
	/** Whichever of the two slabs is tougher: it decides the tool, the mining speed and whether a tool is needed to get drops. */
	private final Block toughest;

	public MixedSlabBlock(Properties properties, Block bottomSlab, Block topSlab) {
		super(properties);
		this.bottomSlab = bottomSlab;
		this.topSlab = topSlab;
		this.toughest = toughestOf(bottomSlab, topSlab);
	}

	/**
	 * The tougher of two slabs, which is what a combined block mines like. Ranked in this order, and the top slab wins a
	 * full tie: first a slab that needs a tool to drop anything (stone, terracotta, concrete, copper) beats one that doesn't
	 * (wood, wool), then the harder one, then the one that resists explosions more. So stone plus wool is stone, oak plus
	 * terracotta is terracotta (a pickaxe), and oak plus wool is oak (an axe).
	 */
	public static Block toughestOf(Block bottomSlab, Block topSlab) {
		BlockState bottom = bottomSlab.defaultBlockState();
		BlockState top = topSlab.defaultBlockState();

		int byTool = Boolean.compare(bottom.requiresCorrectToolForDrops(), top.requiresCorrectToolForDrops());
		if (byTool != 0) {
			return byTool > 0 ? bottomSlab : topSlab;
		}

		int byHardness = Float.compare(hardnessOf(bottomSlab), hardnessOf(topSlab));
		if (byHardness != 0) {
			return byHardness > 0 ? bottomSlab : topSlab;
		}

		return Float.compare(bottomSlab.getExplosionResistance(), topSlab.getExplosionResistance()) > 0 ? bottomSlab : topSlab;
	}

	/** How long a slab takes to mine before tools, read the same way vanilla does, so it works before any world exists. */
	public static float hardnessOf(Block slab) {
		return slab.defaultBlockState().getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
	}

	public Block bottomSlab() {
		return this.bottomSlab;
	}

	public Block topSlab() {
		return this.topSlab;
	}

	/**
	 * Middle-click with no aim to go on (something other than a player picking, or a pick that missed the block): the
	 * bottom slab. Without this the default is this block's own item, which doesn't exist, so a pick would give nothing.
	 * A player's pick is answered by {@link com.boaringpanda.bpsbettervanillabuilding.block.MixedSlabPicking}, which gives
	 * the slab they are pointing at.
	 */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.bottomSlab);
	}

	/**
	 * The sound for everything that has no better answer: footsteps and landings, which are the top slab, since that is the
	 * surface you stand on. The sounds that do have a better answer are handled separately: placing plays the slab that was
	 * just placed ({@link com.boaringpanda.bpsbettervanillabuilding.mixin.BlockItemMixin}), and breaking and mining play the
	 * slab being looked at ({@link #spawnDestroyByEntityParticles} and the client's {@code ClientLevelMixin}).
	 */
	@Override
	protected SoundType getSoundType(BlockState state) {
		return this.topSlab.defaultBlockState().getSoundType();
	}

	/**
	 * The break sound and particles come from the slab the breaker is looking at, not from this block (which would give
	 * one sound for both). Vanilla's break sequence calls this on the server and on the breaker's own client, and it plays
	 * the sound and spawns the particles of whatever block state ID it is given, so giving it the aimed slab's real state
	 * is enough. Lily pad combos do the same, see {@code LilyPadTarget#spawnDestroyParticles}. With no player (fire, an
	 * explosion) it is the top slab.
	 */
	@Override
	public void spawnDestroyByEntityParticles(Level level, @Nullable Entity entity, BlockPos pos, BlockState state) {
		Block slab = MixedSlabTarget.aimedSlab(this, entity instanceof Player player ? player : null, pos);
		level.levelEvent(entity, LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(slab.defaultBlockState()));
	}

	/**
	 * Mining speed is exactly what mining the tougher slab gives with this tool: an axe on wood, shears on wool, a
	 * pickaxe on stone. Asking the real slab's state means all of vanilla's tool rules (which key off that slab's block tags)
	 * apply as they do to the slab itself, so nothing about tools is copied onto this block. The combination is only
	 * registered with the tougher slab's hardness, and never with {@code requiresCorrectToolForDrops()}: vanilla only
	 * reaches {@link #playerDestroy} if the held tool is right for <em>this</em> block's own tags, so leaving it off makes
	 * vanilla always get there, and the check that matters is made below against the real slab.
	 */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return this.toughest.defaultBlockState().getDestroyProgress(player, level, pos);
	}

	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);

		// The drops follow the tougher slab's own rule: nothing in creative, and nothing if that slab needs a tool
		// (stone, terracotta...) and the one used isn't right for it. A wood or wool slab drops with anything.
		BlockState toughestState = this.toughest.defaultBlockState();
		boolean shouldDrop = !player.isCreative() && (!toughestState.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(toughestState));
		if (shouldDrop) {
			popResource(level, pos, new ItemStack(this.bottomSlab));
			popResource(level, pos, new ItemStack(this.topSlab));
		}
	}
}
