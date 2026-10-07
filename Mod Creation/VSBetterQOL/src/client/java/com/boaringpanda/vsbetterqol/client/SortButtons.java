package com.boaringpanda.vsbetterqol.client;

import java.util.List;

import com.boaringpanda.vsbetterqol.InventorySorting;
import com.boaringpanda.vsbetterqol.InventorySorting.SortPayload;
import com.boaringpanda.vsbetterqol.VSBetterQOL;
import com.boaringpanda.vsbetterqol.client.mixin.AbstractContainerScreenAccessor;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

// A small "Sort A-Z" button above the top-right slot of the survival inventory (and creative's Survival Inventory tab), and of
// chests/barrels/ender chests/shulker boxes (those get one for the container and one for the player's inventory). Only when the
// server has the mod (InventorySorting).
public final class SortButtons {
	private static final WidgetSprites SPRITES = new WidgetSprites(VSBetterQOL.id("sort_button"), VSBetterQOL.id("sort_button_highlighted"));
	private static final int SIZE = 10;

	private SortButtons() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!ClientConfig.SORT_BUTTONS.on || !ClientPlayNetworking.canSend(SortPayload.TYPE)) {
				return;
			}
			if (screen instanceof InventoryScreen || screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen) {
				addButton((AbstractContainerScreen<?>) screen, client, true);
				addButton((AbstractContainerScreen<?>) screen, client, false);
			} else if (screen instanceof CreativeModeInventoryScreen creative) {
				addButton(creative, client, true);
			}
		});
	}

	private static void addButton(AbstractContainerScreen<?> screen, Minecraft client, boolean playerInventory) {
		boolean creative = screen instanceof CreativeModeInventoryScreen;
		if (!creative && topRightSlot(screen, client, playerInventory) == null) {
			return;
		}
		// The creative screen's own menu is client-only. Its Survival Inventory tab shows the player's inventory menu, which is
		// what the server has open.
		int containerId = creative ? client.player.inventoryMenu.containerId : screen.getMenu().containerId;
		Component label = Component.translatable("gui.vsbetterqol.sort");
		ImageButton button = new ImageButton(SIZE, SIZE, SPRITES,
				pressed -> ClientPlayNetworking.send(new SortPayload(containerId, playerInventory)), label);
		button.setTooltip(Tooltip.create(label));
		Screens.getWidgets(screen).add(button);

		// Centred over the 16 px slot, just above its frame. Placed every frame, since the recipe book moves the GUI and creative tabs
		// swap the slots without a re-init. Hidden when there's no slot (creative tabs other than Survival Inventory).
		AbstractContainerScreenAccessor pos = (AbstractContainerScreenAccessor) screen;
		ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
			Slot topRight = topRightSlot(screen, client, playerInventory);
			button.visible = topRight != null;
			if (topRight != null) {
				button.setPosition(pos.getLeftPos() + topRight.x + (16 - SIZE) / 2, pos.getTopPos() + topRight.y - SIZE - 3);
			}
		});
	}

	// First row's last slot of what the button sorts. In creative, the inventory tab's slot wrappers count their menu index as the
	// container slot, and the player's main inventory is 9..35 both ways.
	private static @Nullable Slot topRightSlot(AbstractContainerScreen<?> screen, Minecraft client, boolean playerInventory) {
		List<Slot> slots = InventorySorting.sortableSlots(screen.getMenu(), client.player.getInventory(), playerInventory);
		return slots.size() < 9 ? null : slots.get(8);
	}
}
