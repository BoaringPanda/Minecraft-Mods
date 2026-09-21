package com.boaringpanda.bpsbettervanillabuilding.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * One end of a rope tied between two fences with no animal on it. It is a fence knot (it looks and behaves like one, and vanilla's
 * own knot renderer draws it), and it is also {@link Leashable}: it is leashed to the knot on the <em>other</em> fence, so
 * vanilla's leash machinery draws the rope, syncs it to players, saves it, and drops a lead when it breaks, exactly as for a
 * leashed animal. Nothing about a rope is drawn or synced by this mod's own code.
 * <p>
 * It differs from a vanilla knot in what it does with its own rope:
 * <ul>
 *   <li>Vanilla knots aren't saved (a leashed animal recreates them on load). This one carries the rope, so it is saved,
 *       with the leash data written next to its block position.</li>
 *   <li>It ticks the leash on the server ({@link Leashable#tickLeash}), which restores the saved leash after a reload and drops
 *       the lead if the other knot is gone or the rope is being carried too far.</li>
 *   <li>Vanilla discards a knot as soon as nothing is leashed to it. This one also has its own leash, so it is only discarded once it
 *       has neither: nothing leashed to it and no rope of its own. (An animal tied to the same fence keeps it alive.)</li>
 *   <li>When its fence is broken it drops its rope, as a leashed animal does when it dies.</li>
 * </ul>
 * Made by {@link com.boaringpanda.bpsbettervanillabuilding.block.FenceRopeInteraction}.
 */
public class RopeKnotEntity extends LeashFenceKnotEntity implements Leashable {
	@Nullable
	private Leashable.LeashData leashData;
	/** Set once removal has begun, so dropping the rope doesn't try to discard the entity a second time. */
	private boolean dying;

	public RopeKnotEntity(EntityType<? extends LeashFenceKnotEntity> type, Level level) {
		super(type, level);
	}

	/** A rope knot on the fence at {@code pos}, not yet in the world and not yet tied to anything. */
	public static RopeKnotEntity create(Level level, BlockPos pos) {
		RopeKnotEntity knot = new RopeKnotEntity(RopeKnots.ROPE_KNOT, level);
		knot.setPos(pos.getX(), pos.getY(), pos.getZ());
		return knot;
	}

	@Override
	@Nullable
	public Leashable.LeashData getLeashData() {
		return this.leashData;
	}

	@Override
	public void setLeashData(@Nullable Leashable.LeashData leashData) {
		this.leashData = leashData;
	}

	/**
	 * How far above the knot's position a rope leaves it: 7 pixels up, one pixel below the top of the knot model, which is 8 pixels
	 * (half a block) tall standing on the entity's position. The user asked for it high on the knot, not in the middle (which would be 0.25).
	 * (An earlier version used {@code LeashFenceKnotEntity.OFFSET_Y} here, which is 0.375: that is how high the knot sits in its block, not
	 * where a rope attaches, and it put this end of the rope 0.175 higher than the other, so the rope slanted.)
	 */
	public static final double ROPE_HEIGHT = 7.0 / 16.0;

	/** The rope leaves from near the top of the knot. A rope between two knots is drawn by the mod's own renderer, which uses the same point at both ends. */
	@Override
	public Vec3 getLeashOffset() {
		return new Vec3(0.0, ROPE_HEIGHT, 0.0);
	}

	@Override
	public Vec3 getLeashOffset(float partialTick) {
		return this.getLeashOffset();
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel serverLevel) {
			Leashable.tickLeash(serverLevel, this);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.writeLeashData(output, this.leashData);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.readLeashData(input);
	}

	/** Something tied to this knot was removed. Only go if this knot has nothing else to hold either: no rope of its own. */
	@Override
	public void notifyLeasheeRemoved(Leashable leashee) {
		if (!this.isLeashed() && Leashable.leashableLeashedTo(this).isEmpty()) {
			this.discard();
		}
	}

	/** This knot's own rope is gone (cut, snapped, or its other end was broken). It stays only if something else is tied to it. */
	@Override
	public void onLeashRemoved() {
		if (!this.dying && !this.level().isClientSide() && Leashable.leashableLeashedTo(this).isEmpty()) {
			this.discard();
		}
	}

	/** A broken fence takes the knot with it, and its rope drops as a lead, like a leashed animal's does when it dies. */
	@Override
	public void remove(Entity.RemovalReason reason) {
		if (!this.dying) {
			this.dying = true;
			if (!this.level().isClientSide() && reason.shouldDestroy() && this.isLeashed()) {
				this.dropLeash();
			}
		}

		super.remove(reason);
	}
}
