package com.boaringpanda.vsbetterqol.mixin;

import java.util.function.Consumer;

import com.boaringpanda.vsbetterqol.DurabilityWarning;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

// Warns a player when their item is about to break (DurabilityWarning). applyDamage is where both hurtAndBreak and
// hurtWithoutBreaking set the new damage, after Unbreaking; creative never reaches it.
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "applyDamage", at = @At("HEAD"))
	private void vsbetterqol$warnBeforeBreaking(int newDamage, @Nullable ServerPlayer player, Consumer<ItemStack> onBreak, CallbackInfo ci) {
		if (player != null) {
			DurabilityWarning.onDamage((ItemStack) (Object) this, player, newDamage);
		}
	}
}
