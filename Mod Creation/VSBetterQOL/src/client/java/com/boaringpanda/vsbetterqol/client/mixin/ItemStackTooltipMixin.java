package com.boaringpanda.vsbetterqol.client.mixin;

import java.util.Optional;

import com.boaringpanda.vsbetterqol.client.ClientConfig;
import com.boaringpanda.vsbetterqol.client.FoodTooltip;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

// Foods get a tooltip image with their hunger and saturation (ClientFoodTooltip). Vanilla puts it right under the name.
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
	@ModifyReturnValue(method = "getTooltipImage", at = @At("RETURN"))
	private Optional<TooltipComponent> vsbetterqol$addFoodTooltip(Optional<TooltipComponent> image) {
		FoodProperties food = ((ItemStack) (Object) this).get(DataComponents.FOOD);
		if (!ClientConfig.FOOD_TOOLTIP.on || image.isPresent() || food == null || food.nutrition() <= 0 && food.saturation() <= 0) {
			return image;
		}
		return Optional.of(new FoodTooltip(food.nutrition(), food.saturation()));
	}
}
