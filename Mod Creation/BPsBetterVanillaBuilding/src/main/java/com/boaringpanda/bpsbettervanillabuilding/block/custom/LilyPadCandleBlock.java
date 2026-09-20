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
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadTarget;

/**
 * A candle (any of the 17 colors) standing on a lily pad, with the real
 * stacking (1-4), lighting and extinguishing behaviour, not just a picture.
 * <p>
 * Extends the real {@link CandleBlock} to inherit its {@code CANDLES}/
 * {@code LIT}/{@code WATERLOGGED} blockstate properties and (via the
 * {@code minecraft:candles} block tag it's registered into, see
 * {@link com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessories}) flint-and-steel lighting, for free.
 * <p>
 * What it does NOT inherit: vanilla's own "click with another candle to add
 * one" check compares the held item against {@code this.asItem()}, which is
 * {@code AIR} for a block with no registered {@code BlockItem} - so it never
 * matches. Stacking is instead handled explicitly by
 * {@link com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessoryInteraction},
 * the same way the initial lily-pad-to-candle combine is.
 */
public class LilyPadCandleBlock extends CandleBlock implements LilyPadCombo {
	/** The real vanilla candle block for this color - used for drops and for matching the held item when stacking. */
	private final Block accessory;

	/** Indexed by candle count - 1. */
	private final LilyPadShapes[] shapes = new LilyPadShapes[4];

	public LilyPadCandleBlock(Properties properties, Block accessory) {
		super(properties);
		this.accessory = accessory;

		for (int count = 1; count <= 4; count++) {
			BlockState state = this.defaultBlockState().setValue(CANDLES, count);
			VoxelShape candles = super.getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
			// The blockstate spins the whole model by the pad's position-seeded 0/90/180/270, which a
			// shape can't cheaply reproduce, and 2-4 candles aren't square - so use all four turns.
			VoxelShape anyTurn = Shapes.or(candles, Shapes.rotateHorizontal(candles).values().toArray(new VoxelShape[0]));
			this.shapes[count - 1] = LilyPadShapes.solid(anyTurn);
		}
	}

	public Block accessory() {
		return this.accessory;
	}

	@Override
	public List<ItemStack> accessoryDrops(BlockState state, @Nullable BlockEntity blockEntity) {
		return List.of(new ItemStack(this.accessory, state.getValue(CANDLES)));
	}

	@Override
	public VoxelShape accessoryShape(BlockState state) {
		return this.shapes[state.getValue(CANDLES) - 1].accessory();
	}

	/** The count decides where the particles appear, and lit or not changes the texture. */
	@Override
	public BlockState accessoryState(BlockState state) {
		return this.accessory.defaultBlockState()
				.setValue(CANDLES, state.getValue(CANDLES))
				.setValue(LIT, state.getValue(LIT));
	}

	@Override
	public void spawnDestroyByEntityParticles(Level level, @Nullable Entity entity, BlockPos pos, BlockState state) {
		LilyPadTarget.spawnDestroyParticles(this, level, entity, pos, state);
	}

	/** One candle, as middle-clicking a real candle stack gives. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.accessory);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LilyPadTarget.outline(this, state, pos, context, this.shapes[state.getValue(CANDLES) - 1].outline());
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return LilyPadTarget.destroyProgress(this, state, player, pos, super.getDestroyProgress(state, player, level, pos));
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		// Never pops off - this only ever exists on top of a lily pad, placed by our
		// own interaction code, not vanilla's normal candle-placement rules.
		return true;
	}
}
