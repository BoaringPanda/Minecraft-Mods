package com.boaringpanda.extrablocks.block.custom;

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
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.extrablocks.block.LilyPadSignBlockEntities;
import com.boaringpanda.extrablocks.block.LilyPadTarget;

/**
 * A real, writable sign (any of the 13 wood types) standing on a lily pad. Extends the real vanilla
 * {@code StandingSignBlock} directly - same "subclass the real vanilla block" pattern as
 * {@link LilyPadCandleBlock}/{@link LilyPadSeaPickleBlock} - to inherit the real text-edit
 * interaction, dye/glow-ink/wax handling ({@code useWithoutItem}/{@code useItemOn}), and the
 * {@code ROTATION}/{@code WATERLOGGED} properties, instead of reimplementing any of it.
 * <p>
 * Rotation is set explicitly by {@link com.boaringpanda.extrablocks.block.LilyPadAccessoryInteraction#combine}
 * using the exact vanilla formula ({@code RotationSegment.convertToSegment(player.getYRot() + 180)},
 * checked by disassembling {@code StandingSignBlock.getStateForPlacement}), since this block is
 * swapped in directly rather than placed through the normal {@code BlockPlaceContext} pipeline that
 * would compute it automatically. That same method also calls {@code setPlacedBy} directly afterward
 * to open the text editor for the placer, reusing vanilla's own {@code SignBlock.setPlacedBy} rather
 * than reimplementing its not-waxed/editable-text checks.
 * <p>
 * Three things the inherited {@code StandingSignBlock} behavior would get wrong for a lily pad and
 * must be overridden:
 * <ul>
 *   <li>{@link #canSurvive} - the inherited version requires a solid block below, but a lily pad
 *       floats on water (not solid); every other accessory combo already "always survives" (the
 *       default, unmodified {@code Block} behavior), so this matches that instead of the real
 *       sign's rule.</li>
 *   <li>{@link #newBlockEntity} - the inherited version constructs a plain vanilla
 *       {@code SignBlockEntity} typed to vanilla's own frozen {@code BlockEntityType.SIGN}, which
 *       this block can never satisfy (see {@link LilyPadSignBlockEntities}) - must return our own
 *       {@link LilyPadSignBlockEntity} instead.</li>
 *   <li>{@link #getTicker} - the inherited version checks reference-equality against vanilla's own
 *       {@code BlockEntityType.SIGN} and would otherwise always return {@code null} for our type,
 *       silently skipping {@code SignBlockEntity.tick}'s stale-edit-lock cleanup forever.</li>
 * </ul>
 */
public class LilyPadSignBlock extends StandingSignBlock implements LilyPadCombo {
	private final Block accessory;

	/** The sign is aimable (so it can be clicked and mined on its own) but, like any sign, not solid. */
	private final LilyPadShapes shapes;

	public LilyPadSignBlock(Properties properties, Block accessory) {
		super(SignBlock.getWoodType(accessory), properties);
		this.accessory = accessory;
		this.shapes = LilyPadShapes.of(
				super.getShape(this.defaultBlockState(), EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()),
				Shapes.empty()
		);
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
		return this.accessory.defaultBlockState();
	}

	@Override
	public void spawnDestroyByEntityParticles(Level level, @Nullable Entity entity, BlockPos pos, BlockState state) {
		LilyPadTarget.spawnDestroyParticles(this, level, entity, pos, state);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.accessory);
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
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return true;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LilyPadSignBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, LilyPadSignBlockEntities.LILY_PAD_SIGN, SignBlockEntity::tick);
	}
}
