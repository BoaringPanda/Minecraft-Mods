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
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.extrablocks.block.LilyPadTarget;

/**
 * A lightning rod (any copper stage, waxed or not) standing on a lily pad - a working one. Extends the real
 * {@link LightningRodBlock}, so it keeps the rod's {@code POWERED}/{@code FACING}/{@code WATERLOGGED}
 * properties, {@link #onLightningStrike} (glow, redstone pulse, sparks, unpowered again 8 ticks later), its
 * redstone signals and its thunderstorm sparks. None of those check which block they're on (checked by
 * disassembly), and {@code LightningBolt} powers anything that is a {@code LightningRodBlock}.
 * <p>
 * Two more things make lightning find it, both done in {@link com.boaringpanda.extrablocks.block.LilyPadAccessories}:
 * every state is registered as vanilla's {@code minecraft:lightning_rod} point of interest, which is what
 * {@code ServerLevel.findLightningRod} searches for; and it's in the {@code minecraft:lightning_rods} tag. Without
 * the tag, a strike on it could become a skeleton-horse trap whose bolt is only visual and never powers the rod,
 * and a Channeling trident couldn't call lightning on it.
 * <p>
 * The unwaxed rods also keep aging - see {@link LilyPadWeatheringLightningRodBlock}.
 */
public class LilyPadLightningRodBlock extends LightningRodBlock implements LilyPadCombo {
	private final Block accessory;
	private final LilyPadShapes shapes;

	public LilyPadLightningRodBlock(Properties properties, Block accessory) {
		super(properties);
		this.accessory = accessory;

		BlockState source = accessory.defaultBlockState();
		this.shapes = LilyPadShapes.of(
				source.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
				source.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
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

	/** Powered or not changes the texture - a struck rod glows. */
	@Override
	public BlockState accessoryState(BlockState state) {
		return this.accessory.defaultBlockState().setValue(POWERED, state.getValue(POWERED));
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

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.accessory);
	}
}
