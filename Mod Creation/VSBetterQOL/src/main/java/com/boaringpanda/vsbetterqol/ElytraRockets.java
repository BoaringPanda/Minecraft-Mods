package com.boaringpanda.vsbetterqol;

import java.util.function.Consumer;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

// Rockets with an elytra on: one starts the glide from the ground or mid-air without jumping first (mixin/FireworkRocketItemMixin), and one
// boosts a swimming player the way they look (mixin/FireworkRocketEntityMixin). The server has to have the mod too.
public final class ElytraRockets {
	// Client: ticks left to start gliding after a rocket hop from the ground (counted down by VSBetterQOLClient).
	public static int clientLaunchTicks;
	// Client: starts gliding the way vanilla's jump key does. Set by VSBetterQOLClient, which can send the packet.
	public static Consumer<Player> clientStartGliding = player -> {
	};

	private ElytraRockets() {
	}

	// Vanilla's glide rules (Player.tryToStartFallFlying), minus "not on the ground".
	public static boolean canLaunch(Player player) {
		return !player.isFallFlying() && !player.getAbilities().flying && !player.isInLiquid() && !player.isPassenger()
				&& !player.hasEffect(MobEffects.LEVITATION) && wearsGlider(player) && enabled(player);
	}

	public static boolean canSwimBoost(LivingEntity entity) {
		return entity instanceof Player player && player.isSwimming() && wearsGlider(player) && enabled(player);
	}

	// An elytra (or anything with the glider component) that won't break on the next point of damage, like vanilla's canGlide.
	private static boolean wearsGlider(Player player) {
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (LivingEntity.canGlideUsing(player.getItemBySlot(slot), slot)) {
				return true;
			}
		}
		return false;
	}

	// On a server without the mod the rocket would be refused, so the client stays vanilla there.
	private static boolean enabled(Player player) {
		return !player.level().isClientSide() || ServerConfig.clientServerHasMod();
	}
}
