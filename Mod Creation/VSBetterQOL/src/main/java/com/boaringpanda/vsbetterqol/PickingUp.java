package com.boaringpanda.vsbetterqol;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import com.boaringpanda.vsbetterqol.mixin.ServerGamePacketListenerImplInvoker;

// Right-clicking a lantern with an empty hand puts it straight into the inventory. Sneak + right-click with an empty hand picks up a
// container (chests, barrels, brewing stands, enchanting tables, beehives...) with everything inside and carries it (see Carrying) until
// it's set down with another right-click. Shulker boxes are the exception: they go into the inventory, like breaking one does.
// With something in hand, right-clicks stay vanilla (placing a block against a lantern or a chest still works).
public final class PickingUp {
	private static final TagKey<Block> PICKED_UP_BY_HAND = TagKey.create(Registries.BLOCK, VSBetterQOL.id("picked_up_by_hand"));
	private static final TagKey<Block> PICKED_UP_BY_SNEAKING = TagKey.create(Registries.BLOCK, VSBetterQOL.id("picked_up_by_sneaking"));

	private PickingUp() {
	}

	// Fired on both sides before vanilla's right-click on a block. The client just swings the arm and tells the server (SUCCESS), so it
	// doesn't try the off-hand item instead; the server does the pick-up.
	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (Carrying.isCarrying(player)) {
				return Carrying.place(player, hand, hit);
			}
			BlockPos pos = hit.getBlockPos();
			if (hand != InteractionHand.MAIN_HAND || !canPickUp(player, level, pos)) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
				return pickUp(serverPlayer, serverLevel, pos);
			}
			return InteractionResult.SUCCESS;
		});
	}

	// Empty hand, a block from one of the tags (sneaking for containers), and allowed to change blocks here (adventure mode, spawn protection).
	private static boolean canPickUp(Player player, Level level, BlockPos pos) {
		if (player.isSpectator() || !player.getMainHandItem().isEmpty() || !level.mayInteract(player, pos)
				|| player.blockActionRestricted(level, pos, player.gameMode())) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		return state.is(PICKED_UP_BY_HAND) || state.is(PICKED_UP_BY_SNEAKING) && player.isShiftKeyDown();
	}

	private static InteractionResult pickUp(ServerPlayer player, ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		BlockEntity blockEntity = level.getBlockEntity(pos);
		boolean carried = state.is(PICKED_UP_BY_SNEAKING) && !state.is(BlockTags.SHULKER_BOXES);
		ItemStack item = state.getCloneItemStack(level, pos, false);
		// Someone has this stonecutter open: its storage is in their menu, so it stays put.
		if (state.is(Blocks.STONECUTTER) && StonecutterStorage.inUse(level, pos, null)) {
			StonecutterStorage.sendInUse(player);
			return InteractionResult.FAIL;
		}
		// Protection mods (claims) get their say like for a normal break.
		if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity)) {
			return InteractionResult.FAIL;
		}

		if (state.is(PICKED_UP_BY_SNEAKING)) {
			if (blockEntity instanceof RandomizableContainer lootContainer) {
				lootContainer.unpackLootTable(player);
			}
			// Vanilla's creative Ctrl+pick-block copy: the block entity's data and its item components (contents, bees, name, lock...).
			item = state.getCloneItemStack(level, pos, true);
			ServerGamePacketListenerImplInvoker.vsbetterqol$addBlockDataToItem(state, level, pos, item);
			// A stonecutter has no block entity; its storage rides along as the item's contents (BlockItemMixin puts it back).
			if (state.is(Blocks.STONECUTTER)) {
				item.set(DataComponents.CONTAINER, StonecutterStorage.take(level, pos));
			}
		}

		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
		level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
		if (state.is(BlockTags.GUARDED_BY_PIGLINS)) {
			PiglinAi.angerNearbyPiglins(level, player, false);
		}
		// Skipping the block entity side effects is what stops the contents dropping (and the enchanting table's lapis, BlockEntityMixin).
		level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
		if (carried) {
			Carrying.pickUp(player, item);
		} else {
			player.getInventory().placeItemBackInInventory(item, Prediction.SERVER_ONLY);
		}
		return InteractionResult.SUCCESS;
	}
}
