package com.boaringpanda.bettervanillaqol.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bettervanillaqol.BetterVanillaQOL;

@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {
	// Farmland with one of these growing on it isn't trampled back to dirt by a landing entity, so the crop doesn't break.
	// Includes #minecraft:crops, so crops from other mods that use that tag are protected too.
	private static final TagKey<Block> PROTECTS_FARMLAND = TagKey.create(Registries.BLOCK, BetterVanillaQOL.id("protects_farmland"));

	// Only the trampling is skipped: the rest of fallOn (fall damage) still runs, and empty farmland still tramples.
	@WrapWithCondition(
			method = "fallOn",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToBaseBlock(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
	private boolean bettervanillaqol$keepCrops(FarmlandBlock farmland, Entity entity, BlockState state, Level level, BlockPos pos) {
		return !level.getBlockState(pos.above()).is(PROTECTS_FARMLAND);
	}
}
