package com.boaringpanda.bettervanillafeatures;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class NameTagRenaming {
	private NameTagRenaming() {
	}

	// Sent by the client's name tag screen when Accept is pressed. The server checks everything again before renaming.
	public record RenamePayload(InteractionHand hand, String name) implements CustomPacketPayload {
		public static final Type<RenamePayload> TYPE = new Type<>(BetterVanillaFeatures.id("rename_name_tag"));
		public static final StreamCodec<ByteBuf, RenamePayload> CODEC = StreamCodec.composite(
				InteractionHand.STREAM_CODEC, RenamePayload::hand,
				ByteBufCodecs.STRING_UTF8, RenamePayload::name,
				RenamePayload::new);

		@Override
		public Type<RenamePayload> type() {
			return TYPE;
		}
	}

	// Creative needs no ink sac; survival needs one anywhere in the inventory.
	public static boolean canRename(Player player) {
		return player.hasInfiniteMaterials() || player.getInventory().contains(stack -> stack.is(Items.INK_SAC));
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(RenamePayload.TYPE, RenamePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RenamePayload.TYPE, (payload, context) -> rename(context.player(), payload));
	}

	// Same name rules as the anvil: filtered text, max 50 characters, and the whole stack is renamed for one cost.
	private static void rename(ServerPlayer player, RenamePayload payload) {
		ItemStack nameTag = player.getItemInHand(payload.hand());
		String name = StringUtil.filterText(payload.name());
		if (!nameTag.is(Items.NAME_TAG) || !canRename(player) || StringUtil.isBlank(name) || name.length() > AnvilMenu.MAX_NAME_LENGTH) {
			return;
		}

		player.getTextFilter().processStreamMessage(name);
		nameTag.set(DataComponents.CUSTOM_NAME, Component.literal(name));
		if (!player.hasInfiniteMaterials()) {
			for (ItemStack stack : player.getInventory()) {
				if (stack.is(Items.INK_SAC)) {
					stack.shrink(1);
					break;
				}
			}
		}

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.INK_SAC_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
	}
}
