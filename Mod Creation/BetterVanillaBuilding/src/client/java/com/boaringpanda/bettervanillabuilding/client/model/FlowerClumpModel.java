package com.boaringpanda.bettervanillabuilding.client.model;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;

import com.boaringpanda.bettervanillabuilding.block.FlowerClumps;

/**
 * A clump of flowers ({@link FlowerClumps}): the flower's own vanilla model, full size, drawn once in each filled quarter. Textures,
 * cutout and the open eyeblossom's glow all come from that model.
 */
public class FlowerClumpModel extends WrapperBlockStateModel {
	public FlowerClumpModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	/** Wraps {@code model} if {@code state} is a clump; otherwise returns it unchanged. */
	public static BlockStateModel wrapIfClump(BlockStateModel model, BlockState state) {
		return FlowerClumps.isClump(state) ? new FlowerClumpModel(model) : model;
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		for (Vec3 offset : FlowerClumps.offsets(state)) {
			float dx = (float) offset.x;
			float dz = (float) offset.z;
			emitter.pushTransform(quad -> {
				for (int vertex = 0; vertex < 4; vertex++) {
					quad.pos(vertex, quad.x(vertex) + dx, quad.y(vertex), quad.z(vertex) + dz);
				}
				return true;
			});
			random.setSeed(state.getSeed(pos));
			wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
			emitter.popTransform();
		}
	}

	/** Never shares cached geometry with a single flower of the same kind. */
	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return null;
	}
}
