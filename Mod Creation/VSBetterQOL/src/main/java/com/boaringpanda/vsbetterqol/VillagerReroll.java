package com.boaringpanda.vsbetterqol;

import java.util.Map;
import java.util.WeakHashMap;

import com.boaringpanda.vsbetterqol.mixin.MerchantMenuAccessor;
import com.boaringpanda.vsbetterqol.mixin.VillagerInvoker;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;

// The ⟳ button on the villager trading screen (client/mixin/MerchantScreenMixin) re-rolls that villager's trades, at most once every
// 3 seconds per villager. Fair like breaking and replacing its workstation: only a Novice villager nobody has traded with. The server
// checks everything again and makes the new trades with vanilla's own code.
public final class VillagerReroll {
	public static final int COOLDOWN_TICKS = 60;
	private static final Map<Villager, Long> LAST_REROLL = new WeakHashMap<>();

	public record RerollPayload() implements CustomPacketPayload {
		public static final RerollPayload INSTANCE = new RerollPayload();
		public static final Type<RerollPayload> TYPE = new Type<>(VSBetterQOL.id("reroll_trades"));
		public static final StreamCodec<ByteBuf, RerollPayload> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<RerollPayload> type() {
			return TYPE;
		}
	}

	private VillagerReroll() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(RerollPayload.TYPE, RerollPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RerollPayload.TYPE, (payload, context) -> reroll(context.player()));
	}

	// Vanilla's rule for a villager losing its job, and with it its trades (ResetProfession): Novice and never traded with.
	public static boolean canReroll(int level, int xp) {
		return level <= 1 && xp == 0;
	}

	private static void reroll(ServerPlayer player) {
		if (!(player.containerMenu instanceof MerchantMenu menu) || !(((MerchantMenuAccessor) menu).vsbetterqol$getTrader() instanceof Villager villager)
				|| villager.getTradingPlayer() != player || !canReroll(villager.getVillagerData().level(), villager.getVillagerXp())) {
			return;
		}
		long now = player.level().getGameTime();
		Long last = LAST_REROLL.get(villager);
		if (last != null && now - last < COOLDOWN_TICKS) {
			return;
		}
		LAST_REROLL.put(villager, now);
		// Vanilla's trade generation into an empty list (it also re-applies this player's special prices), then the same update vanilla
		// sends when prices change.
		villager.setOffers(new MerchantOffers());
		((VillagerInvoker) villager).vsbetterqol$updateTrades(player.level());
		menu.updateSellItem();
		player.sendMerchantOffers(menu.containerId, villager.getOffers(), villager.getVillagerData().level(), villager.getVillagerXp(),
				villager.showProgressBar(), villager.canRestock());
		villager.playSound(SoundEvents.VILLAGER_YES);
	}
}
