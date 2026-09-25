package com.boaringpanda.bettervanillabuilding.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Matrix4fc;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.leash.LeashKnotModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import com.boaringpanda.bettervanillabuilding.entity.RopeKnotEntity;

/**
 * Draws a {@link RopeKnotEntity}: vanilla's own knot model and texture, and, when it is tied to another fence's knot, a rope drawn here
 * instead of by vanilla, for two reasons (both read from the game's own {@code LeashFeatureRenderer}):
 * <ul>
 *   <li>Vanilla draws a leash as a <em>straight line</em> between its two attachment points, and only bends it when one end is higher than the
 *       other. So two fences at the same height never got any hang.</li>
 *   <li>Vanilla ends the rope 0.2 above the knot, off its position. Here both ends are near the top of the knot ({@link RopeKnotEntity#ROPE_HEIGHT}), where the rope leaves the knot's side.</li>
 * </ul>
 * The rope sags in a smooth arc, the same at both ends, by an amount that grows with its length ({@link #SAG_PER_BLOCK}), so a short rope
 * is nearly taut and a long one hangs a little. It is the same thin two-tone ribbon in the same brown as vanilla's, with the same lighting
 * blended between the two ends.
 * <p>
 * If the rope is tied to something other than a fence knot (a player carrying it), vanilla's own drawing is left alone.
 */
public class RopeKnotRenderer extends EntityRenderer<RopeKnotEntity, RopeKnotRenderer.State> {
	private static final Identifier KNOT_TEXTURE = Identifier.withDefaultNamespace("textures/entity/lead_knot/lead_knot.png");
	/** How long each light or dark stripe of the rope is, in blocks (the number of steps follows the rope's length). */
	private static final float SEGMENT_LENGTH = 0.1F;
	/** Half the width of the knot model (6 pixels wide): where a rope emerges from its side. */
	private static final float KNOT_HALF_WIDTH = 3.0F / 16.0F;
	private static final float WIDTH = 0.05F;
	/** How far the middle of a rope hangs below the straight line between its ends, for each block it spans across (not up). */
	private static final float SAG_PER_BLOCK = 0.06F;
	/** How many short straight pieces a rope is measured in to space its stripes evenly (see {@link #evenlySpaced}). */
	private static final int MEASURE_STEPS = 64;

	private final LeashKnotModel model;

