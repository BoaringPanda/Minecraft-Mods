package com.boaringpanda.bpsbettervanillabuilding.block;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.boaringpanda.bpsbettervanillabuilding.BPsBetterVanillaBuilding;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadSignBlockEntity;

/**
 * The one custom {@code BlockEntityType} this mod has. Vanilla's own {@code BlockEntityTypes.SIGN}
 * is a frozen {@code Set<Block>} allow-list of the 26 literal vanilla sign blocks (confirmed by
 * disassembly - {@code isValid} is a plain {@code Set.contains}, never {@code instanceof}), so no
 * custom block satisfies it on its own, whatever it extends. This separate type, with our own 13
 * {@link com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadSignBlock} instances as its valid blocks,
 * is how signs deal with that. (Fabric API's {@code addValidBlock} can instead add a block to a
 * vanilla type; heads and banners do that, see {@link LilyPadAccessories}. Signs predate that
 * finding, and this works just as well.)
 * <p>
 * <b>Load-order requirement:</b> this class's static init reads {@link LilyPadAccessories#SIGN_BLOCKS},
 * which is only fully populated once {@code LilyPadAccessories}'s own static block (all 13
 * {@code registerSign} calls) has run. {@code BPsBetterVanillaBuilding.onInitialize()} must call
 * {@code LilyPadAccessories.initialize()} before {@code LilyPadSignBlockEntities.initialize()} -
 * get that order wrong and this silently registers an incomplete (possibly empty) valid-blocks set,
 * with no crash, just broken block-entity loading for whichever signs registered after this class did.
 */
public class LilyPadSignBlockEntities {
	public static final BlockEntityType<LilyPadSignBlockEntity> LILY_PAD_SIGN = register();

	private static BlockEntityType<LilyPadSignBlockEntity> register() {
		Set<Block> validBlocks = new HashSet<>(LilyPadAccessories.SIGN_BLOCKS);
		BlockEntityType<LilyPadSignBlockEntity> type = new BlockEntityType<>(LilyPadSignBlockEntity::new, validBlocks);
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BPsBetterVanillaBuilding.id("lily_pad_sign"), type);
	}

	public static void initialize() {
	}
}
