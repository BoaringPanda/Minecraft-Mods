package com.boaringpanda.bettervanillabuilding.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;
import com.boaringpanda.bettervanillabuilding.entity.RopeCollision;

/**
 * Makes tied ropes solid to moving entities, and has mobs jump the ropes they walk into (see {@link RopeCollision}). Also makes walking
 * and sprinting on a mixed slab block sound and look like its top slab ({@link MixedSlabs}).
 */
@Mixin(Entity.class)
public class EntityMixin {
	@WrapOperation(
			method = "collide",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
	private List<VoxelShape> bettervanillabuilding$addRopes(Level level, Entity source, AABB area, Operation<List<VoxelShape>> original) {
		return RopeCollision.withRopes(source, area, original.call(level, source, area));
	}

	@Inject(method = "move", at = @At("TAIL"))
	private void bettervanillabuilding$hopRopes(MoverType moverType, Vec3 delta, CallbackInfo ci) {
		RopeCollision.hopIfBlockedByRope((Entity) (Object) this);
	}

	/**
	 * Every step sound starts here, before any mob's own {@code playStepSound} (and the player's underwater and carpet-on-top sounds),
	 * so they all get the top slab of a mixed block.
	 */
	@ModifyVariable(method = "walkingStepSound", at = @At("HEAD"), argsOnly = true)
	private BlockState bettervanillabuilding$stepOnTopSlab(BlockState onState, @Local(argsOnly = true) BlockPos onPos) {
		return MixedSlabs.surface(((Entity) (Object) this).level(), onPos, onState);
	}

	@WrapOperation(
			method = "spawnSprintParticle",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState bettervanillabuilding$sprintOnTopSlab(Level level, BlockPos pos, Operation<BlockState> original) {
		return MixedSlabs.surface(level, pos, original.call(level, pos));
	}
}
