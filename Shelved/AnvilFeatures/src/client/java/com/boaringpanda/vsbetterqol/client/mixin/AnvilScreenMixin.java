package com.boaringpanda.vsbetterqol.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;

import com.boaringpanda.vsbetterqol.AnvilDurability;

// Draws the anvil's durability above the anvil window: the villager trade screen's XP bar (102 px, green) and "uses left/max".
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {
	@Unique
	private static final Identifier BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_background");
	@Unique
	private static final Identifier BAR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_current");
	@Unique
	private static final int BAR_WIDTH = 102;
	@Unique
	private static final int BAR_HEIGHT = 5;
	// Vanilla's green for the XP level number and the anvil's affordable cost line.
	@Unique
	private static final int GREEN = 0xFF80FF20;

	private AnvilScreenMixin(AnvilMenu menu, Inventory inventory, Component title, Identifier menuResource) {
		super(menu, inventory, title, menuResource);
	}

	// Free jobs (name tag renames, repairs, see AnvilMenuMixin) have cost 0, where vanilla draws no cost line. Show "Free" in vanilla's
	// cost-line spot and green instead.
	@Inject(method = "extractLabels", at = @At("TAIL"))
	private void vsbetterqol$extractFreeLabel(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
		if (this.menu.getCost() != 0 || !this.menu.getSlot(AnvilMenu.RESULT_SLOT).hasItem()) {
			return;
		}

		Component line = Component.translatable("container.vsbetterqol.repair.free");
		int tx = this.imageWidth - 8 - this.font.width(line) - 2;
		graphics.fill(tx - 2, 67, this.imageWidth - 8, 79, 0x4F000000);
		graphics.text(this.font, line, tx, 69, GREEN);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void vsbetterqol$extractDurabilityBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		int usesLeft = ((AnvilDurability.View) this.menu).vsbetterqol$usesLeft();
		if (usesLeft < 0) {
			return;
		}

		String text = usesLeft + "/" + AnvilDurability.MAX_USES;
		int width = BAR_WIDTH + 4 + this.font.width(text);
		int x = this.leftPos + (this.imageWidth - width) / 2;
		int y = this.topPos - 9;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND_SPRITE, x, y, BAR_WIDTH, BAR_HEIGHT);
		int progress = BAR_WIDTH * usesLeft / AnvilDurability.MAX_USES;
		if (progress > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_PROGRESS_SPRITE, BAR_WIDTH, BAR_HEIGHT, 0, 0, x, y, progress, BAR_HEIGHT);
		}
		graphics.text(this.font, text, x + BAR_WIDTH + 4, y - 2, GREEN);
	}
}
