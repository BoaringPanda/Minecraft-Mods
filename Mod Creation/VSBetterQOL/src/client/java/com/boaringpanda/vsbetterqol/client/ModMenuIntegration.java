package com.boaringpanda.vsbetterqol.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

// Mod Menu's settings button for this mod (the "modmenu" entrypoint in fabric.mod.json). Only Mod Menu loads this class, so the mod
// runs fine without Mod Menu installed.
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ClientConfigScreen::new;
	}
}
