package com.boaringpanda.vsbetterbuilding.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.boaringpanda.vsbetterbuilding.block.CornerTorches;

/** Each torch of a group ({@link CornerTorches}) has its own smoke and flame, where vanilla's one torch would have them. */
@Mixin({TorchBlock.class, WallTorchBlock.class})
public class TorchParticlesMixin {
	@WrapOperation(method = "animateTick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private void vsbetterbuilding$particlePerTorch(Level level, ParticleOptions particle, double x, double y, double z, double xd,
			double yd, double zd, Operation<Void> original, @Local(argsOnly = true) BlockState state) {
		List<Vec3> offsets = CornerTorches.offsets(state);
		if (offsets.isEmpty()) {
			original.call(level, particle, x, y, z, xd, yd, zd);
			return;
		}
		for (Vec3 offset : offsets) {
			original.call(level, particle, x + offset.x, y, z + offset.z, xd, yd, zd);
		}
	}
}
