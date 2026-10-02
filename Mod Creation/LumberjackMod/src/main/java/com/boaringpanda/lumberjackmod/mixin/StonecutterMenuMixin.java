package com.boaringpanda.lumberjackmod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import com.boaringpanda.lumberjackmod.Woodcutter;
import com.boaringpanda.lumberjackmod.WoodcutterMenu;

// The woodcutter and the stonecutter share vanilla's stonecutting recipes; this splits them by input item. The woodcutter only takes
// #lumberjackmod:woodcutter_inputs (logs, planks, bamboo blocks) and the stonecutter takes everything else. Runs on both sides (the
// client works out the recipe list itself), and item tags are synced, so both agree.
@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin {
	// The recipe list shown for the item in the input slot.
	@WrapOperation(
			method = "setupRecipeList",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/crafting/SelectableRecipe$SingleInputSet;selectByInput(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/crafting/SelectableRecipe$SingleInputSet;"))
	private SelectableRecipe.SingleInputSet<StonecutterRecipe> lumberjackmod$splitRecipes(
			SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes, ItemStack input, Operation<SelectableRecipe.SingleInputSet<StonecutterRecipe>> original) {
		return this.lumberjackmod$takes(input) ? original.call(recipes, input) : SelectableRecipe.SingleInputSet.empty();
	}

	// Shift-clicking an item from the inventory into the input slot.
	@WrapOperation(
			method = "quickMoveStack",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/crafting/SelectableRecipe$SingleInputSet;acceptsInput(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean lumberjackmod$splitInputs(
			SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes, ItemStack input, Operation<Boolean> original) {
		return this.lumberjackmod$takes(input) && original.call(recipes, input);
	}

	private boolean lumberjackmod$takes(ItemStack input) {
		return input.is(Woodcutter.INPUTS) == ((Object) this instanceof WoodcutterMenu);
	}
}
