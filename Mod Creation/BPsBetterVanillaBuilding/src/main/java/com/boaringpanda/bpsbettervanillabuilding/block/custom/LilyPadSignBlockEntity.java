package com.boaringpanda.bpsbettervanillabuilding.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bpsbettervanillabuilding.block.LilyPadSignBlockEntities;

/**
 * Just {@code SignBlockEntity} pointed at our own {@code BlockEntityType} instead of vanilla's -
 * text storage/persistence, the edit-lock, and click commands are all inherited unchanged. A
 * separate type is required (not optional): vanilla's own {@code BlockEntityType.SIGN} is a frozen
 * {@code Set<Block>} allow-list of the 26 literal vanilla sign blocks (confirmed by disassembly -
 * {@code isValid} is a plain {@code Set.contains}, not an {@code instanceof} check), so it can never
 * accept {@link LilyPadSignBlock}. Mirrors how vanilla's own {@code HangingSignBlockEntity} forwards
 * to {@code SignBlockEntity}'s typed constructor with its own type instead of {@code SIGN}.
 */
public class LilyPadSignBlockEntity extends SignBlockEntity {
	public LilyPadSignBlockEntity(BlockPos pos, BlockState state) {
		super(LilyPadSignBlockEntities.LILY_PAD_SIGN, pos, state);
	}
}
