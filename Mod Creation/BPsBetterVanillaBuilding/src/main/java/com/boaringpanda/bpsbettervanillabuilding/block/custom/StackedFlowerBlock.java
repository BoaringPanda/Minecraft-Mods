package com.boaringpanda.bpsbettervanillabuilding.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EyeblossomBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bpsbettervanillabuilding.block.StackedFlowers;

/**
 * Two, three or four of the same small flower in one block, the way a candle or a sea pickle stacks. A single flower is
 * still the ordinary vanilla block; this only exists once a second one is added, so nothing about flowers already in a
 * world changes.
 * <p>
 * It extends {@link FlowerBlock} on purpose, built with the vanilla flower's own suspicious stew effects, so everything that
 * treats a flower as a flower (bees, the flower tags) still does. What it does <em>not</em> copy it asks the real flower
 * for: whether it can stand here ({@link #canSurvive}, so a wither rose still needs its nether blocks and the rest need
 * grass or dirt), what it does when something is inside it (the wither rose's wither effect), its particles, and its
 * bee effect. It has no item: it is only made by right-clicking a flower with the same flower, see
 * {@link com.boaringpanda.bpsbettervanillabuilding.mixin.BlockItemMixin}, and breaking it drops the whole stack.
 */
public class StackedFlowerBlock extends FlowerBlock {
	public static final IntegerProperty FLOWERS = IntegerProperty.create("flowers", 2, 4);

	/** The four flowers fill the block's quadrants and a stack has no position offset, so this is centred on the block. */
	private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 12.0);

	/** The ordinary single flower this stack is made of. */
	private final FlowerBlock flower;

	public StackedFlowerBlock(Properties properties, FlowerBlock flower) {
		super(flower.getSuspiciousEffects(), properties);
		this.flower = flower;
		this.registerDefaultState(this.stateDefinition.any().setValue(FLOWERS, 2));
	}

	public FlowerBlock flower() {
		return this.flower;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FLOWERS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Wherever the real flower can stand, and only there. Also what makes the stack pop off when its ground goes. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return this.flower.defaultBlockState().canSurvive(level, pos);
	}

	/**
	 * Vanilla's own rule for adding to a candle or sea pickle, for a flower: not sneaking, holding the same flower, and fewer than
	 * four here already. Vanilla asks this of the block in the space being placed into, so it holds for a click on any face of
	 * the flowers, on the ground under them and on a block next to them.
	 */
	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		if (!context.isSecondaryUseActive() && context.getItemInHand().is(this.flower.asItem()) && state.getValue(FLOWERS) < 4) {
			return true;
		}

		return super.canBeReplaced(state, context);
	}

	/** One flower, as middle-clicking a real flower gives. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.flower);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean precise) {
		this.flower.defaultBlockState().entityInside(level, pos, entity, applier, precise);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		this.flower.animateTick(this.flower.defaultBlockState(), level, pos, random);
	}

	@Override
	public MobEffectInstance getBeeInteractionEffect() {
		return this.flower.getBeeInteractionEffect();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		this.flower.defaultBlockState().randomTick(level, pos, random);
		this.keepTheStack(state, level, pos);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		this.flower.defaultBlockState().tick(level, pos, random);
		this.keepTheStack(state, level, pos);
	}

	/**
	 * An eyeblossom opens at night and closes by day, and vanilla does that by replacing the block with a <em>single</em> one
	 * of the other kind. That would turn a stack of four into one, so the vanilla logic is run as usual (it plays the
	 * sound and the particles too) and then, if it did swap the block, the matching stack of the other kind goes back in with the
	 * same count. Nothing else in the mod's flowers changes state by itself, so nothing else needs this.
	 */
	private void keepTheStack(BlockState before, ServerLevel level, BlockPos pos) {
		if (!(this.flower instanceof EyeblossomBlock)) {
			return;
		}

		BlockState now = level.getBlockState(pos);
		if (now.getBlock() != this && now.getBlock() instanceof EyeblossomBlock changed) {
			StackedFlowerBlock other = StackedFlowers.of(changed);
			if (other != null) {
				level.setBlock(pos, other.defaultBlockState().setValue(FLOWERS, before.getValue(FLOWERS)), Block.UPDATE_ALL);
			}
		}
	}
}
