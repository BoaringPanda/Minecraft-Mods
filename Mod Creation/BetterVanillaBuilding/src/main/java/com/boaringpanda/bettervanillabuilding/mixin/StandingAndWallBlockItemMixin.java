package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bettervanillabuilding.block.CornerTorches;

/**
 * A crouch-placed torch goes where the cursor is ({@link CornerTorches#place}). Both the standing and the wall torch come out of this
 * one method, so it also keeps a click meant for a group's free quarter from turning into a wall torch.
 */
@Mixin(StandingAndWallBlockItem.class)
public class StandingAndWallBlockItemMixin {
	@ModifyReturnValue(method = "getPlacementState", at = @At("RETURN"))
	private @Nullable BlockState bettervanillabuilding$torchWhereAimed(@Nullable BlockState state, BlockPlaceContext context) {
		return CornerTorches.place(context, state);
	}
}
