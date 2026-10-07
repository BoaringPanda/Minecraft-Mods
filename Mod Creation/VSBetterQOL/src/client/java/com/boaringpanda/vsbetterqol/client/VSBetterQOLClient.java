package com.boaringpanda.vsbetterqol.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;

public class VSBetterQOLClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SortButtons.register();
		// Draws the hunger/saturation image foods get in their tooltip (mixin/ItemStackTooltipMixin).
		ClientTooltipComponentCallback.EVENT.register(data -> data instanceof FoodTooltip food ? new ClientFoodTooltip(food) : null);
	}
}
