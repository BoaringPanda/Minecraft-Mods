package com.boaringpanda.extrablocks.client;

import net.fabricmc.api.ClientModInitializer;

import com.boaringpanda.extrablocks.client.block.LilyPadAccessoryColors;

public class ExtraBlocksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		LilyPadAccessoryColors.initialize();
	}
}