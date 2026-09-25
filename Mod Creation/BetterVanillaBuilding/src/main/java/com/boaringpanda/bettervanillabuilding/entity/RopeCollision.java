package com.boaringpanda.bettervanillabuilding.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.boaringpanda.bettervanillabuilding.block.FenceRopeInteraction;

/**
 * Makes tied fence ropes solid, as an invisible wall along each rope, of one of two heights:
 * <ul>
 *   <li>Animals get a wall as tall as a fence's collision ({@value #ANIMAL_WALL_HEIGHT}), which they can't jump, so ropes make pens.</li>
 *   <li>Players, every other mob (hostile ones, villagers, golems) and an animal a player is riding get a wall only as high as the rope
 *       itself ({@link #JUMP_WALL_HEIGHT}): too high to step over, low enough to jump.</li>
 * </ul>
 * Anything else (items, boats, arrows) passes through. The top of a wall follows the rope's slope, so a rope going up a block is the
 * same height to jump everywhere along it.
 * <p>
 * The walls are only added to what a moving entity collides with ({@code Entity.collide}, via
 * {@link com.boaringpanda.bettervanillabuilding.mixin.EntityMixin}), so nothing else about the world changes. Mob pathfinding only looks at
 * blocks, so a mob doesn't know a rope is there: a non-animal mob that walks into one is told to jump ({@link #hopIfBlockedByRope}),
 * and an animal heading outside its pen walks up to the rope and stops, as it would at a fence.
 */
public class RopeCollision {
	private static final double ANIMAL_WALL_HEIGHT = 1.5;
	/** The rope's height where it leaves a knot (6/16 knot offset + 7/16 up the knot = 13/16), plus its thickness. */
	private static final double JUMP_WALL_HEIGHT = 14.0 / 16.0;
	private static final double WALL_HALF_WIDTH = 2.0 / 16.0;
	/** Spacing of the small boxes that make up a wall, less than their width so a diagonal wall has no gaps. */
	private static final double STEP = 0.125;
	/** How far from an entity a rope knot can be and its rope still reach it (the longest rope is about 6.1 blocks: 6 across, 1 up). */
	private static final double SEARCH_RANGE = FenceRopeInteraction.MAX_DISTANCE + 0.5;
	/** How far beside a mob that has bumped into something a rope counts as what it bumped into. */
	private static final double TOUCH_DISTANCE = 0.1;

	/** How tall ropes are to {@code entity}, or 0 if it passes through them. */
	private static double wallHeight(Entity entity) {
		if (entity instanceof Animal) {
			return entity.getControllingPassenger() instanceof Player ? JUMP_WALL_HEIGHT : ANIMAL_WALL_HEIGHT;
		}

		return entity instanceof Player || entity instanceof Mob ? JUMP_WALL_HEIGHT : 0.0;
	}

	/** {@code shapes} (what {@code entity} collides with in {@code area}) plus the walls of any tied ropes there. */
	public static List<VoxelShape> withRopes(Entity entity, AABB area, List<VoxelShape> shapes) {
		double height = wallHeight(entity);
		if (height <= 0.0) {
			return shapes;
		}

		List<VoxelShape> result = null;
		AABB searchArea = area.inflate(SEARCH_RANGE, ANIMAL_WALL_HEIGHT, SEARCH_RANGE);
		for (RopeKnotEntity rope : entity.level().getEntitiesOfClass(RopeKnotEntity.class, searchArea)) {
			if (!(rope.getLeashHolder() instanceof LeashFenceKnotEntity other)) {
				continue;
			}

			BlockPos from = rope.getPos();
			BlockPos to = other.getPos();
			double bottom = Math.min(from.getY(), to.getY());
			double dx = to.getX() - from.getX();
			double dz = to.getZ() - from.getZ();
			int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) / STEP));
			for (int i = 0; i <= steps; i++) {
				double progress = i / (double) steps;
				double x = from.getX() + 0.5 + dx * progress;
				double z = from.getZ() + 0.5 + dz * progress;
				double top = Mth.lerp(progress, from.getY(), to.getY()) + height;
				AABB box = new AABB(x - WALL_HALF_WIDTH, bottom, z - WALL_HALF_WIDTH, x + WALL_HALF_WIDTH, top, z + WALL_HALF_WIDTH);
				if (box.intersects(area)) {
					if (result == null) {
						result = new ArrayList<>(shapes);
					}
					result.add(Shapes.create(box));
				}
			}
		}

		return result == null ? shapes : result;
	}

	/** A mob (not an animal) on the ground that has just walked into a rope jumps it, since its pathfinding can't see the rope. */
	public static void hopIfBlockedByRope(Entity entity) {
		if (entity instanceof Mob mob && !(mob instanceof Animal) && mob.horizontalCollision && mob.onGround()
				&& !withRopes(mob, mob.getBoundingBox().inflate(TOUCH_DISTANCE, 0.0, TOUCH_DISTANCE), List.of()).isEmpty()) {
			mob.getJumpControl().jump();
		}
	}
}
