package com.boaringpanda.vsbetterbuilding.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.boaringpanda.vsbetterbuilding.block.FenceRopeInteraction;

/**
 * One end of a rope tied between two fences with no animal on it. It is a fence knot (it looks and behaves like one), and it is also
 * {@link Leashable}: its rope is held by the knot on the <em>other</em> fence, or by a player while the rope is being carried. So
 * vanilla's leash machinery syncs the rope to players, saves it, cuts it with shears, hands it to a player who right-clicks the knot,
 * and drops a lead when it breaks, exactly as for a leashed animal.
 * <p>
 * It differs from a vanilla knot in what it does with its own rope:
 * <ul>
 *   <li>Vanilla knots aren't saved (a leashed animal recreates them on load). This one carries the rope, so it is saved, with the leash
 *       data written next to its block position.</li>
 *   <li>It ticks the leash on the server ({@link Leashable#tickLeash}), which restores the saved leash after a reload and snaps the rope
 *       (dropping the lead) if the other end is gone or the player carrying it walks too far.</li>
 *   <li>It is never pulled or spun by its rope, since it is tied to a fence.</li>
 *   <li>Vanilla discards a knot as soon as nothing is leashed to it. This one also has its own rope, so it is only discarded once it has
 *       neither: nothing leashed to it and no rope of its own. (An animal or another rope tied to the same fence keeps it alive.)</li>
 *   <li>When its fence is broken it drops its rope as a lead, as a leashed animal does when it dies.</li>
 * </ul>
 * Made by {@link FenceRopeInteraction}.
 */
public class RopeKnotEntity extends LeashFenceKnotEntity implements Leashable {
	/**
	 * How far above the knot's position a rope leaves it: 7 pixels up, one pixel below the top of the knot model, which is 8 pixels (half a
	 * block) tall standing on the entity's position. (Not {@code LeashFenceKnotEntity.OFFSET_Y}, 0.375, which is how high the knot sits in
	 * its block, not where a rope attaches.)
	 */
	public static final double ROPE_HEIGHT = 7.0 / 16.0;
	private static final int LIMIT_MESSAGE_TICKS = 40;

	private Leashable.@Nullable LeashData leashData;
	/** Set once removal has begun, so dropping the rope doesn't try to discard the entity a second time. */
	private boolean dying;
	/** Whether the player carrying this rope was out of reach last tick, so the limit message is sent the moment they cross it. */
	private boolean beyondReach;

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
	public Leashable.@Nullable LeashData getLeashData() {
		return this.leashData;
	}

	@Override
	public void setLeashData(Leashable.@Nullable LeashData leashData) {
		this.leashData = leashData;
	}

	/**
	 * The rope leaves from near the top of the knot. A rope between two knots is drawn by the mod's own renderer, which uses the same
	 * point at both ends.
	 */
	@Override
	public Vec3 getLeashOffset() {
		return new Vec3(0.0, ROPE_HEIGHT, 0.0);
	}

	@Override
	public Vec3 getLeashOffset(float partialTick) {
		return this.getLeashOffset();
	}

	/**
	 * What this knot's rope may be tied to: the player carrying it, or a knot on another fence within reach
	 * ({@link FenceRopeInteraction#tieProblem}). Every vanilla way of tying asks this first (a fence clicked with a lead or anything else, a
	 * knot clicked, a mob or boat sneak-clicked), so a click on the start fence or on a fence out of reach ties nothing and goes on to the
	 * held item as usual: a held block is placed.
	 */
	@Override
	public boolean canHaveALeashAttachedTo(Entity entity) {
		if (entity instanceof LeashFenceKnotEntity knot) {
			if (knot.getPos().equals(this.getPos()) || FenceRopeInteraction.tieProblem(this.level(), this.getPos(), knot.getPos()) != null) {
				return false;
			}
		} else if (!(entity instanceof Player)) {
			return false;
		}

		return Leashable.super.canHaveALeashAttachedTo(entity);
	}

	/** A knot is tied to its fence: a player walking off with the rope must not push it around or turn it (vanilla's elastic pull does both). */
	@Override
	public boolean checkElasticInteractions(Entity leashHolder, Leashable.LeashData leashData) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel serverLevel) {
			Leashable.tickLeash(serverLevel, this);
			this.warnIfCarriedBeyondReach();
		}
	}

	/**
	 * While a player carries this rope further than it can be tied, tell them: right away on crossing the limit, then every
	 * {@value #LIMIT_MESSAGE_TICKS} ticks so the message stays up (an action-bar message fades after about 3 seconds).
	 */
	private void warnIfCarriedBeyondReach() {
		if (!(this.getLeashHolder() instanceof Player player) || !FenceRopeInteraction.isBeyondReach(this.getPos(), player.blockPosition())) {
			this.beyondReach = false;
			return;
		}

		if (!this.beyondReach || this.tickCount % LIMIT_MESSAGE_TICKS == 0) {
			player.sendOverlayMessage(Component.translatable("message.vsbetterbuilding.rope_too_far"));
		}

		this.beyondReach = true;
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
