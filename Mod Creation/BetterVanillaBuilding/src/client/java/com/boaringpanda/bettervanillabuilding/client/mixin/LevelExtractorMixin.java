package com.boaringpanda.bettervanillabuilding.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bettervanillabuilding.block.LilyPadDecorations;
import com.boaringpanda.bettervanillabuilding.block.StackedHeads;

/**
 * On a decoration on a lily pad, the block outline shows only the part under the cursor, the one a break would take: the bare pad or
 * the decoration ({@link LilyPadDecorations#partShape}). On a stack of heads, it shows only the head under the cursor
 * ({@link StackedHeads#partShape}).
 */
@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
	@WrapOperation(method = "extractBlockOutline",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
	private VoxelShape bettervanillabuilding$aimedPart(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context,
			Operation<VoxelShape> original) {
		Player player = Minecraft.getInstance().player;
		boolean pad = LilyPadDecorations.aimsAtPad(player, level, pos, state);
		if (!pad && StackedHeads.isStacked(state)) {
			return StackedHeads.partShape(state, level, pos, context, StackedHeads.aimsAtTop(player, level, pos, state));
		}
		if (!LilyPadDecorations.onPad(state)) {
			return original.call(state, level, pos, context);
		}
		return LilyPadDecorations.partShape(state, level, pos, context, pad);
	}
}
