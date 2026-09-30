package com.boaringpanda.bettervanillaqol.client;

import com.boaringpanda.bettervanillaqol.NameTagRenaming;
import com.boaringpanda.bettervanillaqol.NameTagRenaming.RenamePayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BetterVanillaQOLClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Right-clicking with a name tag opens the rename screen (needs an ink sac outside creative). Only when the server has the
		// mod; otherwise the name tag acts like vanilla. The event also fires for the singleplayer server, which must pass.
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!level.isClientSide() || !stack.is(Items.NAME_TAG) || !ClientPlayNetworking.canSend(RenamePayload.TYPE)) {
				return InteractionResult.PASS;
			}
			if (!NameTagRenaming.canRename(player)) {
				player.sendOverlayMessage(Component.translatable("message.bettervanillaqol.ink_sac_needed"));
				return InteractionResult.FAIL;
			}
			Minecraft.getInstance().gui.setScreen(new NameTagScreen(hand, stack));
			return InteractionResult.SUCCESS;
		});
	}
}
