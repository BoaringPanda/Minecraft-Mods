package com.boaringpanda.bpsbettervanillabuilding.block.custom;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bpsbettervanillabuilding.block.StackedHeads;
import com.boaringpanda.bpsbettervanillabuilding.block.StackedHeadsTarget;

/**
 * Two floor heads in one block space: one in the bottom half, one in the top half (a head is half a block tall).
 * <p>
 * Nothing about the heads lives here. They are held by the {@link StackedHeadsBlockEntity}, which keeps two real
 * vanilla skull block entities, and drawn by vanilla's own skull renderer (see the client's
 * {@code StackedHeadsRenderer}). This block is only what the world sees: the shape, redstone power, drops, and
 * being a block at all. It is never held or placed directly, so it has no item; it only ever appears when a head is
 * put on top of another head ({@link com.boaringpanda.bpsbettervanillabuilding.block.StackedHeadsInteraction}).
 * <p>
 * Like a real head it needs no support and is destroyed by pistons. Breaking removes one head at a time, see
 * {@link com.boaringpanda.bpsbettervanillabuilding.block.StackedHeadsBreaking}.
 */
public class StackedHeadsBlock extends BaseEntityBlock {
	/** The same property as a real head's, so the powered dragon and piglin animation works the same way. */
	public static final BooleanProperty POWERED = AbstractSkullBlock.POWERED;

	/** Two 8-pixel columns, only used if the block entity is missing. */
	private static final VoxelShape FALLBACK_SHAPE = Block.column(8.0, 0.0, 16.0);

	public StackedHeadsBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StackedHeadsBlockEntity(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (level.getBlockEntity(pos) instanceof StackedHeadsBlockEntity heads) {
			return StackedHeadsTarget.outline(heads, pos, context);
		}

		return FALLBACK_SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (level.getBlockEntity(pos) instanceof StackedHeadsBlockEntity heads) {
			return Shapes.or(heads.shape(false, true), heads.shape(true, true));
		}

		return FALLBACK_SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	/** Vanilla's own rule for a head ({@code AbstractSkullBlock.neighborChanged}): powered when any neighbour is. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		if (!level.isClientSide()) {
			boolean powered = level.hasNeighborSignal(pos);
			if (powered != state.getValue(POWERED)) {
				level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
			}
		}
	}

	/**
	 * Both heads' drops, for whatever destroys the whole block without a player choosing one: explosions and pistons.
	 * Vanilla passes both of those the block entity in {@code params}, which is where each head's loot table copies
	 * its skin from. Without this the block has no loot table, so an explosion would delete both heads.
	 */
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof StackedHeadsBlockEntity heads) {
			List<ItemStack> drops = new ArrayList<>(heads.drops(false));
			drops.addAll(heads.drops(true));
			return drops;
		}

		return List.of();
	}

	/**
	 * A fallback only: middle-click is answered by {@link com.boaringpanda.bpsbettervanillabuilding.block.StackedHeadsBreaking},
	 * which picks the head being aimed at.
	 */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		if (level.getBlockEntity(pos) instanceof StackedHeadsBlockEntity heads) {
			SkullBlockEntity head = heads.has(false) ? heads.head(false) : heads.head(true);
			if (head != null) {
				return new ItemStack(head.getBlockState().getBlock());
			}
		}

		return new ItemStack(Blocks.SKELETON_SKULL);
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide()
				? createTickerHelper(type, StackedHeads.BLOCK_ENTITY, StackedHeadsBlockEntity::clientTick)
				: null;
	}
}
