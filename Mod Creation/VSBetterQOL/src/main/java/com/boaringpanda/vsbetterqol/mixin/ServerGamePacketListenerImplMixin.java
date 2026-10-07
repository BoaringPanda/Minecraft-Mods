package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import com.boaringpanda.vsbetterqol.Carrying;

// The hotbar is locked while carrying a container. Players with the mod can't change slot at all (client/mixin/MouseHandlerMixin and the
// hotbar keys in VSBetterQOLClient); a player without it is put back on their slot.
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	// After vanilla's hop from the network thread to the server thread.
	@Inject(
			method = "handleSetCarriedItem",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
					shift = At.Shift.AFTER),
			cancellable = true)
	private void vsbetterqol$hotbarLockedWhileCarrying(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
		if (Carrying.isCarrying(this.player)) {
			this.player.connection.send(new ClientboundSetHeldSlotPacket(this.player.getInventory().getSelectedSlot()));
			ci.cancel();
		}
	}
}
