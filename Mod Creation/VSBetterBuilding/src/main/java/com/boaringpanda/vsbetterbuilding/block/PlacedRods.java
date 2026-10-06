package com.boaringpanda.vsbetterbuilding.block;

import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;

import com.boaringpanda.vsbetterbuilding.VSBetterBuilding;

/**
 * Sticks, blaze rods and breeze rods can be placed like end rods. Each is a {@link PlacedRodBlock} with no item of its own: the vanilla item
 * places it ({@link #place}, called from {@code ItemMixin} on {@code Item.useOn}, which runs after the clicked block's own interaction,
 * just as for a block item).
 * <ul>
 *   <li>A stick snaps soon after it's stepped on ({@link StickBlock}), and burns like grass: fire or lava next to it catches it and
 *       burns it away.</li>
 *   <li>A blaze rod smoulders and burns whoever walks on it like a campfire ({@link BlazeRodBlock}), and gives light level 7, half a
 *       torch's.</li>
 *   <li>A breeze rod slows whoever stands on it ({@link BreezeRodBlock}).</li>
 * </ul>
 * The blocks copy the end rod's properties (instant to break, not a full block). Their textures are drawn in each item's own colours.
 */
public class PlacedRods {
	public static final Block STICK = register("stick", StickBlock::new, rodProperties(SoundType.WOOD).mapColor(MapColor.WOOD));
	public static final Block BLAZE_ROD = register("blaze_rod", BlazeRodBlock::new,
			rodProperties(SoundType.CHAIN).mapColor(MapColor.COLOR_YELLOW).lightLevel(state -> 7));
	public static final Block BREEZE_ROD = register("breeze_rod", BreezeRodBlock::new,
			rodProperties(SoundType.CHAIN).mapColor(MapColor.COLOR_LIGHT_BLUE));

	private static final Map<Item, Block> BY_ITEM = Map.of(Items.STICK, STICK, Items.BLAZE_ROD, BLAZE_ROD, Items.BREEZE_ROD, BREEZE_ROD);

	/**
	 * The vanilla end rod's properties, with the given sounds (a stick sounds like wood, the rods like a chain). {@code forceSolidOff} is
	 * deprecated but vanilla's end rod still uses it: without it a rod standing up (a full block tall) would count as a "solid" block to the
	 * legacy checks that still ask.
	 */
	@SuppressWarnings("deprecation")
	private static BlockBehaviour.Properties rodProperties(SoundType sound) {
		return BlockBehaviour.Properties.of().forceSolidOff().instabreak().sound(sound).noOcclusion();
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, VSBetterBuilding.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
		// The same odds as short grass (vanilla: ignite 60, burn 100).
		FlammableBlockRegistry.getDefaultInstance().add(STICK, 100, 60);
	}

	/** The rod block {@code item} places, or null if it isn't one of the placeable rods. */
	public static @Nullable Block blockFor(Item item) {
		return BY_ITEM.get(item);
	}

	/**
	 * Places {@code block} as vanilla's {@code BlockItem.place} does: the same checks (replaceable spot, can survive, no entity in the way),
	 * the same block update, placed-by call, advancement trigger, sound and game event, and one item used (not in creative).
	 */
	public static InteractionResult place(BlockPlaceContext clicked, Block block) {
		if (!clicked.canPlace()) {
			return InteractionResult.FAIL;
		}

		// Into a lily pad, the rod stands on the pad, as a block item would (LilyPadDecorations).
		BlockPlaceContext onPad = LilyPadDecorations.padContext(clicked);
		BlockPlaceContext context = onPad != null ? onPad : clicked;
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		BlockState state = LilyPadDecorations.placedOnPad(context, block.getStateForPlacement(context));
		if (state == null || !state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, CollisionContext.placementContext(player))
				|| !level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) {
			return InteractionResult.FAIL;
		}

		ItemStack stack = context.getItemInHand();
		BlockState placed = level.getBlockState(pos);
		if (placed.is(block)) {
			block.setPlacedBy(level, pos, placed, player, stack);
			if (player instanceof ServerPlayer serverPlayer) {
				CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
			}
		}

		SoundType sound = placed.getSoundType();
		level.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placed));
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
