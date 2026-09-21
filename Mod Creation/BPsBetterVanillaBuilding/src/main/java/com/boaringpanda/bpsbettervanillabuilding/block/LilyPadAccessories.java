package com.boaringpanda.bpsbettervanillabuilding.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;

import com.boaringpanda.bpsbettervanillabuilding.BPsBetterVanillaBuilding;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadAccessoryBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadBannerBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadCandleBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadLightningRodBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadPottedPlantBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadRedstoneTorchBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadSeaPickleBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadSignBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadSkullBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadWeatheringAccessoryBlock;
import com.boaringpanda.bpsbettervanillabuilding.block.custom.LilyPadWeatheringLightningRodBlock;
import com.boaringpanda.bpsbettervanillabuilding.mixin.PoiTypesInvoker;

/**
 * Registers a combo block for each supported "lily pad + accessory"
 * pairing. See {@link LilyPadAccessoryInteraction} for how a player creates
 * (and, for candles/sea pickles, stacks) one in-world.
 * <p>
 * Most accessories are a {@link LilyPadAccessoryBlock} - purely decorative,
 * a single fixed appearance, no BlockEntity. Candles, sea pickles, and signs are
 * different: they're {@link LilyPadCandleBlock}/{@link LilyPadSeaPickleBlock}/
 * {@link LilyPadSignBlock}, which extend the *real* vanilla
 * {@code CandleBlock}/{@code SeaPickleBlock}/{@code StandingSignBlock} to get
 * real stacking/lighting/extinguishing/text behaviour, not just a picture -
 * see those classes' own docs for what's inherited and what isn't. Heads and
 * banners ({@link LilyPadSkullBlock}/{@link LilyPadBannerBlock}) extend the real
 * {@code SkullBlock}/{@code BannerBlock} too, and go one step further: vanilla's
 * own renderer draws them, from a vanilla block entity.
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

	// The copper lantern in every oxidation stage, plus the waxed version of each. These are
	// WeatheringCopperCollection entries, not Blocks.* fields - resolved by registry lookup, same as
	// the wool/concrete/copper materials in MixedSlabBlocks. Each variant is its own combo so that
	// placing and dropping give back exactly the item that was used. Brightness is 15 at every stage
	// (unlike copper bulbs, a copper lantern's light doesn't change with oxidation).
	private static final List<String> COPPER_LANTERN_IDS = List.of(
			"copper_lantern",
			"exposed_copper_lantern",
			"weathered_copper_lantern",
			"oxidized_copper_lantern",
			"waxed_copper_lantern",
			"waxed_exposed_copper_lantern",
			"waxed_weathered_copper_lantern",
			"waxed_oxidized_copper_lantern"
	);

	static {
		for (String lanternId : COPPER_LANTERN_IDS) {
			register(
					"lily_pad_with_" + lanternId,
					resolve(lanternId),
					BlockBehaviour.Properties.of()
							.sound(SoundType.LILY_PAD)
							.lightLevel(state -> 15)
							.strength(3.5f)
							.requiresCorrectToolForDrops()
			);
		}
	}

	/**
	 * Always lit, and a real power source: it gives the same signal a lit redstone torch on a
	 * solid block does (see {@link LilyPadRedstoneTorchBlock}). A real one turns off when the block
	 * below is powered, but that block is water here, so there is nothing to invert.
	 */
	public static final LilyPadRedstoneTorchBlock LILY_PAD_WITH_REDSTONE_TORCH = registerRedstoneTorch();

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
	 * The four amethyst growth stages you can place: small, medium and large bud, then the cluster. Always the upward
	 * orientation (they're standing on the pad), which is the base model for all four: vanilla's blockstate only turns them
	 * for the other five directions. Each is a plain {@code cross} model with its own texture, so the merged models are the
	 * lily pad plus that cross. Light level is read from the vanilla block itself (1, 2, 4 and 5) rather than typed in.
	 */
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_SMALL_AMETHYST_BUD = registerAmethyst("small_amethyst_bud", Blocks.SMALL_AMETHYST_BUD);
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_MEDIUM_AMETHYST_BUD = registerAmethyst("medium_amethyst_bud", Blocks.MEDIUM_AMETHYST_BUD);
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_LARGE_AMETHYST_BUD = registerAmethyst("large_amethyst_bud", Blocks.LARGE_AMETHYST_BUD);
	public static final LilyPadAccessoryBlock LILY_PAD_WITH_AMETHYST_CLUSTER = registerAmethyst("amethyst_cluster", Blocks.AMETHYST_CLUSTER);

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

	static {
		for (String candleId : CANDLE_IDS) {
			registerCandle(candleId);
		}
	}

	public static final LilyPadSeaPickleBlock LILY_PAD_WITH_SEA_PICKLE = registerSeaPickle();

	/**
	 * Real, writable text - see CLAUDE.md for how: {@link LilyPadSignBlock} extends the real
	 * vanilla {@code StandingSignBlock} directly, with its own {@link LilyPadSignBlockEntities}
	 * type backing it. Rotation is a real 16-value rotation matching wherever the player was
	 * facing when they placed it, same as a sign on any other block; see
	 * {@link LilyPadAccessoryInteraction#combine} for how.
	 * <p>
	 * Hardness 1 matches a real sign's vanilla value, and like a real sign it drops with any
	 * tool - an axe is only faster (checked against the wiki). That needs `mineable/axe` too,
	 * in `data/minecraft/tags/block/mineable/axe.json`. (Unlike the lantern family, which
	 * deliberately still requires a pickaxe - see {@link #LILY_PAD_WITH_LANTERN}.)
	 */
	private static final List<String> SIGN_WOODS = List.of(
			"acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle",
			"mangrove", "oak", "pale_oak", "poplar", "spruce", "warped"
	);

	/**
	 * Every registered {@link LilyPadSignBlock}, for {@link LilyPadSignBlockEntities} to build its
	 * {@code BlockEntityType}'s valid-blocks set from - see that class for why a dedicated type (and
	 * thus this list) is needed at all.
	 */
	static final List<LilyPadSignBlock> SIGN_BLOCKS = new ArrayList<>();

	static {
		for (String wood : SIGN_WOODS) {
			registerSign(wood);
		}
	}

	/**
	 * Every head that stands on a block - the seven in vanilla's {@code minecraft:skulls} tag, player
	 * heads included. Unlike the accessories above, a head isn't merged into the lily pad's model:
	 * the combo's model is just the plain lily pad, and vanilla's own head renderer draws the head on
	 * top, reading the skin, rotation and animation from a vanilla block entity (see
	 * {@link LilyPadSkullBlock} for how that works).
	 * <p>
	 * Hardness 1 is a real head's own, and like a real head it drops with any tool.
	 */
	private static final List<String> SKULL_IDS = List.of(
			"skeleton_skull",
			"wither_skeleton_skull",
			"zombie_head",
			"player_head",
			"creeper_head",
			"dragon_head",
			"piglin_head"
	);

	static {
		for (String skullId : SKULL_IDS) {
			registerSkull(skullId);
		}
	}

	/**
	 * Every banner colour's own block id. {@code Blocks.BANNER} is a {@code ColorCollection}, so these
	 * are resolved by id like the copper lanterns. Built the same way as the heads, with vanilla's own
	 * banner renderer (see {@link LilyPadBannerBlock}).
	 * <p>
	 * Hardness 1 is a real banner's own. Like a real banner it drops with any tool, and an axe is faster
	 * because these are in the {@code minecraft:banners} tag, which vanilla's {@code mineable/axe} includes.
	 */
	private static final List<String> BANNER_IDS = List.of(
			"white_banner",
			"orange_banner",
			"magenta_banner",
			"light_blue_banner",
			"yellow_banner",
			"lime_banner",
			"pink_banner",
			"gray_banner",
			"light_gray_banner",
			"cyan_banner",
			"purple_banner",
			"blue_banner",
			"brown_banner",
			"green_banner",
			"red_banner",
			"black_banner"
	);

	static {
		for (String bannerId : BANNER_IDS) {
			registerBanner(bannerId);
		}
	}

	/**
	 * The four copper stages of the lightning rod. Each is registered with its waxed version, as working
	 * rods (see {@link LilyPadLightningRodBlock}). Hardness 3 and "only drops to a pickaxe" are a real rod's
	 * own (checked in {@code Blocks}' bytecode). The stone-or-better tier comes from the
	 * {@code minecraft:lightning_rods} tag, which vanilla's {@code needs_stone_tool} includes.
	 */
	private static final List<String> LIGHTNING_ROD_STAGES = List.of(
			"lightning_rod",
			"exposed_lightning_rod",
			"weathered_lightning_rod",
			"oxidized_lightning_rod"
	);

	static {
		for (String stage : LIGHTNING_ROD_STAGES) {
			registerLightningRod(stage);
			registerLightningRod("waxed_" + stage);
		}
		registerCopperAging(LIGHTNING_ROD_STAGES);
	}

	/**
	 * The four copper stages of the chain, each registered with its waxed version, plus the regular chain
	 * (26.3 calls it {@code iron_chain}). Decorative: the chain stands upright on the pad, as one placed on
	 * top of a block does. Hardness 5 and "only drops to a pickaxe" are a real chain's own (checked in
	 * {@code Blocks}' bytecode). The pickaxe comes from the {@code minecraft:chains} tag, which vanilla's
	 * {@code mineable/pickaxe} includes.
	 */
	private static final List<String> COPPER_CHAIN_STAGES = List.of(
			"copper_chain",
			"exposed_copper_chain",
			"weathered_copper_chain",
			"oxidized_copper_chain"
	);

	static {
		registerChain("iron_chain");
		for (String stage : COPPER_CHAIN_STAGES) {
			registerChain(stage);
			registerChain("waxed_" + stage);
		}
		registerCopperAging(COPPER_CHAIN_STAGES);
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
		Identifier id = BPsBetterVanillaBuilding.id("lily_pad_with_potted_" + comboSuffix);
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

	private static LilyPadAccessoryBlock registerAmethyst(String amethystId, Block accessory) {
		int light = accessory.defaultBlockState().getLightEmission();
		return register(
				"lily_pad_with_" + amethystId,
				accessory,
				BlockBehaviour.Properties.of()
						.sound(SoundType.LILY_PAD)
						.lightLevel(state -> light)
						.strength(0.0f)
		);
	}

	private static LilyPadAccessoryBlock register(String name, Block accessory, BlockBehaviour.Properties properties) {
		Identifier id = BPsBetterVanillaBuilding.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadAccessoryBlock block = new LilyPadAccessoryBlock(properties.setId(key), accessory);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(accessory, block);

		return block;
	}

	private static LilyPadRedstoneTorchBlock registerRedstoneTorch() {
		Identifier id = BPsBetterVanillaBuilding.id("lily_pad_with_redstone_torch");
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadRedstoneTorchBlock block = new LilyPadRedstoneTorchBlock(
				BlockBehaviour.Properties.of()
						.sound(SoundType.LILY_PAD)
						.lightLevel(state -> 7)
						.strength(0.0f)
						.setId(key),
				Blocks.REDSTONE_TORCH
		);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(Blocks.REDSTONE_TORCH, block);

		return block;
	}

	private static void registerSign(String wood) {
		Block accessory = resolve(wood + "_sign");
		Identifier id = BPsBetterVanillaBuilding.id("lily_pad_with_" + wood + "_sign");
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadSignBlock block = new LilyPadSignBlock(
				BlockBehaviour.Properties.of()
						.sound(SoundType.LILY_PAD)
						.strength(1.0f)
						.setId(key),
				accessory
		);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(accessory, block);
		SIGN_BLOCKS.add(block);
	}

	/**
	 * {@code addValidBlock} (Fabric API) lets the combo use vanilla's own head block entity type, whose
	 * valid-blocks set would otherwise refuse it, and with it vanilla's own head renderer. It's done here,
	 * with the registration, so it can't be missed or run in the wrong order.
	 */
	private static void registerSkull(String skullId) {
		Block accessory = resolve(skullId);
		Identifier id = BPsBetterVanillaBuilding.id("lily_pad_with_" + skullId);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadSkullBlock block = new LilyPadSkullBlock(
				BlockBehaviour.Properties.of()
						.sound(SoundType.LILY_PAD)
						.strength(1.0f)
						.pushReaction(PushReaction.POPPED) // what vanilla uses for both a lily pad and a head
						.setId(key),
				accessory
		);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(accessory, block);
		BlockEntityTypes.SKULL.addValidBlock(block);
	}

	/** Same as {@link #registerSkull}, with vanilla's banner block entity type. */
	private static void registerBanner(String bannerId) {
		Block accessory = resolve(bannerId);
		Identifier id = BPsBetterVanillaBuilding.id("lily_pad_with_" + bannerId);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		LilyPadBannerBlock block = new LilyPadBannerBlock(
				BlockBehaviour.Properties.of()
						.sound(SoundType.LILY_PAD)
						.strength(1.0f)
						.pushReaction(PushReaction.POPPED)
						.setId(key),
				accessory
		);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(accessory, block);
		BlockEntityTypes.BANNER.addValidBlock(block);
	}

	/**
	 * Each state is also registered as vanilla's {@code minecraft:lightning_rod} point of interest - the only way
	 * lightning finds a rod (see {@link LilyPadLightningRodBlock}). The unwaxed stages are the aging subclass,
	 * just as vanilla's own unwaxed rods are its {@code WeatheringCopper} ones.
	 */
	private static void registerLightningRod(String rodId) {
		Block accessory = resolve(rodId);
		Identifier id = BPsBetterVanillaBuilding.id("lily_pad_with_" + rodId);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
				.sound(SoundType.LILY_PAD)
				.strength(3.0f)
				.requiresCorrectToolForDrops()
				.setId(key);
		LilyPadLightningRodBlock block = accessory instanceof WeatheringCopper
				? new LilyPadWeatheringLightningRodBlock(properties, accessory)
				: new LilyPadLightningRodBlock(properties, accessory);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(accessory, block);
		PoiTypesInvoker.invokeRegisterBlockStates(
				BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.LIGHTNING_ROD),
				Set.copyOf(block.getStateDefinition().getPossibleStates())
		);
	}

	/** The unwaxed copper stages are the aging subclass; the regular chain and the waxed ones don't age. */
	private static void registerChain(String chainId) {
		Block accessory = resolve(chainId);
		Identifier id = BPsBetterVanillaBuilding.id("lily_pad_with_" + chainId);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
				.sound(SoundType.LILY_PAD)
				.strength(5.0f)
				.requiresCorrectToolForDrops()
				.setId(key);
		LilyPadAccessoryBlock block = accessory instanceof WeatheringCopper
				? new LilyPadWeatheringAccessoryBlock(properties, accessory)
				: new LilyPadAccessoryBlock(properties, accessory);

		Registry.register(BuiltInRegistries.BLOCK, id, block);
		BY_ACCESSORY.put(accessory, block);
	}

	/**
	 * Hands one copper family's combos to vanilla's own aging through Fabric's {@code OxidizableBlocksRegistry}:
	 * each stage ages into the next, and each can be waxed into its {@code waxed_} combo. Vanilla then does the
	 * rest - random ticks, honeycomb, axe scraping and wax removal, and lightning resetting a struck rod to fresh
	 * copper. {@code stages} are vanilla ids, oldest last; all the combos must be registered already.
	 */
	private static void registerCopperAging(List<String> stages) {
		for (int i = 0; i < stages.size(); i++) {
			Block combo = get(resolve(stages.get(i)));
			if (i + 1 < stages.size()) {
				OxidizableBlocksRegistry.registerNextStage(combo, get(resolve(stages.get(i + 1))));
			}
			OxidizableBlocksRegistry.registerWaxable(combo, get(resolve("waxed_" + stages.get(i))));
		}
	}

	private static void registerCandle(String candleId) {
		Block accessory = resolve(candleId);
		Identifier id = BPsBetterVanillaBuilding.id("lily_pad_with_" + candleId);
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
	}

	private static LilyPadSeaPickleBlock registerSeaPickle() {
		Identifier id = BPsBetterVanillaBuilding.id("lily_pad_with_sea_pickle");
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

	/** The registered potted-plant combo for this exact plant, or null if it's not something a real flower pot accepts. */
	@Nullable
	public static LilyPadPottedPlantBlock pottedFor(Block plant) {
		return POTTED.get(plant);
	}
}
