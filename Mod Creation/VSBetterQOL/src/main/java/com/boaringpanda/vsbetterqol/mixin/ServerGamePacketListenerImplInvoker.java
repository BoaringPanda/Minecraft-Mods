package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// Vanilla's creative Ctrl+pick-block copy of a block entity onto its item, reused for picked-up containers (see PickingUp).
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplInvoker {
	@Invoker("addBlockDataToItem")
	static void vsbetterqol$addBlockDataToItem(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack) {
		throw new AssertionError();
	}
}
