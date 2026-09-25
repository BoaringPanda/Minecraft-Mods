package com.boaringpanda.bettervanillabuilding.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;

/**
 * Remembers which two slabs a {@link MixedSlabBlock} is made of. It syncs them to the client, whose model draws them
 * ({@link #getRenderData}).
 */
public class MixedSlabBlockEntity extends BlockEntity implements RenderDataBlockEntity {
	private MixedSlabs.Halves halves = MixedSlabs.Halves.FALLBACK;

	public MixedSlabBlockEntity(BlockPos pos, BlockState state) {
		super(MixedSlabs.MIXED_SLAB_ENTITY, pos, state);
	}

	public MixedSlabs.Halves halves() {
		return halves;
	}

	public void setHalves(MixedSlabs.Halves halves) {
		this.halves = halves;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("slabs", MixedSlabs.Halves.CODEC, halves);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		halves = input.read("slabs", MixedSlabs.Halves.CODEC).orElse(MixedSlabs.Halves.FALLBACK);
		if (level != null && level.isClientSide()) {
			// New data from the server: redraw the block, whose model may have been built before the data arrived.
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Object getRenderData() {
		return halves;
	}
}
