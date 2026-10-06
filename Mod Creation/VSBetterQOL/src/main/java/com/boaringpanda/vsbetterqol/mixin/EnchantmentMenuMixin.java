package com.boaringpanda.vsbetterqol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;

import com.boaringpanda.vsbetterqol.EnchantingLapis;

// The lapis slot is filled from the table when the menu opens and emptied back into it when it closes (see EnchantingLapis). Server only:
// the client's menu has ContainerLevelAccess.NULL, so access.execute never runs there.
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
	@Unique
	private static final int LAPIS_SLOT = 1;

	@Shadow
	@Final
	private Container enchantSlots;

	@Inject(
			method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("TAIL"))
	private void vsbetterqol$takeLapis(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		access.execute((level, pos) -> {
			if (level.getBlockEntity(pos) instanceof EnchantingTableBlockEntity table) {
				this.enchantSlots.setItem(LAPIS_SLOT, EnchantingLapis.take(table));
			}
		});
	}

	// Before vanilla hands the slots back to the player. Lapis that doesn't fit (or a table that's gone) goes to the player like vanilla.
	@WrapOperation(
			method = "lambda$removed$0",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/inventory/EnchantmentMenu;clearContainer(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/Container;)V"))
	private void vsbetterqol$storeLapis(EnchantmentMenu menu, Player player, Container container, Operation<Void> original,
			@Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof EnchantingTableBlockEntity table) {
			EnchantingLapis.store(table, container.getItem(LAPIS_SLOT));
		}
		original.call(menu, player, container);
	}
}
