package com.boaringpanda.vsbetterqol.client.mixin;

import com.boaringpanda.vsbetterqol.VSBetterQOL;
import com.boaringpanda.vsbetterqol.client.ClientConfig;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;

// Saturation shows on the hunger bar as a bright green outline around the shanks (1 saturation = half a shank), right to left like
// the shanks. Saturation is never above the food level, so the outlines always sit on filled shanks. Holding food that can be eaten
// slowly flashes the shanks and outlines eating it would add.
@Mixin(Hud.class)
public abstract class HudMixin {
	@Unique
	private static final Identifier SATURATION_FULL = VSBetterQOL.id("hud/saturation_full");
	@Unique
	private static final Identifier SATURATION_HALF = VSBetterQOL.id("hud/saturation_half");
	@Unique
	private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	@Unique
	private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
	@Unique
	private static final Identifier FOOD_FULL_HUNGER = Identifier.withDefaultNamespace("hud/food_full_hunger");
	@Unique
	private static final Identifier FOOD_HALF_HUNGER = Identifier.withDefaultNamespace("hud/food_half_hunger");
	@Unique
	private static final float FLASH_PERIOD_MS = 2000.0F;

	// Same spots as vanilla's shanks. They only jiggle at 0 saturation, when there's no outline.
	@Inject(method = "extractFood", at = @At("TAIL"))
	private void vsbetterqol$extractSaturation(GuiGraphicsExtractor graphics, Player player, int yLineBase, int xRight, CallbackInfo ci) {
		if (!ClientConfig.SATURATION_BAR.on) {
			return;
		}
		FoodData foodData = player.getFoodData();
		int food = foodData.getFoodLevel();
		float saturation = foodData.getSaturationLevel();
		for (int i = 0; i < 10; i++) {
			Identifier sprite = vsbetterqol$sprite(saturation, i, SATURATION_FULL, SATURATION_HALF);
			if (sprite != null) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, xRight - i * 8 - 9, yLineBase, 9, 9);
			}
		}

		// The hand vanilla eats from first, and only if it could be eaten right now.
		FoodProperties eaten = player.getMainHandItem().get(DataComponents.FOOD);
		if (eaten == null) {
			eaten = player.getOffhandItem().get(DataComponents.FOOD);
		}
		if (eaten == null || !player.canEat(eaten.canAlwaysEat())) {
			return;
		}
		// FoodData.add: food is capped at 20, saturation at the new food level.
		int newFood = Math.min(food + eaten.nutrition(), 20);
		float newSaturation = Math.min(saturation + eaten.saturation(), newFood);
		float pulse = (Mth.sin(Util.getMillis() * Mth.TWO_PI / FLASH_PERIOD_MS) + 1.0F) / 2.0F;
		int color = ((int) ((0.2F + 0.6F * pulse) * 255.0F) << 24) | 0xFFFFFF;
		boolean hunger = player.hasEffect(MobEffects.HUNGER);
		Identifier full = hunger ? FOOD_FULL_HUNGER : FOOD_FULL;
		Identifier half = hunger ? FOOD_HALF_HUNGER : FOOD_HALF;
		for (int i = 0; i < 10; i++) {
			int x = xRight - i * 8 - 9;
			Identifier shank = vsbetterqol$sprite(newFood, i, full, half);
			if (shank != vsbetterqol$sprite(food, i, full, half) && shank != null) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, shank, x, yLineBase, 9, 9, color);
			}
			Identifier outline = vsbetterqol$sprite(newSaturation, i, SATURATION_FULL, SATURATION_HALF);
			if (outline != vsbetterqol$sprite(saturation, i, SATURATION_FULL, SATURATION_HALF) && outline != null) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, outline, x, yLineBase, 9, 9, color);
			}
		}
	}

	// Shank i (0 = rightmost) of a bar filled to this many half shanks.
	@Unique
	private static @Nullable Identifier vsbetterqol$sprite(float level, int i, Identifier full, Identifier half) {
		return level >= i * 2 + 2 ? full : level >= i * 2 + 1 ? half : null;
	}
}
