package com.boaringpanda.bpsbettervanillabuilding.block.custom;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadTarget;

/**
 * A lily pad with a torch, lantern, flower pot, etc. standing on it, in the same
 * block space. Purely visual - the blockstate JSON points at one hand-built model
 * that merges the lily pad's geometry with the accessory's, so no BlockEntity or
 * custom renderer is needed.
 * <p>
 * Never placed directly - see {@link com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessoryInteraction}
 * for how it's created by right-clicking a placed lily pad with the item.
 * <p>
 * Nothing here handles a player breaking it: that goes through
 * {@link com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessoryBreaking}, which removes either just the
 * accessory (see {@link #accessoryDrops}) or the whole thing, depending on which part is aimed at.
 */
public class LilyPadAccessoryBlock extends Block implements LilyPadCombo {
	private final Block accessory;
	private final BlockState accessoryState;
	private final LilyPadShapes shapes;

	public LilyPadAccessoryBlock(Properties properties, Block accessory) {
		this(properties, accessory, accessory);
	}

	/**
	 * @param shapeSource the vanilla block that stands on the pad - usually the accessory itself, but not
	 *                    for a potted plant (the pot's shape, and the pot's particles and sounds, which is
	 *                    what a real potted plant has too).
	 */
	public LilyPadAccessoryBlock(Properties properties, Block accessory, Block shapeSource) {
		super(properties);
		this.accessory = accessory;
		this.accessoryState = shapeSource.defaultBlockState();

		this.shapes = LilyPadShapes.of(
				this.accessoryState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
				this.accessoryState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
		);
	}

	public Block accessory() {
		return this.accessory;
	}

	@Override
	public List<ItemStack> accessoryDrops(BlockState state, @Nullable BlockEntity blockEntity) {
		return List.of(new ItemStack(this.accessory));
	}

	@Override
	public VoxelShape accessoryShape(BlockState state) {
		return this.shapes.accessory();
	}

	@Override
	public BlockState accessoryState(BlockState state) {
		return this.accessoryState;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LilyPadTarget.outline(this, state, pos, context, this.shapes.outline());
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.shapes.collision();
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return LilyPadTarget.destroyProgress(this, state, player, pos, super.getDestroyProgress(state, player, level, pos));
	}

	@Override
	public void spawnDestroyByEntityParticles(Level level, @Nullable Entity entity, BlockPos pos, BlockState state) {
		LilyPadTarget.spawnDestroyParticles(this, level, entity, pos, state);
	}

	/** The accessory's item, as middle-clicking the real block gives - for a potted plant, the plant. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.accessory);
	}
}
