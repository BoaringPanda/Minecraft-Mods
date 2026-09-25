package com.boaringpanda.bettervanillabuilding.client.model;

import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;

import com.boaringpanda.bettervanillabuilding.block.MixedSlabs;

/**
 * Draws a mixed slab block as the vanilla bottom-slab model of one slab plus the vanilla top-slab model of the other. The two slabs come
 * from the block entity ({@code getBlockEntityRenderData}), so one model serves every pair of slabs, including other mods' slabs. The
 * vanilla models draw themselves, so textures, tints, ambient occlusion and face culling are exactly theirs.
 */
public class MixedSlabModel implements BlockStateModel {
	/** The faces where the two slabs meet are inside the block. Slab models don't tag them for culling, so they're dropped here. */
	private static final QuadTransform HIDE_BOTTOM_SLAB_TOP = quad -> !(quad.nominalFace() == Direction.UP && quad.cullFace() == null);
	private static final QuadTransform HIDE_TOP_SLAB_BOTTOM = quad -> !(quad.nominalFace() == Direction.DOWN && quad.cullFace() == null);

	private final Material.Baked fallbackParticle;

	private MixedSlabModel(Material.Baked fallbackParticle) {
		this.fallbackParticle = fallbackParticle;
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		MixedSlabs.Halves halves = halves(level, pos);
		BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
		BlockState bottom = halves.bottomState();
		BlockState top = halves.topState();
		emitter.pushTransform(HIDE_BOTTOM_SLAB_TOP);
		models.get(bottom).emitQuads(emitter, level, pos, bottom, random, cullTest);
		emitter.popTransform();
		emitter.pushTransform(HIDE_TOP_SLAB_BOTTOM);
		models.get(top).emitQuads(emitter, level, pos, top, random, cullTest);
		emitter.popTransform();
	}

	/** Particles that know where they are (walking, mining) use the top slab's texture. */
	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		BlockState top = halves(level, pos).topState();
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(top).particleMaterial(level, pos, top);
	}

	@Override
	public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		MixedSlabs.Halves halves = halves(level, pos);
		BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
		return models.get(halves.bottomState()).materialFlags(level, pos, halves.bottomState(), random)
				| models.get(halves.topState()).materialFlags(level, pos, halves.topState(), random);
	}

	/** Only for callers with no block to look at; the Fabric renderer draws the block through {@link #emitQuads}. */
	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
	}

	@Override
	public Material.Baked particleMaterial() {
		return fallbackParticle;
	}

	@Override
	public int materialFlags() {
		return 0;
	}

	private static MixedSlabs.Halves halves(BlockAndTintGetter level, BlockPos pos) {
		return level.getBlockEntityRenderData(pos) instanceof MixedSlabs.Halves halves ? halves : MixedSlabs.Halves.FALLBACK;
	}

	/** The mixed slab block's only model. It needs nothing loaded but the fallback particle (smooth stone, as {@code Halves.FALLBACK}). */
	public record Unbaked() implements BlockStateModel.UnbakedRoot {
		private static final Material FALLBACK_PARTICLE = new Material(Identifier.withDefaultNamespace("block/smooth_stone"));

		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
		}

		@Override
		public BlockStateModel bake(BlockState state, ModelBaker baker) {
			return new MixedSlabModel(baker.materials().get(FALLBACK_PARTICLE, () -> "bettervanillabuilding:mixed_slab"));
		}

		@Override
		public Object visualEqualityGroup(BlockState state) {
			return this;
		}
	}
}
