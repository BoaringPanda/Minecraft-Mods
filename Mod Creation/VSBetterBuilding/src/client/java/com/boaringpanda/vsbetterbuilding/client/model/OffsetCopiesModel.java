package com.boaringpanda.vsbetterbuilding.client.model;

import java.util.List;
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

import com.boaringpanda.vsbetterbuilding.block.CornerTorches;
import com.boaringpanda.vsbetterbuilding.block.FlowerClumps;

/**
 * A block's own vanilla model, full size, drawn once at each of a few offsets: a clump of flowers ({@link FlowerClumps}) or a group of
 * torches ({@link CornerTorches}). Textures, cutout and glow (the open eyeblossom) all come from that model.
 */
public class OffsetCopiesModel extends WrapperBlockStateModel {
	private final List<Vec3> offsets;

	public OffsetCopiesModel(BlockStateModel wrapped, List<Vec3> offsets) {
		super(wrapped);
		this.offsets = offsets;
	}

	/** Wraps {@code model} if {@code state} is a clump of flowers or a group of torches; otherwise returns it unchanged. */
	public static BlockStateModel wrapIfCopied(BlockStateModel model, BlockState state) {
		List<Vec3> offsets = FlowerClumps.isClump(state) ? FlowerClumps.offsets(state) : CornerTorches.offsets(state);
		return offsets.isEmpty() ? model : new OffsetCopiesModel(model, offsets);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		for (Vec3 offset : offsets) {
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

	/** Never shares cached geometry with the same block drawn once. */
	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return null;
	}
}
