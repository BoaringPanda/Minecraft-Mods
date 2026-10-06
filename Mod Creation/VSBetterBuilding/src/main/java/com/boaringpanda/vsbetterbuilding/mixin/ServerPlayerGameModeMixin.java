package com.boaringpanda.vsbetterbuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.vsbetterbuilding.block.CornerTorches;
import com.boaringpanda.vsbetterbuilding.block.LilyPadDecorations;
import com.boaringpanda.vsbetterbuilding.block.StackedHeads;

/**
 * A player breaking a decoration while aiming at the bare lily pad under it breaks the pad, and the decoration drops with it (as a
 * torch does when the block under it goes). Otherwise the decoration comes off and the pad stays ({@code LevelMixin}).
 *
 * <p>On a stack of heads ({@link StackedHeads}), a player breaks only the head they aim at. The other head stays where it is. Likewise
 * only the aimed torch of a group of corner torches comes off ({@link CornerTorches}).
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@Shadow
	@Final
	protected ServerPlayer player;

	@WrapOperation(method = "destroyBlock",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
	private boolean vsbetterbuilding$breakPad(ServerLevel level, BlockPos pos, boolean movedByPiston, Operation<Boolean> original,
			@Share("padAimed") LocalBooleanRef padAimed, @Share("brokenHead") LocalRef<StackedHeads.@Nullable Head> brokenHead,
			@Share("brokenTorch") LocalRef<@Nullable BlockState> brokenTorch) {
		BlockState state = level.getBlockState(pos);
		if (LilyPadDecorations.aimsAtPad(player, level, pos, state)) {
			padAimed.set(true);
			return level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), Block.UPDATE_ALL);
		}
		if (StackedHeads.isStacked(state)) {
			// Only the aimed head comes off; the other head (and the pad) stays.
			brokenHead.set(StackedHeads.breakHead(level, pos, state, StackedHeads.aimsAtTop(player, level, pos, state), Block.UPDATE_ALL));
			return brokenHead.get() != null;
		}
		BlockState torch = CornerTorches.aimedTorch(player, level, pos, state);
		if (torch != null) {
			// Only the aimed torch comes off; the rest of the group stays.
			brokenTorch.set(torch);
			return CornerTorches.breakTorch(level, pos, state, torch, Block.UPDATE_ALL);
		}
		return original.call(level, pos, movedByPiston);
	}

	/**
	 * Breaking one head of a stack drops that head alone, by its own loot table, and counts as mining that head. Breaking one torch of
	 * a group drops that torch alone (its loot table counts one torch).
	 */
	@WrapOperation(method = "destroyBlock",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V"))
	private void vsbetterbuilding$dropBrokenHead(Block block, ServerLevel level, ServerPlayer breaker, BlockPos pos, BlockState state,
			@Nullable BlockEntity entity, ItemStack tool, Operation<Void> original,
			@Share("brokenHead") LocalRef<StackedHeads.@Nullable Head> brokenHead,
			@Share("brokenTorch") LocalRef<@Nullable BlockState> brokenTorch) {
		StackedHeads.Head head = brokenHead.get();
		if (head == null) {
			BlockState torch = brokenTorch.get();
			original.call(block, level, breaker, pos, torch != null ? torch : state, entity, tool);
			return;
		}
		BlockState headState = head.state();
		original.call(headState.getBlock(), level, breaker, pos, headState, StackedHeads.tempEntity(pos, head), tool);
	}

	/** A decoration knocked off by its pad breaking drops whatever the tool, as when its support goes (a lantern without a pickaxe). */
	@ModifyExpressionValue(method = "destroyBlock",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerPlayer;hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean vsbetterbuilding$padDropsAnyway(boolean correctTool, @Share("padAimed") LocalBooleanRef padAimed) {
		return correctTool || padAimed.get();
	}
}
