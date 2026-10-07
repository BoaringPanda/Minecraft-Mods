package com.boaringpanda.vsbetterqol.client;

import java.util.List;

import com.boaringpanda.vsbetterqol.ServerConfig;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;

public class VSBetterQOLClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientConfig.load();
		// The server's settings for swords and double doors (ServerConfig). Cleared on every new connection, so on a server without the
		// mod (which never sends them) those stay vanilla.
		ClientPlayConnectionEvents.INIT.register((handler, client) -> ServerConfig.setClientView(List.of()));
		ClientPlayNetworking.registerGlobalReceiver(ServerConfig.SyncPayload.TYPE, (payload, context) -> ServerConfig.setClientView(payload.on()));
		// Draws the hunger/saturation image foods get in their tooltip (mixin/ItemStackTooltipMixin).
		ClientTooltipComponentCallback.EVENT.register(data -> data instanceof FoodTooltip food ? new ClientFoodTooltip(food) : null);
	}
}
