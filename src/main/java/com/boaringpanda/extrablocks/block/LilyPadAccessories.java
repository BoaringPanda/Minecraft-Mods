package com.boaringpanda.extrablocks.block;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.boaringpanda.extrablocks.ExtraBlocks;
import com.boaringpanda.extrablocks.block.custom.LilyPadAccessoryBlock;

/**
 * Registers a {@link LilyPadAccessoryBlock} for each supported "lily pad +
 * accessory" pairing. See {@link LilyPadAccessoryInteraction} for how a
 * player creates one in-world.
 * <p>
 * To support another accessory, add a {@code register(...)} call below and
 * a matching {@code assets/extra_blocks/blockstates/lily_pad_with_<name>.json}
 * (a two-entry {@code multipart} layering the vanilla lily pad model with
 * the accessory's own model - see the existing files for the pattern).
 */
public class LilyPadAccessories {
	/** Matches torch's own vanilla properties: instant break, no tool needed, light 14. */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_TORCH = register(
			"lily_pad_with_torch",
			Blocks.TORCH,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 14)
					.strength(0.0f)
	);

	/** Matches lantern's own vanilla properties: needs a pickaxe, light 15. */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_LANTERN = register(
			"lily_pad_with_lantern",
			Blocks.LANTERN,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 15)
					.strength(3.5f)
					.requiresCorrectToolForDrops()
	);

	private static final Map<Block, LilyPadAccessoryBlock> BY_ACCESSORY = new HashMap<>();

	public static void initialize() {
	}

	private static LilyPadAccessoryBlock register(String name, Block accessory, BlockBehaviour.Properties properties) {
		Identifier id = ExtraBlocks.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadAccessoryBlock block = new LilyPadAccessoryBlock(properties.setId(key), accessory);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(accessory, block);

		return block;
	}

	/** The registered combo block for this accessory, or null if unsupported. */
	@Nullable
	public static LilyPadAccessoryBlock get(Block accessory) {
		return BY_ACCESSORY.get(accessory);
	}
}
