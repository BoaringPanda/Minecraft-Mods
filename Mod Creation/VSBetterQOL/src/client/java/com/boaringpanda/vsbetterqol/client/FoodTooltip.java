package com.boaringpanda.vsbetterqol.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

// A food's hunger and saturation (both in half shanks), shown under its name by ClientFoodTooltip.
public record FoodTooltip(int nutrition, float saturation) implements TooltipComponent {
}
