package com.boaringpanda.vsbetterqol.client;

import com.boaringpanda.vsbetterqol.Carrying;
import com.boaringpanda.vsbetterqol.ServerConfig;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public class VSBetterQOLClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientConfig.load();
		// The server's settings for swords and double doors (ServerConfig). Cleared on every new connection, so on a server without the
		// mod (which never sends them) those stay vanilla.
		ClientPlayConnectionEvents.INIT.register((handler, client) -> ServerConfig.clearClientView());
		ClientPlayNetworking.registerGlobalReceiver(ServerConfig.SyncPayload.TYPE, (payload, context) -> ServerConfig.setClientView(payload.on()));
		// Draws the hunger/saturation image foods get in their tooltip (mixin/ItemStackTooltipMixin).
		ClientTooltipComponentCallback.EVENT.register(data -> data instanceof FoodTooltip food ? new ClientFoodTooltip(food) : null);
		// A carried container (Carrying), drawn in front of every player carrying one.
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatarRenderer) {
				helper.register(new CarriedBlockLayer(avatarRenderer));
			}
		});
		// Hands full while carrying: the inventory, drop, swap-hands, pick-block and hotbar keys do nothing. Eaten before vanilla reads them.
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player != null && Carrying.isCarrying(client.player)) {
				Options options = client.options;
				blockKeys(options.keyInventory, options.keyDrop, options.keySwapOffhand, options.keyPickItem);
				blockKeys(options.keyHotbarSlots);
			}
		});
	}

	private static void blockKeys(KeyMapping... keys) {
		for (KeyMapping key : keys) {
			while (key.consumeClick()) {
				// Throw the press away.
			}
		}
	}
}
