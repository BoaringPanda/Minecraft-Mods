package com.boaringpanda.extrablocks.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.extrablocks.block.custom.LilyPadCombo;
import com.boaringpanda.extrablocks.block.custom.LilyPadShape;

/**
 * Works out whether a player is aiming at the accessory or at the lily pad of a combo block.
 * <p>
 * A combo is one block, and the server is never told where on a block the player was aiming when
 * they break it (the break packet carries only the position), so this casts the player's own view
 * ray against the pad and the accessory separately and takes the nearer one. Mining speed
 * ({@link #destroyProgress}), what actually gets removed ({@link LilyPadAccessoryBreaking}) and
 * which wireframe is drawn ({@link #outline}) all use this, so they agree.
 */
public final class LilyPadTarget {
	public enum Part {
		ACCESSORY,
		PAD,
		/** The ray hits neither (not looking at this block, or a rounding-level miss). */
		UNKNOWN
	}

	private LilyPadTarget() {
	}

	public static Part aimedPart(LilyPadCombo combo, BlockState state, Player player, BlockPos pos) {
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));

		BlockHitResult padHit = LilyPadShape.SHAPE.clip(from, to, pos);
		BlockHitResult accessoryHit = combo.accessoryShape(state).clip(from, to, pos);
		if (accessoryHit == null) {
			return padHit == null ? Part.UNKNOWN : Part.PAD;
		}

		boolean accessoryNearer = padHit == null || accessoryHit.getLocation().distanceToSqr(from) <= padHit.getLocation().distanceToSqr(from);
		return accessoryNearer ? Part.ACCESSORY : Part.PAD;
	}

	/** Breaking treats "unknown" as the accessory - what a break did before there was a choice. */
	public static boolean aimedAtAccessory(LilyPadCombo combo, BlockState state, Player player, BlockPos pos) {
		return aimedPart(combo, state, player, pos) != Part.PAD;
	}

	/**
	 * Mining progress per tick for a combo: normal (the accessory's own hardness and tool rules) when
	 * aiming at the accessory, instant when aiming at the pad, like a plain lily pad.
	 */
	public static float destroyProgress(LilyPadCombo combo, BlockState state, Player player, BlockPos pos, float normalProgress) {
		return aimedAtAccessory(combo, state, player, pos) ? normalProgress : 1.0F;
	}

	/**
	 * The shape a combo reports as its outline. The game uses the same {@code getShape} call both to
	 * cast the crosshair ray and to draw the wireframe, and gives it the looking player as context, so
	 * returning only the aimed part makes just that part light up. Anything else (other entities, no
	 * context, not looking at this block) gets {@code wholeOutline}, the pad and accessory together.
	 */
	public static VoxelShape outline(LilyPadCombo combo, BlockState state, BlockPos pos, CollisionContext context, VoxelShape wholeOutline) {
		if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player) {
			return switch (aimedPart(combo, state, player, pos)) {
				case ACCESSORY -> combo.accessoryShape(state);
				case PAD -> LilyPadShape.SHAPE;
				case UNKNOWN -> wholeOutline;
			};
		}

		return wholeOutline;
	}

	/**
	 * The real vanilla state of the part this player is aiming at: a plain lily pad, or the accessory on its
	 * own. The cracks and hit sound while mining come from it (see the client's {@code ClientLevelMixin}).
	 * Aiming at neither counts as the accessory, the same as for breaking.
	 */
	public static BlockState partState(LilyPadCombo combo, BlockState state, @Nullable Player player, BlockPos pos) {
		if (player != null && aimedPart(combo, state, player, pos) == Part.PAD) {
			return Blocks.LILY_PAD.defaultBlockState();
		}

		return combo.accessoryState(state);
	}

	/**
	 * The particle burst and break sound for a combo, from the real vanilla state of each part that
	 * actually goes: just the accessory when a player broke only that, otherwise the pad and the accessory
	 * together, since the pad takes the accessory with it. Every combo's
	 * {@code spawnDestroyByEntityParticles} hands off to this.
	 * <p>
	 * {@code entity} is whoever broke it, or null. On the server, a breaking player is left out of the
	 * packet: their own client already did this when it predicted the break. That is the same split
	 * vanilla's own {@code spawnDestroyByEntityParticles} makes.
	 */
	public static void spawnDestroyParticles(LilyPadCombo combo, Level level, @Nullable Entity entity, BlockPos pos, BlockState state) {
		boolean accessoryOnly = entity instanceof Player player && aimedAtAccessory(combo, state, player, pos);
		if (!accessoryOnly) {
			level.levelEvent(entity, LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(Blocks.LILY_PAD.defaultBlockState()));
		}

		level.levelEvent(entity, LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(combo.accessoryState(state)));
	}
}
