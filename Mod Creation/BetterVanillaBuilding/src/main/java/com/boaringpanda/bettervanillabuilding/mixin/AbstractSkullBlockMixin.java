package com.boaringpanda.bettervanillabuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bettervanillabuilding.block.StackedHeads;

/**
 * Vanilla only ticks the powered mouth animation of a piglin or dragon head. A piglin or dragon head on top of another head
 * ({@link StackedHeads}) animates too, so the client ticks the stack whatever the bottom head is.
 */
@Mixin(AbstractSkullBlock.class)
public class AbstractSkullBlockMixin {
	@ModifyReturnValue(method = "getTicker", at = @At("RETURN"))
	private <T extends BlockEntity> @Nullable BlockEntityTicker<T> bettervanillabuilding$animateTopHead(@Nullable BlockEntityTicker<T> ticker,
			Level level, BlockState state, BlockEntityType<T> type) {
		if (ticker == null && level.isClientSide() && type == BlockEntityTypes.SKULL && state.hasProperty(StackedHeads.TOP)
				&& (state.getValue(StackedHeads.TOP) == StackedHeads.Top.PIGLIN || state.getValue(StackedHeads.TOP) == StackedHeads.Top.DRAGON)) {
			return (tickLevel, pos, tickState, entity) -> SkullBlockEntity.animation(tickLevel, pos, tickState, (SkullBlockEntity) entity);
		}
		return ticker;
	}
}
