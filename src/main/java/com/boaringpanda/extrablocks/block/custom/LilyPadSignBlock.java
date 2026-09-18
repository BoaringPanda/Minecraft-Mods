package com.boaringpanda.extrablocks.block.custom;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.extrablocks.block.LilyPadSignBlockEntities;

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

	public LilyPadSignBlock(Properties properties, Block accessory) {
		super(SignBlock.getWoodType(accessory), properties);
		this.accessory = accessory;
	}

	public Block accessory() {
		return this.accessory;
	}

	@Override
	public List<ItemStack> accessoryDrops(BlockState state) {
		return List.of(new ItemStack(this.accessory));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LilyPadShape.SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LilyPadShape.SHAPE;
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

	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);

		boolean shouldDrop = !player.isCreative() && (!state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state));
		if (shouldDrop) {
			popResource(level, pos, new ItemStack(Blocks.LILY_PAD));
			for (ItemStack drop : accessoryDrops(state)) {
				popResource(level, pos, drop);
			}
		}
	}
}
