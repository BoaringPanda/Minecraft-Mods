package com.boaringpanda.vsbetterqol;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

// A picked-up container (see PickingUp) is carried in both arms instead of going into the inventory: Slowness I, no sprinting, hotbar
// locked, and no attacking, mining or using anything until it's set down with a right-click on a block. The carried block is an item
// stack (vanilla's Ctrl+pick-block copy, contents included) kept on the player, saved with them and sent to every client so they can
// draw it (client/CarriedBlockLayer).
public final class Carrying {
	private static final AttachmentType<ItemStack> CARRIED = AttachmentRegistry.create(
			VSBetterQOL.id("carried"),
			builder -> builder.persistent(ItemStack.CODEC).syncWith(ItemStack.STREAM_CODEC, AttachmentSyncPredicate.all()));

	private Carrying() {
	}

	public static void register() {
		// Hands are full: no mining, hitting, using items or using entities. Both sides, so the client doesn't even start.
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> isCarrying(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> isCarrying(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> isCarrying(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, level, hand) -> isCarrying(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		// Dying while carrying spills it like it was broken: the contents scatter and the empty block drops.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player && isCarrying(player)) {
				CarriedBrewing.flush(player);
				spill(player, player.removeAttached(CARRIED));
			}
		});
	}

	public static boolean isCarrying(Player player) {
		return player.hasAttached(CARRIED);
	}

	// The carried block, or empty.
	public static ItemStack carried(Player player) {
		return player.getAttachedOrElse(CARRIED, ItemStack.EMPTY);
	}

	public static void pickUp(ServerPlayer player, ItemStack stack) {
		CarriedBrewing.forget(player);
		player.setAttached(CARRIED, stack);
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, MobEffectInstance.INFINITE_DURATION, 0));
	}

	// The carried block changed while carrying it (a carried brewing stand brewing, CarriedBrewing).
	public static void replace(ServerPlayer player, ItemStack stack) {
		if (isCarrying(player)) {
			player.setAttached(CARRIED, stack);
		}
	}

	// Right-click on a block while carrying: placed like the block item would be (vanilla's placement rules, facing, sound, contents
	// loaded from the stack). Runs on the client too, so the block and sound show straight away like any placement.
	public static InteractionResult place(Player player, InteractionHand hand, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			CarriedBrewing.flush(serverPlayer);
		}
		ItemStack stack = carried(player);
		if (hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof BlockItem blockItem)) {
			return InteractionResult.FAIL;
		}
		// A copy, since placing uses the stack up and the client's copy is the synced one.
		InteractionResult result = blockItem.place(new BlockPlaceContext(player, hand, stack.copy(), hit));
		if (result.consumesAction() && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.removeAttached(CARRIED);
			removeOurSlowness(serverPlayer);
		}
		return result.consumesAction() ? result : InteractionResult.FAIL;
	}

	// Only the endless Slowness I from picking up, so a slowness potion that was already running isn't taken away.
	private static void removeOurSlowness(ServerPlayer player) {
		MobEffectInstance slowness = player.getEffect(MobEffects.SLOWNESS);
		if (slowness != null && slowness.isInfiniteDuration() && slowness.getAmplifier() == 0) {
			player.removeEffect(MobEffects.SLOWNESS);
		}
	}

	private static void spill(ServerPlayer player, ItemStack stack) {
		ItemContainerContents contents = stack.remove(DataComponents.CONTAINER);
		if (contents != null) {
			contents.nonEmptyItemCopyStream().forEach(item -> Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), item));
		}
		// The rest of the saved block data (brewing fuel and progress) is lost, like breaking it.
		stack.remove(DataComponents.BLOCK_ENTITY_DATA);
		Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
	}
}