	public RopeKnotRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new LeashKnotModel(context.bakeLayer(ModelLayers.LEASH_KNOT));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RopeKnotEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);

		state.tied = false;
		if (!(entity.getLeashHolder() instanceof LeashFenceKnotEntity holder)) {
			return;
		}

		Vec3 start = entity.getPosition(partialTick).add(0.0, RopeKnotEntity.ROPE_HEIGHT, 0.0);
		Vec3 end = holder.getPosition(partialTick).add(0.0, RopeKnotEntity.ROPE_HEIGHT, 0.0);
		state.dx = (float) (end.x - start.x);
		state.dy = (float) (end.y - start.y);
		state.dz = (float) (end.z - start.z);

		Level level = entity.level();
		BlockPos startPos = BlockPos.containing(start);
		BlockPos endPos = BlockPos.containing(end);
		state.startBlockLight = level.getBrightness(LightLayer.BLOCK, startPos);
		state.endBlockLight = level.getBrightness(LightLayer.BLOCK, endPos);
		state.startSkyLight = level.getBrightness(LightLayer.SKY, startPos);
		state.endSkyLight = level.getBrightness(LightLayer.SKY, endPos);

		state.tied = true;
		// Vanilla's straight rope would be drawn on top of this one, so it is left out.
		state.leashStates = null;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		collector.submitModel(this.model, state, poseStack, KNOT_TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();

		if (state.tied) {
			collector.submitCustomGeometry(poseStack, RenderTypes.leash(), (pose, buffer) -> drawRope(pose, buffer, state));
		}

		super.submit(state, poseStack, collector, camera);
	}

	/**
	 * The rope as vanilla builds one (a flat two-tone ribbon, forward along the top then back along the bottom), with a sagging path.
	 * <p>
	 * It runs from where the rope leaves one knot's side to where it meets the other's, at {@link RopeKnotEntity#ROPE_HEIGHT} on both, so
	 * both ends look attached at the same point whatever the height difference (a curve that started at the knots' centres left each knot
	 * at a different height, depending on how steep the rope was there). The steps are spaced by distance along the curved rope, so the
	 * light and dark stripes are the same size everywhere on every rope: not stretched on a long one, nor at one end of a steep one.
	 * <p>
	 * The sag grows with the rope's flat span, not its full length, so a steep rope hangs less than a flat one, and a rope between fences
	 * stacked straight above each other hangs straight.
	 */
	private static void drawRope(PoseStack.Pose pose, VertexConsumer buffer, State state) {
		float horizontal = Mth.sqrt(state.dx * state.dx + state.dz * state.dz);
		float ux = horizontal > 1.0E-4F ? state.dx / horizontal : 1.0F;
		float uz = horizontal > 1.0E-4F ? state.dz / horizontal : 0.0F;

		// How far the rope is from a knot's centre when it leaves the knot's side (a square box, so further out along a diagonal).
		float inset = horizontal > 1.0E-4F ? KNOT_HALF_WIDTH / Math.max(Math.abs(ux), Math.abs(uz)) : 0.0F;
		if (horizontal < inset * 2.0F + 0.1F) {
			inset = 0.0F;
		}

		float sx = ux * inset;
		float sz = uz * inset;
		float cx = state.dx - 2.0F * sx;
		float cy = state.dy;
		float cz = state.dz - 2.0F * sz;
		float sag = Mth.sqrt(cx * cx + cz * cz) * SAG_PER_BLOCK;
		float[] progresses = evenlySpaced(cx, cy, cz, sag);
		int steps = progresses.length - 1;

		Matrix4fc matrix = pose.pose();
		for (int k = 0; k <= steps; k++) {
			addVertexPair(buffer, matrix, state, sx, sz, cx, cy, cz, sag, ux, uz, k, progresses[k], false, WIDTH);
		}

		for (int k = steps; k >= 0; k--) {
			addVertexPair(buffer, matrix, state, sx, sz, cx, cy, cz, sag, ux, uz, k, progresses[k], true, 0.0F);
		}
	}

	/**
	 * Where along the curve (0 to 1) each step of the rope goes, so that the steps are {@link #SEGMENT_LENGTH} apart measured along the
	 * rope itself. Equal steps of the curve's own 0-to-1 are not equal lengths: where a sloped rope's sag steepens it, they are longer, so
	 * the stripes there would stretch. The curve is measured in {@value #MEASURE_STEPS} short straight pieces, and each step is placed by
	 * how far along those it falls.
	 */
	private static float[] evenlySpaced(float cx, float cy, float cz, float sag) {
		float[] lengthTo = new float[MEASURE_STEPS + 1];
		float dxz = Mth.sqrt(cx * cx + cz * cz) / MEASURE_STEPS;
		float prevY = 0.0F;
		for (int i = 1; i <= MEASURE_STEPS; i++) {
			float y = ropeY(cy, sag, i / (float) MEASURE_STEPS);
			float dy = y - prevY;
			lengthTo[i] = lengthTo[i - 1] + Mth.sqrt(dxz * dxz + dy * dy);
			prevY = y;
		}

		float length = lengthTo[MEASURE_STEPS];
		int steps = Math.max(2, Math.round(length / SEGMENT_LENGTH));
		float[] progresses = new float[steps + 1];
		int piece = 0;
		for (int k = 0; k <= steps; k++) {
			float target = length * k / steps;
			while (piece < MEASURE_STEPS - 1 && lengthTo[piece + 1] < target) {
				piece++;
			}

			float pieceLength = lengthTo[piece + 1] - lengthTo[piece];
			float within = pieceLength > 1.0E-6F ? Mth.clamp((target - lengthTo[piece]) / pieceLength, 0.0F, 1.0F) : 0.0F;
			progresses[k] = (piece + within) / MEASURE_STEPS;
		}

		return progresses;
	}

	/** How far above the start the rope is at {@code progress}: along the straight line, dipping below it by a parabola deepest halfway. */
	private static float ropeY(float cy, float sag, float progress) {
		return cy * progress - sag * 4.0F * progress * (1.0F - progress);
	}

	private static void addVertexPair(VertexConsumer buffer, Matrix4fc matrix, State state, float sx, float sz, float cx, float cy, float cz, float sag, float ux, float uz, int k, float progress, boolean backwards, float fudge) {
		int block = (int) Mth.lerp(progress, (float) state.startBlockLight, (float) state.endBlockLight);
		int sky = (int) Mth.lerp(progress, (float) state.startSkyLight, (float) state.endSkyLight);
		int light = LightCoordsUtil.pack(block, sky);

		float colorModifier = k % 2 == (backwards ? 1 : 0) ? 0.7F : 1.0F;
		float r = 0.5F * colorModifier;
		float g = 0.4F * colorModifier;
		float b = 0.3F * colorModifier;

		float x = sx + cx * progress;
		float y = (float) RopeKnotEntity.ROPE_HEIGHT + ropeY(cy, sag, progress);
		float z = sz + cz * progress;

		// The ribbon's cross-section must be square to the rope, not to the ground. Widening it sideways and straight up (as vanilla does)
		// squashes and shears it wherever the rope is tilted, which is the warping seen on ropes between fences at different heights. So its
		// "up" is tilted to stay perpendicular to the rope's direction here.
		float tangentHorizontal = Mth.sqrt(cx * cx + cz * cz);
		float tangentY = cy - sag * 4.0F * (1.0F - 2.0F * progress);
		float tangentLength = Mth.sqrt(tangentHorizontal * tangentHorizontal + tangentY * tangentY);
		float nHorizontal = tangentLength > 1.0E-4F ? -tangentY / tangentLength : 0.0F;
		float nVertical = tangentLength > 1.0E-4F ? tangentHorizontal / tangentLength : 1.0F;
		float sideX = -uz * (WIDTH / 2.0F);
		float sideZ = ux * (WIDTH / 2.0F);
		float low = fudge - WIDTH / 2.0F;
		float high = WIDTH / 2.0F - fudge;
		float centreY = y + WIDTH / 2.0F;

		buffer.addVertex(matrix, x + sideX + ux * nHorizontal * low, centreY + nVertical * low, z + sideZ + uz * nHorizontal * low).setColor(r, g, b, 1.0F).setLight(light);
		buffer.addVertex(matrix, x - sideX + ux * nHorizontal * high, centreY + nVertical * high, z - sideZ + uz * nHorizontal * high).setColor(r, g, b, 1.0F).setLight(light);
	}

	/** What is needed to draw the rope, worked out on the render thread's extract step so drawing never touches the world. */
	public static class State extends EntityRenderState {
		boolean tied;
		float dx;
		float dy;
		float dz;
		int startBlockLight;
		int endBlockLight;
		int startSkyLight;
		int endSkyLight;
	}
}
