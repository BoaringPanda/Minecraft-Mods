package com.boaringpanda.vsbetterqol.client.mixin;

import java.util.Locale;

import com.boaringpanda.vsbetterqol.client.ClientConfig;
import com.boaringpanda.vsbetterqol.client.ClientConfig.SpeedBarPosition;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;

// While gliding with an elytra, a bar shows the speed in blocks per second, filling up and fading green → yellow → red. The colours follow
// vanilla's wall-crash damage (LivingEntity.handleFallFlyingCollisions: speed lost in blocks/tick × 10 - 3): green up to SAFE_SPEED (a
// crash does nothing), red from DEADLY_SPEED (a crash does 20 damage). It sits above the hearts by default, or in a corner / top middle
// (ClientConfig.ELYTRA_SPEED_POSITION). Above the hearts, vanilla's held item name and action bar move up above it.
@Mixin(Hud.class)
public abstract class ElytraSpeedHudMixin {
	@Unique
	private static final float SAFE_SPEED = 6.0F;
	@Unique
	private static final float DEADLY_SPEED = 46.0F;
	@Unique
	private static final int WIDTH = 91;
	@Unique
	private static final int HEIGHT = 11;
	// Gap from the screen edges in the corner and top positions.
	@Unique
	private static final int MARGIN = 4;
	@Unique
	private static final int GREEN = 0xFF55FF55;
	@Unique
	private static final int YELLOW = 0xFFFFFF55;
	@Unique
	private static final int RED = 0xFFFF5555;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
	private void vsbetterqol$extractElytraSpeed(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		LocalPlayer player = this.minecraft.player;
		if (!vsbetterqol$showing(player)) {
			return;
		}
		// How far the player moved in the last tick.
		float speed = (float) Math.sqrt(Mth.lengthSquared(player.getX() - player.xo, player.getY() - player.yo, player.getZ() - player.zo)) * 20.0F;
		float danger = Mth.clamp((speed - SAFE_SPEED) / (DEADLY_SPEED - SAFE_SPEED), 0.0F, 1.0F);
		int color = danger < 0.5F ? ARGB.srgbLerp(danger * 2.0F, GREEN, YELLOW) : ARGB.srgbLerp(danger * 2.0F - 1.0F, YELLOW, RED);
		int filled = Math.round(Math.min(speed / DEADLY_SPEED, 1.0F) * (WIDTH - 2));
		Font font = this.minecraft.font;

		// The size slider scales it from the corner (or edge) it's anchored to.
		float scale = ClientConfig.ELYTRA_SPEED_SIZE.value / 100.0F;
		float width = WIDTH * scale;
		float height = HEIGHT * scale;
		int screenWidth = graphics.guiWidth();
		int screenHeight = graphics.guiHeight();
		float x = switch (ClientConfig.ELYTRA_SPEED_POSITION.value) {
			case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
			case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - width - MARGIN;
			case ABOVE_HOTBAR, TOP_MIDDLE -> screenWidth / 2 - WIDTH / 2 * scale;
		};
		float y = switch (ClientConfig.ELYTRA_SPEED_POSITION.value) {
			case TOP_LEFT, TOP_MIDDLE, TOP_RIGHT -> MARGIN;
			case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - height - MARGIN;
			case ABOVE_HOTBAR -> vsbetterqol$bottom(graphics, player) - height;
		};

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale);
		graphics.fill(0, 0, WIDTH, HEIGHT, 0xFF000000);
		graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, 0xFF303030);
		graphics.fill(1, 1, 1 + filled, HEIGHT - 1, color);
		graphics.centeredText(font, Component.translatable("hud.vsbetterqol.elytra_speed", String.format(Locale.ROOT, "%.1f", speed)),
				WIDTH / 2, 1, 0xFFFFFFFF);
		graphics.pose().popMatrix();
	}

	// The held item's name sits right above the bar instead of on it (only when the hearts show; otherwise vanilla puts it lower anyway).
	@ModifyExpressionValue(method = "extractSelectedItemName", at = @At(value = "CONSTANT", args = "intValue=59"))
	private int vsbetterqol$itemNameAboveSpeed(int fromBottom, GuiGraphicsExtractor graphics) {
		return fromBottom + vsbetterqol$moveUp(graphics, fromBottom, 9);
	}

	// Action bar messages (drawn 4 px above and below this line) sit right above the bar too.
	@ModifyExpressionValue(method = "extractOverlayMessage", at = @At(value = "CONSTANT", args = "intValue=68"))
	private int vsbetterqol$messageAboveSpeed(int fromBottom, GuiGraphicsExtractor graphics) {
		return fromBottom + vsbetterqol$moveUp(graphics, fromBottom - 4, 9);
	}

	// How far text drawn from (height - fromBottom), this tall, has to move up to clear the bar. Only above the hotbar, where they meet.
	@Unique
	private int vsbetterqol$moveUp(GuiGraphicsExtractor graphics, int fromBottom, int textHeight) {
		LocalPlayer player = this.minecraft.player;
		if (!vsbetterqol$showing(player) || ClientConfig.ELYTRA_SPEED_POSITION.value != SpeedBarPosition.ABOVE_HOTBAR
				|| !this.minecraft.gameMode.canHurtPlayer()) {
			return 0;
		}
		float top = vsbetterqol$bottom(graphics, player) - HEIGHT * ClientConfig.ELYTRA_SPEED_SIZE.value / 100.0F;
		return Math.max(0, Mth.ceil(graphics.guiHeight() - fromBottom + textHeight + 1 - top));
	}

	@Unique
	private static boolean vsbetterqol$showing(LocalPlayer player) {
		return ClientConfig.ELYTRA_SPEED.on && player != null && player.isFallFlying();
	}

	// Above the hotbar: 1 px above the armor row, which vanilla moves up with extra heart rows (Hud.extractPlayerHealth's maths).
	@Unique
	private static int vsbetterqol$bottom(GuiGraphicsExtractor graphics, LocalPlayer player) {
		float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), Mth.ceil(player.getHealth()));
		int rows = Mth.ceil((maxHealth + Mth.ceil(player.getAbsorptionAmount())) / 2.0F / 10.0F);
		int rowHeight = Math.max(10 - (rows - 2), 3);
		return graphics.guiHeight() - 39 - (rows - 1) * rowHeight - 10 - 1;
	}
}
