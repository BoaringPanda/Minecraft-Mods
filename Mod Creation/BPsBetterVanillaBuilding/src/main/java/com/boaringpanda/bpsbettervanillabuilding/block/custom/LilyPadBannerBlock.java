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
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadTarget;

/**
 * A standing banner (any of the 16 colours) on a lily pad. Built exactly like {@link LilyPadSkullBlock}:
 * the block model is only the plain lily pad, and vanilla's own {@code BannerRenderer} draws the pole
 * and flag. The renderer only needs the block to be a {@link BannerBlock} with its {@code ROTATION}
 * property, and it takes the base colour from the block entity (checked by disassembly). The block
 * entity is a plain vanilla {@link BannerBlockEntity}, so patterns, custom names, map markers and the
 * waving flag all work as they do for a normal banner.
 * <p>
 * What subclassing gets wrong for a lily pad, and is overridden here:
 * <ul>
 *   <li>{@link #canSurvive} - a real banner needs a solid block below, and its {@code updateShape}
 *       would turn this into air on the next update from underneath (the same trap as
 *       {@link LilyPadSignBlock}).</li>
 *   <li>{@link #getCloneItemStack} - vanilla builds the item from the block, which for this combo
 *       has no item, so pick-block would give nothing.</li>
 *   <li>Drops - see {@link LilyPadVanillaDrops}.</li>
 * </ul>
 * Rotation is set when it is placed, see
 * {@link com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessoryInteraction#combine}.
 */
public class LilyPadBannerBlock extends BannerBlock implements LilyPadCombo {
	private final Block accessory;
	private final LilyPadShapes shapes;

	public LilyPadBannerBlock(Properties properties, Block accessory) {
		super(((AbstractBannerBlock) accessory).getColor(), properties);
		this.accessory = accessory;

		// A banner has no collision in vanilla, so this combo is solid only as far as the pad.
		BlockState source = accessory.defaultBlockState();
		this.shapes = LilyPadShapes.of(
				source.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
				source.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
		);
	}

	@Override
	public List<ItemStack> accessoryDrops(BlockState state, @Nullable BlockEntity blockEntity) {
		return LilyPadVanillaDrops.accessory(this.accessory, blockEntity);
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
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		return LilyPadVanillaDrops.padAndAccessory(this.accessory, params);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return true;
	}

	/** The real banner item with this banner's patterns and name, as vanilla's pick-block gives for a normal banner. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack stack = new ItemStack(this.accessory);
		if (level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
			stack.applyComponents(banner.collectComponents());
		}

		return stack;
	}
}
