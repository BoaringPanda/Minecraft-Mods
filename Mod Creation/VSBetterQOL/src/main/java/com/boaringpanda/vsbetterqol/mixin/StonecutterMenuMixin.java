package com.boaringpanda.vsbetterqol.mixin;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import com.boaringpanda.vsbetterqol.StonecutterMenuStorage;
import com.boaringpanda.vsbetterqol.StonecutterStorage;

// The stonecutter's 9 storage slots (StonecutterStorage), between the recipes and the inventory. They're added after vanilla's slots
// (38-46), so vanilla's own slot numbers stay the same, and the inventory moves down a row to make room (client/mixin/StonecutterScreenMixin
// draws it). While crafting, an emptied input refills from storage with the same block, so the picked recipe stays up.
@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin extends AbstractContainerMenu implements StonecutterMenuStorage {
	@Unique
	private static final int STORAGE_START = 38;
	@Unique
	private static final int STORAGE_END = STORAGE_START + StonecutterStorage.SIZE;
	@Unique
	private static final int INVENTORY_START = 2;
	@Unique
	private static final int STORAGE_Y = 75;
	// How far the player's inventory moves down: one slot row plus a little gap.
	@Unique
	private static final int SHIFT = 22;

	@Shadow
	@Final
	private Level level;

	@Shadow
	@Final
	private Slot inputSlot;

	// The input item before the latest change: what the input refills with.
	@Shadow
	private ItemStack input;

	// Null when the storage is off for this player (vanilla menu).
	@Unique
	private @Nullable SimpleContainer vsbetterqol$storage;

	@Unique
	private @Nullable BlockPos vsbetterqol$pos;

	@Unique
	private boolean vsbetterqol$crafting;

	protected StonecutterMenuMixin(MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	@ModifyArg(
			method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/inventory/StonecutterMenu;addStandardInventorySlots(Lnet/minecraft/world/Container;II)V"),
			index = 2)
	private int vsbetterqol$moveInventoryDown(int y, @Local(argsOnly = true) Inventory inventory) {
		return StonecutterStorage.enabledFor(inventory.player) ? y + SHIFT : y;
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void vsbetterqol$addStorage(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		// Server only (the client's access is NULL). Every stonecutter menu, so the one-player-at-a-time check also covers players without the mod.
		access.execute((level, pos) -> this.vsbetterqol$pos = pos.immutable());
		if (!StonecutterStorage.enabledFor(inventory.player)) {
			return;
		}
		SimpleContainer storage = new SimpleContainer(StonecutterStorage.SIZE);
		this.vsbetterqol$storage = storage;
		for (int i = 0; i < StonecutterStorage.SIZE; i++) {
			this.addSlot(new StonecutterStorage.StorageSlot(storage, i, 8 + i * 18, STORAGE_Y, this.level));
		}
		access.execute((level, pos) -> StonecutterStorage.takeInto(level, pos, storage));
	}

	// Runs inside the result slot's "take one from the input" (StonecutterResultSlotMixin), before vanilla looks at the input: if that
	// emptied it, top it up from storage with the same block, so vanilla sees no change and keeps the recipe selected. Setting the input
	// runs this again, which then does nothing.
	@Inject(method = "slotsChanged", at = @At("HEAD"))
	private void vsbetterqol$refillInput(Container container, CallbackInfo ci) {
		SimpleContainer storage = this.vsbetterqol$storage;
		if (storage == null || !this.vsbetterqol$crafting || this.inputSlot.hasItem() || this.input.isEmpty()) {
			return;
		}
		ItemStack refill = ItemStack.EMPTY;
		for (int i = 0; i < StonecutterStorage.SIZE && refill.getCount() < this.input.getMaxStackSize(); i++) {
			ItemStack stored = storage.getItem(i);
			if (ItemStack.isSameItemSameComponents(stored, this.input)) {
				ItemStack taken = stored.split(this.input.getMaxStackSize() - refill.getCount());
				if (refill.isEmpty()) {
					refill = taken;
				} else {
					refill.grow(taken.getCount());
				}
			}
		}
		if (!refill.isEmpty()) {
			storage.setChanged();
			this.inputSlot.set(refill);
		}
	}

	// Shift-clicking a storage slot moves it into the inventory (hotbar first, like taking from a chest).
	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void vsbetterqol$quickMoveFromStorage(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		if (this.vsbetterqol$storage == null || slotIndex < STORAGE_START) {
			return;
		}
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) {
			cir.setReturnValue(ItemStack.EMPTY);
			return;
		}
		ItemStack stack = slot.getItem();
		ItemStack clicked = stack.copy();
		if (!this.moveItemStackTo(stack, INVENTORY_START, STORAGE_START, true)) {
			cir.setReturnValue(ItemStack.EMPTY);
			return;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		}
		slot.setChanged();
		slot.onTake(player, stack);
		cir.setReturnValue(clicked);
	}

	// Shift-clicking a cuttable block in the inventory fills the input like vanilla, and whatever the input can't take goes into storage.
	@WrapOperation(
			method = "quickMoveStack",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/inventory/StonecutterMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z",
					ordinal = 2))
	private boolean vsbetterqol$quickMoveIntoStorage(StonecutterMenu menu, ItemStack stack, int start, int end, boolean backwards,
			Operation<Boolean> original) {
		boolean moved = original.call(menu, stack, start, end, backwards);
		if (this.vsbetterqol$storage != null && !stack.isEmpty()) {
			moved |= this.moveItemStackTo(stack, STORAGE_START, STORAGE_END, false);
		}
		return moved;
	}

	// Back into the stonecutter on close, or to the player if it's gone (like vanilla does with the input).
	@Inject(method = "removed", at = @At("TAIL"))
	private void vsbetterqol$saveStorage(Player player, CallbackInfo ci) {
		SimpleContainer storage = this.vsbetterqol$storage;
		BlockPos pos = this.vsbetterqol$pos;
		if (storage == null || pos == null) {
			return;
		}
		if (this.level.getBlockState(pos).is(Blocks.STONECUTTER)) {
			StonecutterStorage.store(this.level, pos, storage);
		} else {
			this.clearContainer(player, storage);
		}
	}

	@Override
	public @Nullable BlockPos vsbetterqol$pos() {
		return this.vsbetterqol$pos;
	}

	@Override
	public void vsbetterqol$setCrafting(boolean crafting) {
		this.vsbetterqol$crafting = crafting;
	}
}
