package com.boaringpanda.lumberjackmod;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;

public class LumberjackModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The woodcutter opens vanilla's stonecutter screen (stonecutter GUI art until there's a woodcutter one).
		MenuScreens.register(Woodcutter.MENU, StonecutterScreen::new);
	}
}
