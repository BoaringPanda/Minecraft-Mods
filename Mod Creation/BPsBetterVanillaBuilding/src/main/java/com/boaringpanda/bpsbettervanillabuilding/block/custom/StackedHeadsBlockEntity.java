package com.boaringpanda.bpsbettervanillabuilding.block.custom;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bpsbettervanillabuilding.block.StackedHeads;

/**
 * The data behind a {@link StackedHeadsBlock}: up to two heads, one in the bottom half of the block and one in the top.
 * Either half can be empty. Breaking one head leaves the other exactly where it is, so a pair block stays a pair block
 * (holding a single head) until its last head goes, and the head that is left is never touched, moved or turned.
 * <p>
 * Each head is a real vanilla {@link SkullBlockEntity} that is never placed in the world. It is only a holder: the
 * head's block state (which head, and its rotation), its skin, custom name and note block sound, all of which
 * vanilla already knows how to save, sync, draw and turn into a drop. Every one of those is reused rather than
 * copied. Each holder is given this block entity's position and level so vanilla's renderer can light it.
 * <p>
 * A holder's block state isn't part of what the skull saves, so it is saved here alongside it: the head's block ID
 * and its rotation. A missing head is simply not saved.
 */
public class StackedHeadsBlockEntity extends BlockEntity {
	@Nullable
	private SkullBlockEntity bottom;
	@Nullable
	private SkullBlockEntity top;

	public StackedHeadsBlockEntity(BlockPos pos, BlockState state) {
		super(StackedHeads.BLOCK_ENTITY, pos, state);
	}

	private SkullBlockEntity newHead(BlockState headState) {
		SkullBlockEntity head = new SkullBlockEntity(this.getBlockPos(), headState);
		if (this.level != null) {
			head.setLevel(this.level);
		}

		return head;
	}

	/** The head in the top half when {@code top} is true, otherwise the one in the bottom half; null if that half is empty. */
	@Nullable
	public SkullBlockEntity head(boolean top) {
		return top ? this.top : this.bottom;
	}

	public boolean has(boolean top) {
		return this.head(top) != null;
	}

	/** How many heads are in the block: 0, 1 or 2. */
	public int count() {
		return (this.bottom != null ? 1 : 0) + (this.top != null ? 1 : 0);
	}

	/**
	 * Puts a fresh head in this state in that half (replacing whatever was there). The caller then puts the head's data on
	 * the one returned ({@code applyComponents}), the same way vanilla puts an item's data on a block entity it has just
	 * placed.
	 */
	public SkullBlockEntity replaceHead(boolean top, BlockState headState) {
		SkullBlockEntity fresh = this.newHead(headState);
		if (top) {
			this.top = fresh;
		} else {
			this.bottom = fresh;
		}

		return fresh;
	}

	/** Empties that half. Nothing else in the block is touched. */
	public void removeHead(boolean top) {
		if (top) {
			this.top = null;
		} else {
			this.bottom = null;
		}
	}

	/**
	 * The head's own outline or collision box, moved up half a block for the top head, or empty if that half is empty.
	 * Taken from the real vanilla head block, so a piglin's wider box and the dragon's outline come out right.
	 */
	public VoxelShape shape(boolean top, boolean collision) {
		SkullBlockEntity head = this.head(top);
		if (head == null) {
			return Shapes.empty();
		}

		BlockState headState = head.getBlockState();
		VoxelShape shape = collision
				? headState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
				: headState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		return top ? shape.move(0.0, 0.5, 0.0) : shape;
	}

	/**
	 * What breaking one head drops: the real head's loot table, run against that head's data. Its
	 * {@code copy_components} entries decide what carries over, so a player head keeps its skin.
	 */
	public List<ItemStack> drops(boolean top) {
		SkullBlockEntity head = this.head(top);
		if (head == null) {
			return List.of();
		}

		return LilyPadVanillaDrops.accessory(head.getBlockState().getBlock(), head);
	}

	/** Client only, once per tick: vanilla's own dragon/piglin animation for each head, driven by the block's power. */
	public static void clientTick(Level level, BlockPos pos, BlockState state, StackedHeadsBlockEntity blockEntity) {
		boolean powered = state.getValue(AbstractSkullBlock.POWERED);
		animate(level, pos, blockEntity.bottom, powered);
		animate(level, pos, blockEntity.top, powered);
	}

	private static void animate(Level level, BlockPos pos, @Nullable SkullBlockEntity head, boolean powered) {
		if (head != null) {
			SkullBlockEntity.animation(level, pos, head.getBlockState().setValue(AbstractSkullBlock.POWERED, powered), head);
		}
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		if (this.bottom != null) {
			this.bottom.setLevel(level);
		}
		if (this.top != null) {
			this.top.setLevel(level);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (this.bottom != null) {
			saveHead(output.child("bottom"), this.bottom);
		}
		if (this.top != null) {
			saveHead(output.child("top"), this.top);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.bottom = input.child("bottom").map(this::loadHead).orElse(null);
		this.top = input.child("top").map(this::loadHead).orElse(null);
	}

	private static void saveHead(ValueOutput output, SkullBlockEntity head) {
		BlockState state = head.getBlockState();
		output.putString("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
		output.putInt("rotation", state.getValue(SkullBlock.ROTATION));
		head.saveWithoutMetadata(output);
	}

	private SkullBlockEntity loadHead(ValueInput input) {
		Identifier id = Identifier.tryParse(input.getStringOr("block", "minecraft:skeleton_skull"));
		Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
		if (!(block instanceof SkullBlock)) {
			// A missing or renamed head shouldn't stop the block loading; fall back rather than lose the other one.
			block = Blocks.SKELETON_SKULL;
		}

		BlockState state = block.defaultBlockState().setValue(SkullBlock.ROTATION, input.getIntOr("rotation", 0));
		SkullBlockEntity head = this.newHead(state);
		head.loadWithComponents(input);
		return head;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveCustomOnly(registries);
	}
}
