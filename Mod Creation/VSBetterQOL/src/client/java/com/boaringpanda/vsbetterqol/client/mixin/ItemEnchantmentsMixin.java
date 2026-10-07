package com.boaringpanda.vsbetterqol.client.mixin;

import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

// Enchanted books show what each enchantment does while Shift is held, under its name. Otherwise one "Hold Shift for info" line.
// Text is lang key enchantment.<namespace>.<id>.desc, so other mods' enchantments without one show nothing. Gear stays vanilla.
@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsMixin {
	@Unique
	private static final int VSBETTERQOL$WRAP_WIDTH = 200;

	// Both of vanilla's loops (tooltip order, then the rest) add each name with this call.
	@WrapOperation(
			method = "addToTooltip",
			at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
	private void vsbetterqol$addDescription(Consumer<Component> consumer, Object name, Operation<Void> original,
			@Local Holder<Enchantment> enchantment, @Local(argsOnly = true) DataComponentGetter components) {
		original.call(consumer, name);
		String key = vsbetterqol$descriptionKey(enchantment);
		if (key == null || !this.vsbetterqol$isBook(components) || !Minecraft.getInstance().hasShiftDown()) {
			return;
		}
		for (FormattedText line : Minecraft.getInstance().font.getSplitter()
				.splitLines(Component.translatable(key), VSBETTERQOL$WRAP_WIDTH, Style.EMPTY)) {
			consumer.accept(Component.literal(" " + line.getString()).withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	@Inject(method = "addToTooltip", at = @At("TAIL"))
	private void vsbetterqol$addShiftHint(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag,
			DataComponentGetter components, CallbackInfo ci) {
		if (this.vsbetterqol$isBook(components) && !Minecraft.getInstance().hasShiftDown()
				&& ((ItemEnchantments) (Object) this).keySet().stream().anyMatch(e -> vsbetterqol$descriptionKey(e) != null)) {
			consumer.accept(Component.translatable("tooltip.vsbetterqol.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	// Stored enchantments are the book's; a sword's ENCHANTMENTS component is a different object.
	@Unique
	private boolean vsbetterqol$isBook(DataComponentGetter components) {
		return components.get(DataComponents.STORED_ENCHANTMENTS) == (Object) this;
	}

	@Unique
	private static @Nullable String vsbetterqol$descriptionKey(Holder<Enchantment> enchantment) {
		return enchantment.unwrapKey()
				.map(key -> Util.makeDescriptionId("enchantment", key.identifier()) + ".desc")
				.filter(Language.getInstance()::has)
				.orElse(null);
	}
}
