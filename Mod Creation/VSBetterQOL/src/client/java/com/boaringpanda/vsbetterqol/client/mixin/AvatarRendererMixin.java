package com.boaringpanda.vsbetterqol.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.boaringpanda.vsbetterqol.Carrying;
import com.boaringpanda.vsbetterqol.client.CarriedBlockLayer;

// Puts the container a player is carrying into their render state for CarriedBlockLayer, and hides whatever is in their hands (they're full).
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"))
	private void vsbetterqol$extractCarried(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		ItemStack stack = entity instanceof Player player ? Carrying.carried(player) : ItemStack.EMPTY;
		ItemStackRenderState carried = CarriedBlockLayer.carried(state);
		if (stack.isEmpty()) {
			carried.clear();
			return;
		}
		Minecraft.getInstance().getItemModelResolver().updateForLiving(carried, stack, ItemDisplayContext.NONE, entity);
		state.rightHandItemState.clear();
		state.leftHandItemState.clear();
	}
}
