package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;

// The villager behind a trading screen, for the re-roll button (VillagerReroll).
@Mixin(MerchantMenu.class)
public interface MerchantMenuAccessor {
	@Accessor("trader")
	Merchant vsbetterqol$getTrader();
}
