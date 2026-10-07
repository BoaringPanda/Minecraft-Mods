package com.boaringpanda.vsbetterqol.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;

// The stonecutter screen gets a row of 9 storage slots between the recipes and the inventory (StonecutterMenuMixin). No new texture:
// vanilla's own background is drawn in pieces with a 22 px band slotted in, made of its plain grey rows and its own slot row.
@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenMixin extends AbstractContainerScreen<StonecutterMenu> {
	@Unique
	private static final int SHIFT = 22;
	// Texture rows: where the band goes in (just above the "Inventory" label), plain grey rows to copy, and the inventory's top slot row.
	@Unique
	private static final int SPLIT_Y = 72;
	@Unique
	private static final int PLAIN_HEIGHT = 11;
	@Unique
	private static final int SLOT_ROW_X = 7;
	@Unique
	private static final int SLOT_ROW_Y = 83;
	@Unique
	private static final int SLOT_ROW_WIDTH = 162;
	@Unique
	private static final int SLOT_ROW_HEIGHT = 18;
	@Unique
	private static final int VANILLA_HEIGHT = 166;
	@Unique
	private static final int VANILLA_SLOTS = 38;

	protected StonecutterScreenMixin(StonecutterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	// Vanilla's menu has 38 slots; the storage adds 9 more (only when the server has the mod too).
	@Unique
	private boolean vsbetterqol$hasStorage() {
		return this.menu.slots.size() > VANILLA_SLOTS;
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void vsbetterqol$makeRoom(StonecutterMenu menu, Inventory inventory, Component title, CallbackInfo ci) {
		if (this.vsbetterqol$hasStorage()) {
			((AbstractContainerScreenAccessor) this).vsbetterqol$setImageHeight(VANILLA_HEIGHT + SHIFT);
			this.inventoryLabelY += SHIFT;
		}
	}

	@WrapOperation(
			method = "extractBackground",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
	private void vsbetterqol$drawWithStorageRow(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y, float u,
			float v, int width, int height, int textureWidth, int textureHeight, Operation<Void> original) {
		if (!this.vsbetterqol$hasStorage()) {
			original.call(graphics, pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
			return;
		}
		// Top: title, input, recipes, result.
		original.call(graphics, pipeline, texture, x, y, 0.0F, 0.0F, width, SPLIT_Y, textureWidth, textureHeight);
		// The band: plain grey twice, then the 9 slot frames on it (slot items sit at y 75).
		original.call(graphics, pipeline, texture, x, y + SPLIT_Y, 0.0F, (float) SPLIT_Y, width, PLAIN_HEIGHT, textureWidth, textureHeight);
		original.call(graphics, pipeline, texture, x, y + SPLIT_Y + PLAIN_HEIGHT, 0.0F, (float) SPLIT_Y, width, PLAIN_HEIGHT, textureWidth,
				textureHeight);
		original.call(graphics, pipeline, texture, x + SLOT_ROW_X, y + SPLIT_Y + 2, (float) SLOT_ROW_X, (float) SLOT_ROW_Y, SLOT_ROW_WIDTH,
				SLOT_ROW_HEIGHT, textureWidth, textureHeight);
		// Bottom: the "Inventory" label area and the player's inventory, moved down.
		original.call(graphics, pipeline, texture, x, y + SPLIT_Y + SHIFT, 0.0F, (float) SPLIT_Y, width, VANILLA_HEIGHT - SPLIT_Y, textureWidth,
				textureHeight);
	}
}
