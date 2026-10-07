package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

// For BlockEntityMixin's "keep brewing after loading from an item" fix.
@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandBlockEntityAccessor {
	@Accessor("brewTime")
	int vsbetterqol$getBrewTime();

	@Accessor("ingredient")
	void vsbetterqol$setIngredient(Item ingredient);
}
