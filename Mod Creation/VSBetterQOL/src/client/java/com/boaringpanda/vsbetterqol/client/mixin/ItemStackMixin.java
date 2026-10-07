package com.boaringpanda.vsbetterqol.client.mixin;

import java.util.function.Consumer;

import com.boaringpanda.vsbetterqol.client.ClientConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// Damaged items always show their durability in the tooltip, not just with F3+H (Advanced Tooltips), coloured like the bar under
// the item (green, yellow, red). F3+H still adds vanilla's item ID and component count.
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Shadow
	public abstract boolean isDamaged();

	@Shadow
	public abstract int getDamageValue();

	@Shadow
	public abstract int getMaxDamage();

	@Shadow
	public abstract int getBarColor();

	// Right before vanilla's F3+H block, where its own durability line would go.
	@Inject(
			method = "addDetailsToTooltip",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/TooltipFlag;isAdvanced()Z"))
	private void vsbetterqol$addDurability(Item.TooltipContext context, TooltipDisplay display, Player player, TooltipFlag tooltipFlag,
			Consumer<Component> builder, CallbackInfo ci) {
		if (ClientConfig.DURABILITY_TOOLTIP.on && this.isDamaged() && display.shows(DataComponents.DAMAGE)) {
			builder.accept(Component.translatable("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage())
					.withColor(this.getBarColor()));
		}
	}

	// Skips vanilla's uncoloured F3+H line, so durability never shows twice. Switched off (ClientConfig), it's all vanilla.
	@ModifyExpressionValue(
			method = "addDetailsToTooltip",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isDamaged()Z"))
	private boolean vsbetterqol$skipVanillaDurability(boolean damaged) {
		return !ClientConfig.DURABILITY_TOOLTIP.on && damaged;
	}
}
