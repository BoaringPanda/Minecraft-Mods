package com.boaringpanda.vsbetterqol.client.mixin;

import com.boaringpanda.vsbetterqol.ServerConfig;
import com.boaringpanda.vsbetterqol.VillagerReroll;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;

// A small ⟳ button right after the "Trades" label that re-rolls the villager's trades (VillagerReroll). The label and button are centred
// together over the trade list (Dylan's spot). Greyed out with the reason on hover when the villager can't be re-rolled or during the
// 3 second wait. Not shown for wandering traders, or on a server without the mod (it couldn't work there); the label is vanilla then.
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
	@Unique
	private static final int SIZE = 12;
	@Unique
	private static final int GAP = 3;
	// Vanilla centres "Trades" on this x (5 + 48), with the text at y 6; the button is centred on that line.
	@Unique
	private static final int LABEL_CENTRE = 53;
	@Unique
	private static final int Y = 4;
	@Unique
	private static final long COOLDOWN_MS = VillagerReroll.COOLDOWN_TICKS * 50L;
	// When this player last pressed it (kept between screens, so reopening doesn't show it as ready early; the server checks anyway).
	@Unique
	private static long vsbetterqol$lastReroll;

	@Shadow
	@Final
	private static Component TRADES_LABEL;
	@Shadow
	private int shopItem;
	@Shadow
	private int scrollOff;

	@Unique
	private @Nullable Button vsbetterqol$reroll;

	protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void vsbetterqol$addReroll(CallbackInfo ci) {
		this.vsbetterqol$reroll = this.addRenderableWidget(Button.builder(Component.literal("⟳"), button -> {
			ClientPlayNetworking.send(VillagerReroll.RerollPayload.INSTANCE);
			vsbetterqol$lastReroll = Util.getMillis();
			// The new trades start from the top, nothing picked.
			this.shopItem = 0;
			this.scrollOff = 0;
		}).bounds(0, 0, SIZE, SIZE).build());
		this.vsbetterqol$updateReroll();
	}

	// Every frame before the widgets are drawn, since the state changes (level and xp arrive after the screen opens, the wait runs out).
	@Inject(method = "extractBackground", at = @At("HEAD"))
	private void vsbetterqol$beforeBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		this.vsbetterqol$updateReroll();
	}

	// "Trades" (vanilla's last label) moves left to make room, so label + gap + button are centred where the label alone was.
	@ModifyArg(method = "extractLabels", index = 2, at = @At(value = "INVOKE", ordinal = 3,
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"))
	private int vsbetterqol$moveTradesLabel(int x) {
		return this.vsbetterqol$reroll != null && this.vsbetterqol$reroll.visible ? this.vsbetterqol$labelX() : x;
	}

	@Unique
	private int vsbetterqol$labelX() {
		return LABEL_CENTRE - (this.font.width(TRADES_LABEL) + GAP + SIZE) / 2;
	}

	@Unique
	private void vsbetterqol$updateReroll() {
		Button button = this.vsbetterqol$reroll;
		if (button == null) {
			return;
		}
		button.visible = this.menu.showProgressBar() && ServerConfig.clientServerHasMod();
		button.setPosition(this.leftPos + this.vsbetterqol$labelX() + this.font.width(TRADES_LABEL) + GAP, this.topPos + Y);

		long waitMs = COOLDOWN_MS - (Util.getMillis() - vsbetterqol$lastReroll);
		boolean allowed = VillagerReroll.canReroll(this.menu.getTraderLevel(), this.menu.getTraderXp());
		button.active = allowed && waitMs <= 0;
		button.setTooltip(Tooltip.create(!allowed ? Component.translatable("gui.vsbetterqol.reroll.locked")
				: waitMs > 0 ? Component.translatable("gui.vsbetterqol.reroll.cooldown", (waitMs + 999) / 1000)
				: Component.translatable("gui.vsbetterqol.reroll")));
	}
}
