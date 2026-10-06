package com.boaringpanda.vsbetterbuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterbuilding.block.MixedSlabs;
import com.boaringpanda.vsbetterbuilding.block.StackedHeads;

/**
 * Middle-click on a mixed slab block picks the slab under the cursor ({@link MixedSlabs#targeted}), and on a stack of heads the head
 * under the cursor ({@link StackedHeads}).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@WrapOperation(
			method = "handlePickItemFromBlock",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack vsbetterbuilding$pickTargetedPart(BlockState state, LevelReader level, BlockPos pos, boolean includeData,
			Operation<ItemStack> original) {
		if (StackedHeads.aimsAtTop(player, level, pos, state)) {
			return StackedHeads.top(level, pos, state).toItem(includeData);
		}
		return original.call(MixedSlabs.is(state) ? MixedSlabs.targeted(level, pos, player) : state, level, pos, includeData);
	}

	/**
	 * Ctrl + middle-click copies a block's data onto the picked item. A mixed block's data (both slabs) mustn't go onto a plain slab: it
	 * would overwrite the slabs of the next mixed block that slab makes. Likewise the top head of a stack mustn't go onto the item of
	 * either head, so a stack gives only the aimed head's skin, sound and name (the top head's are already on its item).
	 */
	@WrapOperation(
			method = "handlePickItemFromBlock",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;addBlockDataToItem(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
	private void vsbetterbuilding$onlyTargetedData(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, Operation<Void> original) {
		if (StackedHeads.isStacked(state)) {
			if (!StackedHeads.aimsAtTop(player, level, pos, state) && level.getBlockEntity(pos) instanceof BlockEntity bottom) {
				stack.applyComponents(bottom.collectComponents());
			}
		} else if (!MixedSlabs.is(state)) {
			original.call(state, level, pos, stack);
		}
	}
}
