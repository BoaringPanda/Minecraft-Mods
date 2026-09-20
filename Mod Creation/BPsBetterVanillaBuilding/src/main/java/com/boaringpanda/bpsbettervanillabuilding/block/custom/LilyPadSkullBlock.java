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
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadTarget;

/**
 * A head (any of the seven that stand on a block, player heads included) sitting on a lily pad.
 * <p>
 * Unlike the other accessories, the head is not part of this block's model. Its blockstate points
 * at the plain vanilla lily pad model and nothing else, so the pad keeps its own random rotation.
 * The head is drawn by vanilla's own {@code SkullBlockRenderer}, the same as any placed head. That
 * works because the renderer only needs the block to be an {@link AbstractSkullBlock} with the
 * {@link SkullBlock#ROTATION} property (checked by disassembly), which subclassing the real
 * {@link SkullBlock} provides. The block entity is a plain vanilla {@link SkullBlockEntity} of
 * vanilla's own type, which accepts this block because
 * {@link com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessories} adds it with Fabric API's
 * {@code addValidBlock}. So the skin, custom name, note block sound and the dragon/piglin animation
 * are all vanilla's, not copies.
 * <p>
 * What subclassing gets wrong for a lily pad, and is overridden here:
 * <ul>
 *   <li>{@link #getTicker} - vanilla only animates a head when the state is literally
 *       {@code Blocks.DRAGON_HEAD}/{@code PIGLIN_HEAD}, so without this a powered dragon head
 *       on a lily pad would never open its mouth.</li>
 *   <li>{@link #getCloneItemStack} - the default would be this block's own item, which doesn't
 *       exist.</li>
 *   <li>Drops - see {@link LilyPadVanillaDrops}.</li>
 * </ul>
 * Rotation and powered state are set when it is placed, see
 * {@link com.boaringpanda.bpsbettervanillabuilding.block.LilyPadAccessoryInteraction#combine}.
 */
public class LilyPadSkullBlock extends SkullBlock implements LilyPadCombo {
	private final Block accessory;
	private final LilyPadShapes shapes;

	public LilyPadSkullBlock(Properties properties, Block accessory) {
		super(((AbstractSkullBlock) accessory).getType(), properties);
		this.accessory = accessory;

		// Taken from the real head, not super.getShape like the sign does: SkullBlock.getShape calls
		// getCollisionShape, which this class overrides to read this.shapes - still null at this point.
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

	/** Its particles are soul sand, like a real head's - that's the texture vanilla's {@code skull.json} names. */
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

	/** The plain head, like vanilla. Ctrl+pick in creative adds the block entity's data (a player's skin) server-side. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(this.accessory);
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		boolean animated = this.getType() == SkullBlock.Types.DRAGON || this.getType() == SkullBlock.Types.PIGLIN;
		if (level.isClientSide() && animated) {
			return createTickerHelper(type, BlockEntityTypes.SKULL, SkullBlockEntity::animation);
		}

		return null;
	}
}
