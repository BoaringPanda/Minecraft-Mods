package com.boaringpanda.vsbetterqol.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;

import com.boaringpanda.vsbetterqol.EnchantingLapis;

// The table's stored lapis goes onto its item as container contents (shown in the tooltip) when it's picked up, and back into the table
// when that item is placed. The lapis attachment isn't part of the block entity data vanilla copies onto items, hence these two.
@Mixin(EnchantingTableBlockEntity.class)
public abstract class EnchantingTableBlockEntityMixin {
	@Inject(method = "collectImplicitComponents", at = @At("TAIL"))
	private void vsbetterqol$lapisToItem(DataComponentMap.Builder components, CallbackInfo ci) {
		ItemStack lapis = EnchantingLapis.stored((EnchantingTableBlockEntity) (Object) this);
		if (!lapis.isEmpty()) {
			components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(lapis)));
		}
	}

	@Inject(method = "applyImplicitComponents", at = @At("TAIL"))
	private void vsbetterqol$lapisFromItem(DataComponentGetter components, CallbackInfo ci) {
		ItemContainerContents contents = components.get(DataComponents.CONTAINER);
		if (contents != null) {
			contents.nonEmptyItemCopyStream().forEach(lapis -> EnchantingLapis.store((EnchantingTableBlockEntity) (Object) this, lapis));
		}
	}
}
