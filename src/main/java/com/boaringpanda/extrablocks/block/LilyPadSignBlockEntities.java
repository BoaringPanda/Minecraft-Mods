package com.boaringpanda.extrablocks.block;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.boaringpanda.extrablocks.ExtraBlocks;
import com.boaringpanda.extrablocks.block.custom.LilyPadSignBlockEntity;

/**
 * The one custom {@code BlockEntityType} this mod needs. Vanilla's own {@code BlockEntityType.SIGN}
 * is a frozen {@code Set<Block>} allow-list of the 26 literal vanilla sign blocks (confirmed by
 * disassembly - {@code isValid} is a plain {@code Set.contains}, never {@code instanceof}), so no
 * custom block can ever satisfy it regardless of what it extends - a separate type, with our own 13
 * {@link com.boaringpanda.extrablocks.block.custom.LilyPadSignBlock} instances as its valid blocks,
 * is required.
 * <p>
 * <b>Load-order requirement:</b> this class's static init reads {@link LilyPadAccessories#SIGN_BLOCKS},
 * which is only fully populated once {@code LilyPadAccessories}'s own static block (all 13
 * {@code registerSign} calls) has run. {@code ExtraBlocks.onInitialize()} must call
 * {@code LilyPadAccessories.initialize()} before {@code LilyPadSignBlockEntities.initialize()} -
 * get that order wrong and this silently registers an incomplete (possibly empty) valid-blocks set,
 * with no crash, just broken block-entity loading for whichever signs registered after this class did.
 */
public class LilyPadSignBlockEntities {
	public static final BlockEntityType<LilyPadSignBlockEntity> LILY_PAD_SIGN = register();

	private static BlockEntityType<LilyPadSignBlockEntity> register() {
		Set<Block> validBlocks = new HashSet<>(LilyPadAccessories.SIGN_BLOCKS);
		BlockEntityType<LilyPadSignBlockEntity> type = new BlockEntityType<>(LilyPadSignBlockEntity::new, validBlocks);
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ExtraBlocks.id("lily_pad_sign"), type);
	}

	public static void initialize() {
	}
}
