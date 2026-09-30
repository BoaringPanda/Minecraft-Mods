package com.boaringpanda.bettervanillaqol.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bettervanillaqol.BetterVanillaQOL;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	// Grass, ferns, flowers and other small plants a sword swing goes straight through.
	private static final TagKey<Block> SWORDS_HIT_THROUGH = TagKey.create(Registries.BLOCK, BetterVanillaQOL.id("swords_hit_through"));

	@Shadow
	private static HitResult filterHitResult(HitResult hitResult, Vec3 from, double maxRange) {
		throw new AssertionError();
	}

	// Vanilla's crosshair pick first finds the nearest block, then only looks for entities closer than it, so a plant in the way
	// hides the mob behind it. With a sword the block ray skips those plants, so vanilla's own entity search reaches the mob.
	// The normal block hit is kept for the check below.
	@WrapOperation(
			method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
	private static HitResult bettervanillaqol$swordsSkipPlants(
			Entity cameraEntity, double range, float a, boolean withLiquids, Operation<HitResult> original,
			@Share("plantHit") LocalRef<HitResult> plantHit) {
		HitResult hit = original.call(cameraEntity, range, a, withLiquids);
		if (!(cameraEntity instanceof Player player) || !player.getMainHandItem().is(ItemTags.SWORDS)) {
			return hit;
		}

		plantHit.set(hit);
		Vec3 from = cameraEntity.getEyePosition(a);
		Vec3 to = from.add(cameraEntity.getViewVector(a).scale(range));
		return cameraEntity.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cameraEntity) {
			@Override
			public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
				return state.is(SWORDS_HIT_THROUGH) ? Shapes.empty() : super.getBlockShape(state, level, pos);
			}
		});
	}

	// No mob behind the plants: target the plant like vanilla, so the outline doesn't jump to the block behind it.
	@ModifyReturnValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At("RETURN"))
	private static HitResult bettervanillaqol$keepPlantTarget(
			HitResult hit, Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float partialTicks,
			@Share("plantHit") LocalRef<HitResult> plantHit) {
		if (hit instanceof EntityHitResult || plantHit.get() == null) {
			return hit;
		}
		return filterHitResult(plantHit.get(), cameraEntity.getEyePosition(partialTicks), blockInteractionRange);
	}
}
