package com.boaringpanda.vslumberjackmod;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.StonecutterMenu;

// Vanilla's stonecutter menu, opened from the woodcutter. Which recipes it shows is filtered in mixin/StonecutterMenuMixin.
public class WoodcutterMenu extends StonecutterMenu {
	private final ContainerLevelAccess access;

	// Client side, from the open-screen packet.
	public WoodcutterMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public WoodcutterMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(containerId, inventory, access);
		this.access = access;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, Woodcutter.BLOCK);
	}

	@Override
	public MenuType<?> getType() {
		return Woodcutter.MENU;
	}
}
