package com.boaringpanda.lumberjackmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.level.Level;

import com.boaringpanda.lumberjackmod.WoodcutterVillager;

// A trade set only picks from one pool, so the woodcutter's always-there trades are a second set per level
// (lumberjackmod:woodcutter/level_<n>_guaranteed), added the way the wandering trader adds several sets. A level without one is
// skipped by vanilla. Runs before updateSpecialPrices so reputation discounts cover these trades too.
@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager {
	private VillagerMixin(EntityType<? extends AbstractVillager> type, Level level) {
		super(type, level);
	}

	@Inject(
			method = "updateTrades",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/npc/villager/Villager;addOffersFromTradeSet(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/trading/MerchantOffers;Lnet/minecraft/resources/ResourceKey;)V",
					shift = At.Shift.AFTER))
	private void lumberjackmod$addGuaranteedTrades(ServerLevel level, CallbackInfo ci) {
		VillagerData data = ((Villager) (Object) this).getVillagerData();
		if (data.profession().is(WoodcutterVillager.PROFESSION)) {
			this.addOffersFromTradeSet(level, this.getOffers(), WoodcutterVillager.guaranteedTradeSet(data.level()));
		}
	}
}
