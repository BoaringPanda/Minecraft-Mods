package com.boaringpanda.vsbetterqol.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterqol.AnvilDurability;
import com.boaringpanda.vsbetterqol.BrokenAnvils;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu implements AnvilDurability.View {
	// Highest price vanilla allows for a rename (the onlyRenaming cap in createResult).
	@Unique
	private static final long MAX_RENAME_COST = 39;

	@Shadow
	private int repairItemCountCost;

	@Shadow
	public abstract int getCost();

	@Unique
	private DataSlot vsbetterqol$usesLeft;

	private AnvilMenuMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
		super(type, containerId, inventory, access, slots);
	}

	// The client's constructor calls this one too (with ContainerLevelAccess.NULL).
	@Inject(
			method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("TAIL"))
	private void vsbetterqol$addUsesLeftSlot(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		this.vsbetterqol$usesLeft = this.addDataSlot(AnvilDurability.usesLeftSlot(access));
	}

	@Override
	public int vsbetterqol$usesLeft() {
		return this.vsbetterqol$usesLeft.get();
	}

	// Vanilla damages the anvil when this roll is under 0.12. It's only reached outside creative on an anvil block, so every call is
	// one use: 0 (damage now) when the stage is used up, otherwise 1 (no damage). Vanilla does the block swap, breaking and sounds.
	@WrapOperation(
			method = "lambda$onTake$0",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"))
	private static float vsbetterqol$countUse(RandomSource random, Operation<Float> original, @Local(argsOnly = true) Level level,
			@Local(argsOnly = true) BlockPos pos) {
		return AnvilDurability.use(level, pos) ? 0.0F : 1.0F;
	}

	// Vanilla removes a damaged anvil whose last stage is used up (damage() returns null). It becomes a broken anvil instead, so
	// vanilla's setBlock branch runs. damage() itself is untouched, so falling anvils still break like vanilla.
	@WrapOperation(
			method = "lambda$onTake$0",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/AnvilBlock;damage(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private static BlockState vsbetterqol$breakIntoBrokenAnvil(BlockState state, Operation<BlockState> original) {
		BlockState damaged = original.call(state);
		if (damaged == null && state.hasProperty(AnvilBlock.FACING)) {
			return BrokenAnvils.BROKEN_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, state.getValue(AnvilBlock.FACING));
		}
		return damaged;
	}

	// The price before the Too Expensive check. Renaming a name tag is free. A material repair (ingots etc., repairItemCountCost > 0) is
	// free too, apart from a rename done at the same time, which costs what renaming alone would. In that branch price is the
	// repair count plus the naming cost, so the naming cost is what's left over.
	@ModifyExpressionValue(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(JJJ)J"))
	private long vsbetterqol$freeJobs(long price, @Local long tax) {
		if (this.inputSlots.getItem(0).is(Items.NAME_TAG) && this.inputSlots.getItem(1).isEmpty()) {
			return 0;
		}
		if (this.repairItemCountCost > 0) {
			long namingCost = price - tax - this.repairItemCountCost;
			return namingCost > 0 ? Math.min(tax + namingCost, MAX_RENAME_COST) : 0;
		}
		return price;
	}

	// Free repairs don't raise the item's prior-work cost, so repairing never makes gear Too Expensive later.
	@WrapOperation(
			method = "createResult",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;calculateIncreasedRepairCost(I)I"))
	private int vsbetterqol$keepRepairCost(int baseCost, Operation<Integer> original) {
		return this.repairItemCountCost > 0 ? baseCost : original.call(baseCost);
	}

	// Vanilla needs a cost above 0. A result with cost 0 only exists for the free jobs above.
	@ModifyReturnValue(method = "mayPickup", at = @At("RETURN"))
	private boolean vsbetterqol$allowFreeJobs(boolean original, Player player, boolean hasItem) {
		return original || (hasItem && this.getCost() == 0);
	}

	// A repair wears the anvil twice: one extra use here, then vanilla's roll counts the normal one (creative is skipped there too).
	@Inject(method = "onTake", at = @At("HEAD"))
	private void vsbetterqol$repairWearsTwice(Player player, ItemStack carried, CallbackInfo ci) {
		if (this.repairItemCountCost > 0 && !player.hasInfiniteMaterials()) {
			this.access.execute((level, pos) -> {
				if (level.getBlockState(pos).is(BlockTags.ANVIL)) {
					AnvilDurability.addWear(level, pos);
				}
			});
		}
	}

	// The setBlock branch plays the anvil-use sound. Turning broken plays vanilla's anvil-destroy sound instead.
	@WrapOperation(
			method = "lambda$onTake$0",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
	private static void vsbetterqol$brokenAnvilSound(Level level, int type, BlockPos pos, int data, Operation<Void> original) {
		if (type == LevelEvent.SOUND_ANVIL_USED && level.getBlockState(pos).is(BrokenAnvils.BROKEN_ANVIL)) {
			type = LevelEvent.SOUND_ANVIL_BROKEN;
		}
		original.call(level, type, pos, data);
	}
}
