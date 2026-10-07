package com.boaringpanda.vsbetterqol.client.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Ordering;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

// Status effects show in a column in the top right, each as vanilla's icon box at 3/4 size with the time left beside it (the
// inventory's text, at half size): good effects first, then the rest. A full column carries on in a new column to its left. Replaces
// vanilla's rows of icons.
@Mixin(Hud.class)
public abstract class EffectHudMixin {
	@Unique
	private static final Identifier EFFECT_BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background");
	@Unique
	private static final Identifier EFFECT_BACKGROUND_AMBIENT = Identifier.withDefaultNamespace("hud/effect_background_ambient");
	@Unique
	private static final float ICON_SCALE = 0.75F;
	@Unique
	private static final float TEXT_SCALE = 0.5F;
	// Vanilla's 24px box at ICON_SCALE, and a 1px gap.
	@Unique
	private static final int BOX = 18;
	@Unique
	private static final int ROW_HEIGHT = BOX + 1;
	// Columns stop this far above the bottom, so outer columns never cover the hotbar, hearts, armor, food or air.
	@Unique
	private static final int BOTTOM_SPACE = 50;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
	private void vsbetterqol$extractEffectColumns(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		ci.cancel();
		// Vanilla's checks: nothing to show, or a screen already lists them (none in vanilla now, see EffectsInInventoryMixin).
		Collection<MobEffectInstance> active = this.minecraft.player.getActiveEffects();
		Screen screen = this.minecraft.gui.screen();
		if (active.isEmpty() || screen != null && screen.showsActiveEffects()) {
			return;
		}
		List<MobEffectInstance> effects = new ArrayList<>();
		List<MobEffectInstance> rest = new ArrayList<>();
		for (MobEffectInstance instance : Ordering.natural().reverse().sortedCopy(active)) {
			if (instance.showIcon()) {
				(instance.getEffect().value().isBeneficial() ? effects : rest).add(instance);
			}
		}
		effects.addAll(rest);

		Font font = this.minecraft.font;
		float tickrate = this.minecraft.level.tickRateManager().tickrate();
		int top = this.minecraft.isDemo() ? 16 : 1;
		int rows = Math.max((graphics.guiHeight() - BOTTOM_SPACE - top) / ROW_HEIGHT, 1);
		int right = graphics.guiWidth() - 1;
		for (int start = 0; start < effects.size(); start += rows) {
			List<MobEffectInstance> column = effects.subList(start, Math.min(start + rows, effects.size()));
			List<Component> times = new ArrayList<>();
			int textWidth = 0;
			for (MobEffectInstance instance : column) {
				Component time = MobEffectUtil.formatDuration(instance, 1.0F, tickrate);
				times.add(time);
				textWidth = Math.max(textWidth, Mth.ceil(font.width(time) * TEXT_SCALE));
			}
			int x = right - BOX;
			for (int row = 0; row < column.size(); row++) {
				MobEffectInstance instance = column.get(row);
				int y = top + row * ROW_HEIGHT;
				// Vanilla's box and icon at vanilla's sizes, scaled down.
				graphics.pose().pushMatrix();
				graphics.pose().translate(x, y);
				graphics.pose().scale(ICON_SCALE);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, instance.isAmbient() ? EFFECT_BACKGROUND_AMBIENT : EFFECT_BACKGROUND, 0, 0, 24, 24);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(instance.getEffect()), 3, 3, 18, 18,
						ARGB.white(vsbetterqol$alpha(instance)));
				graphics.pose().popMatrix();
				// The time, right-aligned 2px left of the box and centred on it.
				Component time = times.get(row);
				graphics.pose().pushMatrix();
				graphics.pose().translate(x - 2 - font.width(time) * TEXT_SCALE, y + (BOX - font.lineHeight * TEXT_SCALE) / 2.0F);
				graphics.pose().scale(TEXT_SCALE);
				graphics.text(font, time, 0, 0, 0xFFFFFFFF);
				graphics.pose().popMatrix();
			}
			right = x - 2 - textWidth - 4;
		}
	}

	// Vanilla's fade-blink for the last 10 seconds. Ambient (beacon) effects never blink.
	@Unique
	private static float vsbetterqol$alpha(MobEffectInstance instance) {
		if (instance.isAmbient() || !instance.endsWithin(200)) {
			return 1.0F;
		}
		int duration = instance.getDuration();
		int fade = 10 - duration / 20;
		float alpha = Mth.clamp(duration / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
				+ Mth.cos(duration * Mth.PI / 5.0F) * Mth.clamp(fade / 10.0F * 0.25F, 0.0F, 0.25F);
		return Mth.clamp(alpha, 0.0F, 1.0F);
	}
}
