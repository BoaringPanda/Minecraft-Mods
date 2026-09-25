package com.boaringpanda.bettervanillabuilding.client.model;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;

import com.boaringpanda.bettervanillabuilding.block.LilyPadDecorations;

/**
 * A decoration on a lily pad ({@link LilyPadDecorations}): vanilla's lily pad model, then the decoration's own model on top. Signs,
 * banners, heads and pots are drawn by their block entity renderers as usual, so for them this draws just the pad.
 */
public class LilyPadDecorationModel extends WrapperBlockStateModel {
	/**
	 * The pad's colour is baked into its quads. Its tint would otherwise be looked up for the decoration's block, which has none, and
	 * the pad would come out grey.
	 */
	private static final QuadTransform PAD_GREEN = quad -> {
		for (int vertex = 0; vertex < 4; vertex++) {
			quad.color(vertex, BlockColors.LILY_PAD_IN_WORLD);
		}
		quad.tintIndex(-1);
		return true;
	};

	public LilyPadDecorationModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	/** Wraps {@code model} if {@code state} is a decoration on a pad; otherwise returns it unchanged. */
	public static BlockStateModel wrapIfOnPad(BlockStateModel model, BlockState state) {
		return LilyPadDecorations.onPad(state) ? new LilyPadDecorationModel(model) : model;
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		// A bare pad's random rotation comes from its own seed, so seeding the same way keeps the pad turned as it was before.
		BlockState pad = Blocks.LILY_PAD.defaultBlockState();
		random.setSeed(pad.getSeed(pos));
		emitter.pushTransform(PAD_GREEN);
		Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(pad).emitQuads(emitter, level, pos, pad, random, cullTest);
		emitter.popTransform();
		random.setSeed(state.getSeed(pos));
		wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
	}

	@Override
	public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		BlockState pad = Blocks.LILY_PAD.defaultBlockState();
		return wrapped.materialFlags(level, pos, state, random)
				| Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(pad).materialFlags(level, pos, pad, random);
	}

	/** Never shares cached geometry with the same decoration off a pad. */
	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return null;
	}
}
