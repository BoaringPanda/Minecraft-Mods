package com.boaringpanda.vsbetterqol.mixin;

import com.boaringpanda.vsbetterqol.ElytraRockets;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.level.Level;

// Vanilla only fires a rocket in the air while gliding. With an elytra on it also fires on the ground or while falling (and starts the
// glide), and while swimming (ElytraRockets). Vanilla's own branch then fires it, uses one up and counts the stat.
@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin {
	// How long the client waits for the hop to leave the ground before giving up on the glide.
	@Unique
	private static final int LAUNCH_TICKS = 10;

	@ModifyExpressionValue(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isFallFlying()Z"))
	private boolean vsbetterqol$launch(boolean fallFlying, Level level, Player player, InteractionHand hand) {
		if (fallFlying || ElytraRockets.canSwimBoost(player)) {
			return true;
		}
		if (!ElytraRockets.canLaunch(player)) {
			return false;
		}
		// The client moves the player: a hop, then the glide starts once it's in the air (VSBetterQOLClient). In the air it starts now,
		// and that packet goes out before this use packet, so the server is already gliding when it fires the rocket.
		if (level.isClientSide()) {
			if (player.onGround()) {
				player.jumpFromGround();
				ElytraRockets.clientLaunchTicks = LAUNCH_TICKS;
			} else {
				ElytraRockets.clientStartGliding.accept(player);
			}
		}
		return true;
	}
}
