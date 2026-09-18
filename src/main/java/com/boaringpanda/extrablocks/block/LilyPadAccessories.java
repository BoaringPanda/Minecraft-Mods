package com.boaringpanda.extrablocks.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.boaringpanda.extrablocks.block.custom.LilyPadPottedPlantBlock;
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
	 * Hardness 3.5 matches lantern's real vanilla value, but
	 * `requiresCorrectToolForDrops()` is a deliberate choice, not vanilla
	 * fidelity - checked against the wiki, and a real lantern actually drops
	 * with *any* tool, a pickaxe is only faster. Gating it behind a pickaxe
	 * anyway (and soul/copper lantern the same way, for consistency within
	 * the category even though real soul/copper lanterns don't gate either)
	 * matches what was actually asked for: removing something from a lily
	 * pad should need "the right tool" the way a player expects, not
	 * necessarily the exact vanilla rule for that one block. Needs
	 * `mineable/pickaxe` too, in `data/minecraft/tags/block/mineable/pickaxe.json`.
	 */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_LANTERN = register(
			"lily_pad_with_lantern",
			Blocks.LANTERN,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 15)
					.strength(3.5f)
					.requiresCorrectToolForDrops()
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
					.strength(3.5f)
					.requiresCorrectToolForDrops()
	);

	// COPPER_LANTERN is a WeatheringCopperCollection<Block>, not a Blocks.* field - resolved
	// by registry lookup below, same as the wool/concrete/copper materials in MixedSlabBlocks.
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_COPPER_LANTERN = register(
			"lily_pad_with_copper_lantern",
			resolve("copper_lantern"),
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.lightLevel(state -> 15)
					.strength(3.5f)
					.requiresCorrectToolForDrops()
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
	 * <p>
	 * Hardness 1 matches a real sign's vanilla value, but
	 * `requiresCorrectToolForDrops()` is a deliberate choice, not vanilla
	 * fidelity - a real sign actually drops with any tool, an axe is only
	 * faster (checked against the wiki). See {@link #LILY_PAD_WITH_LANTERN}
	 * for the same reasoning. Needs `mineable/axe` too, in
	 * `data/minecraft/tags/block/mineable/axe.json`.
	 */
	private static final List<String> SIGN_WOODS = List.of(
			"acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle",
			"mangrove", "oak", "pale_oak", "poplar", "spruce", "warped"
	);

	static {
		for (String wood : SIGN_WOODS) {
			register(
					"lily_pad_with_" + wood + "_sign",
					resolve(wood + "_sign"),
					BlockBehaviour.Properties.of()
							.sound(SoundType.LILY_PAD)
							.strength(1.0f)
							.requiresCorrectToolForDrops()
			);
		}
	}

	/**
	 * Starts empty - see {@link LilyPadAccessoryInteraction} for how it gets planted
	 * into (only with whatever items go into a real flower pot normally, nothing else).
	 */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_FLOWER_POT = register(
			"lily_pad_with_flower_pot",
			Blocks.FLOWER_POT,
			BlockBehaviour.Properties.of()
					.sound(SoundType.LILY_PAD)
					.strength(0.0f)
	);

	/**
	 * plant Block -> the potted combo for it. Built from the *real* vanilla
	 * plant/pot pairing, not a guess: {@code combo suffix} is usually the
	 * plant's own id, but not always - e.g. the plant you actually pot to get
	 * "potted_azalea_bush" is {@code azalea}, not {@code azalea_bush} (that
	 * name only exists for the potted model/texture) - checked against the
	 * game's own files rather than assumed, since one of these being wrong
	 * would otherwise be a hard-to-notice mismatch (right item, wrong combo,
	 * or vice versa).
	 */
	private static final Map<String, String> POTTED_PLANTS = new LinkedHashMap<>();

	static {
		for (String plain : new String[] {
				"acacia_sapling", "allium", "azure_bluet", "birch_sapling", "blue_orchid",
				"brown_mushroom", "cherry_sapling", "closed_eyeblossom", "cornflower",
				"crimson_fungus", "crimson_roots", "dandelion", "dark_oak_sapling", "dead_bush",
				"golden_dandelion", "jungle_sapling", "lily_of_the_valley", "oak_sapling",
				"orange_tulip", "oxeye_daisy", "pale_oak_sapling", "pink_tulip", "poplar_sapling",
				"poppy", "red_mushroom", "red_tulip", "spruce_sapling", "torchflower",
				"warped_fungus", "warped_roots", "white_tulip", "wither_rose",
				"fern", "open_eyeblossom", "bamboo", "cactus", "mangrove_propagule"
		}) {
			POTTED_PLANTS.put(plain, plain);
		}
		POTTED_PLANTS.put("azalea", "azalea_bush");
		POTTED_PLANTS.put("flowering_azalea", "flowering_azalea_bush");
	}

	private static final Map<Block, LilyPadPottedPlantBlock> POTTED = new HashMap<>();

	static {
		for (Map.Entry<String, String> entry : POTTED_PLANTS.entrySet()) {
			registerPotted(entry.getKey(), entry.getValue());
		}
	}

	public static void initialize() {
	}

	private static void registerPotted(String plantId, String comboSuffix) {
		Block plant = resolve(plantId);
		Identifier id = ExtraBlocks.id("lily_pad_with_potted_" + comboSuffix);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadPottedPlantBlock block = new LilyPadPottedPlantBlock(
				BlockBehaviour.Properties.of()
						.sound(SoundType.LILY_PAD)
						.strength(0.0f)
						.setId(key),
				plant
		);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		// Deliberately not added to BY_ACCESSORY: potted plants are only reachable
		// by planting into an already-placed lily_pad_with_flower_pot, never by
		// combining a plant item directly onto a bare lily pad.
		POTTED.put(plant, block);
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
						.strength(0.1f) // matches a real candle's hardness; no tool requirement, same as vanilla
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

	/**
	 * Every registered combo block, regardless of accessory - every one of
	 * them reuses the lily pad's own model (and its {@code tintindex}), so
	 * this is what the client-side tint registration iterates instead of
	 * listing them out by hand and risking missing one, which is exactly
	 * what happened the first few times more accessories were added here
	 * without updating that list to match.
	 */
	public static Collection<Block> all() {
		// POTTED is separate from BY_ACCESSORY (see registerPotted - potted plants
		// are deliberately unreachable by combining directly onto a bare lily pad),
		// but every one of these blocks reuses the lily pad's own model regardless,
		// so both need the tint registration.
		List<Block> combined = new ArrayList<>(BY_ACCESSORY.values());
		combined.addAll(POTTED.values());
		return combined;
	}

	/** The registered candle combo for this exact candle color, or null if {@code candle} isn't a candle. */
	@Nullable
	public static LilyPadCandleBlock candleFor(Block candle) {
		return CANDLES.get(candle);
	}

	/** The registered potted-plant combo for this exact plant, or null if it's not something a real flower pot accepts. */
	@Nullable
	public static LilyPadPottedPlantBlock pottedFor(Block plant) {
		return POTTED.get(plant);
	}
}
