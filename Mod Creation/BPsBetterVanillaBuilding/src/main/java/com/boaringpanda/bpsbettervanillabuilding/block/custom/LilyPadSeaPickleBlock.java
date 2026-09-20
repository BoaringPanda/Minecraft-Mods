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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadTarget;

/**
 * A sea pickle cluster standing on a lily pad, with real stacking (1-4), not
 * just a picture. Always non-waterlogged (sitting on top of the pad, not
 * submerged), so it uses the "dry" appearance and doesn't glow - matching
 * a real sea pickle taken out of water.
 * <p>
 * Extends the real {@link SeaPickleBlock} to inherit its {@code PICKLES}/
 * {@code WATERLOGGED} blockstate properties. Stacking isn't inherited
 * though - vanilla's own "click with another pickle to add one" check
 * compares the held item against {@code this.asItem()}, which is
 * {@code AIR} for a block with no registered {@code BlockItem}. Handled
 * explicitly by {@link com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessoryInteraction}
 * instead, same as the initial lily-pad-to-pickle combine.
 */
public class LilyPadSeaPickleBlock extends SeaPickleBlock implements LilyPadCombo {
	/** Indexed by pickle count - 1. Pickle shapes are square, so the pad's spin doesn't matter. */
	private final LilyPadShapes[] shapes = new LilyPadShapes[4];

	public LilyPadSeaPickleBlock(Properties properties) {
		super(properties);

		for (int count = 1; count <= 4; count++) {
			BlockState state = this.defaultBlockState().setValue(PICKLES, count);
			VoxelShape pickles = super.getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
			this.shapes[count - 1] = LilyPadShapes.solid(pickles);
		}
	}

	@Override
	public List<ItemStack> accessoryDrops(BlockState state, @Nullable BlockEntity blockEntity) {
		return List.of(new ItemStack(Blocks.SEA_PICKLE, state.getValue(PICKLES)));
	}

	@Override
	public VoxelShape accessoryShape(BlockState state) {
		return this.shapes[state.getValue(PICKLES) - 1].accessory();
	}

	/** The count decides where the particles appear. */
	@Override
	public BlockState accessoryState(BlockState state) {
		return Blocks.SEA_PICKLE.defaultBlockState()
				.setValue(PICKLES, state.getValue(PICKLES))
				.setValue(WATERLOGGED, state.getValue(WATERLOGGED));
	}

	@Override
	public void spawnDestroyByEntityParticles(Level level, @Nullable Entity entity, BlockPos pos, BlockState state) {
		LilyPadTarget.spawnDestroyParticles(this, level, entity, pos, state);
	}

	/** One sea pickle, as middle-clicking a real cluster gives. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(Blocks.SEA_PICKLE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LilyPadTarget.outline(this, state, pos, context, this.shapes[state.getValue(PICKLES) - 1].outline());
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return LilyPadTarget.destroyProgress(this, state, player, pos, super.getDestroyProgress(state, player, level, pos));
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		// Never pops off - this only ever exists on top of a lily pad, placed by our
		// own interaction code, not vanilla's normal sea-pickle-placement rules
		// (which require being on top of water).
		return true;
	}
}
