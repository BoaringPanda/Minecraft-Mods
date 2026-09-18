package com.boaringpanda.extrablocks.block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.boaringpanda.extrablocks.ExtraBlocks;
import com.boaringpanda.extrablocks.block.custom.LilyPadAccessoryBlock;
import com.boaringpanda.extrablocks.block.custom.LilyPadCandleBlock;
import com.boaringpanda.extrablocks.block.custom.LilyPadSeaPickleBlock;

/**
 * Registers a combo block for each supported "lily pad + accessory"
 * pairing. See {@link LilyPadAccessoryInteraction} for how a player creates
 * (and, for candles/sea pickles, stacks) one in-world.
 * <p>
 * Most accessories are a {@link LilyPadAccessoryBlock} - purely decorative,
 * a single fixed appearance, no BlockEntity. Candles and sea pickles are
 * different: they're {@link LilyPadCandleBlock}/{@link LilyPadSeaPickleBlock},
 * which extend the *real* vanilla {@code CandleBlock}/{@code SeaPickleBlock}
 * to get real stacking/lighting/extinguishing behaviour, not just a picture -
 * see those classes' own docs for what's inherited and what isn't.
 * <p>
 * To support another simple (decorative-only) accessory, add a
 * {@code register(...)} call below and a matching merged model + blockstate
 * (see CLAUDE.md for the exact pattern and the pixel/rotation lessons learned
 * building the existing ones).
 */
public class LilyPadAccessories {
	// Declared first deliberately: register() below needs this to already exist when
	// the LILY_PAD_WITH_* fields' initializers run, and static fields initialize in
	// textual declaration order.
	private static final Map<Block, Block> BY_ACCESSORY = new HashMap<>();

	/** Matches torch's own vanilla properties: instant break, no tool needed, light 14. */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_TORCH = register(
			"lily_pad_with_torch",
			Blocks.TORCH,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 14)
					.strength(0.0f)
	);

	/**
	 * Deliberately not matching lantern's own vanilla properties (needs a
	 * pickaxe, strength 3.5) - anything standing on a lily pad should break
	 * instantly with no tool required, same as {@link #LILY_PAD_WITH_TORCH}.
	 */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_LANTERN = register(
			"lily_pad_with_lantern",
			Blocks.LANTERN,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 15)
					.strength(0.0f)
	);

	public static final LilyPadAccessoryBlock LILY_PAD_WITH_SOUL_TORCH = register(
			"lily_pad_with_soul_torch",
			Blocks.SOUL_TORCH,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 10)
					.strength(0.0f)
	);

	public static final LilyPadAccessoryBlock LILY_PAD_WITH_COPPER_TORCH = register(
			"lily_pad_with_copper_torch",
			Blocks.COPPER_TORCH,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 14)
					.strength(0.0f)
	);

	public static final LilyPadAccessoryBlock LILY_PAD_WITH_SOUL_LANTERN = register(
			"lily_pad_with_soul_lantern",
			Blocks.SOUL_LANTERN,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 10)
					.strength(0.0f)
	);

	// COPPER_LANTERN is a WeatheringCopperCollection<Block>, not a Blocks.* field - resolved
	// by registry lookup below, same as the wool/concrete/copper materials in MixedSlabBlocks.
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_COPPER_LANTERN = register(
			"lily_pad_with_copper_lantern",
			resolve("copper_lantern"),
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 15)
					.strength(0.0f)
	);

	/**
	 * Always the "lit" appearance - a real redstone torch inverts based on
	 * whether the block below is powered, but this one is purely decorative
	 * (see CLAUDE.md for why: replicating that as a real circuit component
	 * was explicitly out of scope).
	 */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_REDSTONE_TORCH = register(
			"lily_pad_with_redstone_torch",
			Blocks.REDSTONE_TORCH,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 7)
					.strength(0.0f)
	);

	/** Always the "facing=up" orientation - the model that's already just the base end rod model. */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_END_ROD = register(
			"lily_pad_with_end_rod",
			Blocks.END_ROD,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 14)
					.strength(0.0f)
	);

	/**
	 * Every candle color's own block id - these ARE the full ids already
	 * ("candle" for plain, "white_candle" etc. for the 16 dye colors), not a
	 * material prefix needing a suffix appended.
	 */
	private static final List<String> CANDLE_IDS = List.of(
			"candle",
			"white_candle",
			"orange_candle",
			"magenta_candle",
			"light_blue_candle",
			"yellow_candle",
			"lime_candle",
			"pink_candle",
			"gray_candle",
			"light_gray_candle",
			"cyan_candle",
			"purple_candle",
			"blue_candle",
			"brown_candle",
			"green_candle",
			"red_candle",
			"black_candle"
	);

	private static final Map<Block, LilyPadCandleBlock> CANDLES = new HashMap<>();

	static {
		for (String candleId : CANDLE_IDS) {
			registerCandle(candleId);
		}
	}

	public static final LilyPadSeaPickleBlock LILY_PAD_WITH_SEA_PICKLE = registerSeaPickle();

	/**
	 * Blank, non-writable - see CLAUDE.md for why: a sign's text is real
	 * BlockEntity data, out of scope for the "simple fixed version" this is.
	 */
	private static final List<String> SIGN_WOODS = List.of(
			"acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle",
			"mangrove", "oak", "pale_oak", "poplar", "spruce", "warped"
	);

	static {
		for (String wood : SIGN_WOODS) {
			registerSimple("lily_pad_with_" + wood + "_sign", resolve(wood + "_sign"));
		}
	}

	/** Always empty - can't be planted into afterward, see CLAUDE.md. */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_FLOWER_POT = register(
			"lily_pad_with_flower_pot",
			Blocks.FLOWER_POT,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.strength(0.0f)
	);

	public static void initialize() {
	}

	private static void registerSimple(String name, Block accessory) {
		register(name, accessory, BlockBehaviour.Properties.of().sound(SoundType.LILY_PAD).strength(0.0f));
	}

	private static Block resolve(String materialPath) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", materialPath));
	}

	private static LilyPadAccessoryBlock register(String name, Block accessory, BlockBehaviour.Properties properties) {
		Identifier id = ExtraBlocks.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadAccessoryBlock block = new LilyPadAccessoryBlock(properties.setId(key), accessory);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(accessory, block);

		return block;
	}

	private static void registerCandle(String candleId) {
		Block accessory = resolve(candleId);
		Identifier id = ExtraBlocks.id("lily_pad_with_" + candleId);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadCandleBlock block = new LilyPadCandleBlock(
				BlockBehaviour.Properties.of()
						.sound(SoundType.LILY_PAD)
						.lightLevel(CandleBlock.LIGHT_EMISSION)
						.strength(0.0f)
						.setId(key),
				accessory
		);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(accessory, block);
		CANDLES.put(accessory, block);
	}

	private static LilyPadSeaPickleBlock registerSeaPickle() {
		Identifier id = ExtraBlocks.id("lily_pad_with_sea_pickle");
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadSeaPickleBlock block = new LilyPadSeaPickleBlock(
				BlockBehaviour.Properties.of()
						.sound(SoundType.LILY_PAD)
						.strength(0.0f)
						.setId(key)
		);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(Blocks.SEA_PICKLE, block);

		return block;
	}

	/** The registered combo block for this accessory, or null if unsupported. */
	@Nullable
	public static Block get(Block accessory) {
		return BY_ACCESSORY.get(accessory);
	}

	/** The registered candle combo for this exact candle color, or null if {@code candle} isn't a candle. */
	@Nullable
	public static LilyPadCandleBlock candleFor(Block candle) {
		return CANDLES.get(candle);
	}
}
