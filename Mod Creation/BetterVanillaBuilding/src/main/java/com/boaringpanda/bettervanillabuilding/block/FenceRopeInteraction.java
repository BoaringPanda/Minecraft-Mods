package com.boaringpanda.bettervanillabuilding.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import com.boaringpanda.bettervanillabuilding.entity.RopeKnotEntity;

/**
 * Lets a player tie a rope (a lead) from one fence to another with no animal on it, the way an animal is led and tied:
 * <ol>
 *   <li>Right-click a fence with a lead while nothing is on a lead to you: "Rope fence started", a knot is tied there and its rope follows
 *       your hand, as a leashed animal's would. One lead is used (not in creative).</li>
 *   <li>Right-click another fence (with anything in hand, as vanilla allows for a led animal) and the rope is tied to it. Until then it
 *       stays in hand: clicking the fence it started from ties nothing (a held block is placed as usual), and it only lets go the
 *       vanilla way, snapping (and dropping the lead) if the player walks {@code Leashable.leashSnapDistance} blocks away.</li>
 * </ol>
 * A rope reaches at most {@value #MAX_BLOCKS} blocks counting both fences (so the fences are at most {@value #MAX_DISTANCE} apart,
 * measured flat between the blocks' centres: (4, 0) and (3, 2) are fine, (3, 3) is not), and may go at most
 * {@value #MAX_HEIGHT_DIFFERENCE} block up or down. Clicking a fence out of reach says why on the action bar and the rope stays in hand.
 * Walking out of reach while carrying a rope shows the same limit message ({@link RopeKnotEntity} checks with {@link #isBeyondReach}).
 * <p>
 * Everything else is vanilla's, because a rope is a {@link RopeKnotEntity} held on a lead like an animal: the actual tying at the second
 * fence ({@code LeadItem.bindPlayerMobs}, or the knot's own {@code interact} when the click lands on a knot), snapping when the player walks
 * too far, picking a rope back up off a knot with an empty hand, cutting it with shears, and dropping the lead when a fence breaks. The
 * reach rules are enforced by the knot itself ({@link RopeKnotEntity#canHaveALeashAttachedTo}, which vanilla asks before tying), so a
 * click on a fence is never cancelled and a block held while carrying a rope places normally. This class starts ropes and says why one
 * wasn't tied.
 * <p>
 * A click near the top of a fence post often lands on a knot already there rather than the fence block, so both are handled: a knot
 * counts as its fence. Holding a lead, a click on a knot starts a new rope from that fence (an empty hand still picks up what is tied to
 * it, as in vanilla).
 * <p>
 * Holding right-click repeats the click every 4 ticks. So a knot clicked a moment after it was made must not undo it (why clicking the
 * start knot does nothing), and a rope must not start in the {@value #REPEAT_TICKS} ticks after the player started or tied one, or
 * holding the button while tying at fence B would start a new rope from B straight away.
 */
public class FenceRopeInteraction {
	private static final int MAX_BLOCKS = 5;
	private static final double MAX_DISTANCE = MAX_BLOCKS - 1;
	/** How many blocks higher or lower than the first fence the second may be. */
	private static final int MAX_HEIGHT_DIFFERENCE = 1;
	/** How long after starting or tying a rope a lead click can't start another one (see the class notes on repeat clicks). */
	private static final int REPEAT_TICKS = 10;

	/** When each player last started or tied a rope (game time). Weak keys, so a player who leaves is forgotten on their own. */
	private static final Map<Player, Long> LAST_ROPE_CLICK = new WeakHashMap<>();

	public static void initialize() {
		UseBlockCallback.EVENT.register(FenceRopeInteraction::onUseBlock);
		UseEntityCallback.EVENT.register(FenceRopeInteraction::onUseEntity);
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (!level.getBlockState(pos).is(BlockTags.FENCES)) {
			return InteractionResult.PASS;
		}

		return onFence(player, level, hand, pos, false);
	}

	private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit) {
		if (!(entity instanceof LeashFenceKnotEntity knot)) {
			return InteractionResult.PASS;
		}

		return onFence(player, level, hand, knot.getPos(), true);
	}

	/**
	 * A click on the fence at {@code pos}, or on a knot tied to it ({@code onKnot}). The server decides; the client only keeps its
	 * prediction of a click on the fence itself in line (see {@link #tieAtFence}), and otherwise passes so vanilla sends the click on.
	 */
	private static InteractionResult onFence(Player player, Level level, InteractionHand hand, BlockPos pos, boolean onKnot) {
		if (player.isSpectator()) {
			return InteractionResult.PASS;
		}

		List<Leashable> carried = Leashable.leashableLeashedTo(player);
		List<RopeKnotEntity> ropes = new ArrayList<>();
		for (Leashable leashable : carried) {
			if (leashable instanceof RopeKnotEntity rope) {
				ropes.add(rope);
			}
		}

		if (!ropes.isEmpty()) {
			return onKnot ? tieAtKnot(player, level, ropes, pos) : tieAtFence(player, level, ropes, pos);
		}

		if (level.isClientSide()) {
			return InteractionResult.PASS;
		}

		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.LEAD) && carried.isEmpty() && !player.isSecondaryUseActive() && !isRepeatClick(player, level)) {
			start(player, level, held, pos);
			return InteractionResult.SUCCESS_SERVER;
		}

		return InteractionResult.PASS;
	}

	/** A new rope knot on the fence at {@code pos}, its rope held by the player. */
	private static void start(Player player, Level level, ItemStack lead, BlockPos pos) {
		RopeKnotEntity rope = RopeKnotEntity.create(level, pos);
		level.addFreshEntity(rope);
		rope.setLeashedTo(player, true);
		rope.playPlacementSound();
		level.gameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Context.of(player));
		player.sendOverlayMessage(Component.translatable("message.bettervanillabuilding.rope_started"));
		LAST_ROPE_CLICK.put(player, level.getGameTime());

		if (!player.hasInfiniteMaterials()) {
			lead.shrink(1);
		}
	}

	/**
	 * The player is carrying one or more ropes and clicked the fence block at {@code pos}. This is never cancelled, so a held block always
	 * gets placed as vanilla would: {@link RopeKnotEntity#canHaveALeashAttachedTo} is what stops vanilla tying a rope to the fence it
	 * started from or to one it can't reach, and the click then goes on to the held item. Here the server only says why a rope wasn't
	 * tied. Sneaking with an item in hand skips the fence in vanilla, so that click isn't an attempt to tie at all.
	 * <p>
	 * The client can't know what the server will do, so it predicts the held item's use (placing a block). When the ropes will be tied,
	 * the server doesn't place anything, so the client returns SUCCESS (Fabric still sends the click) and draws no block that would only
	 * vanish again.
	 */
	private static InteractionResult tieAtFence(Player player, Level level, List<RopeKnotEntity> ropes, BlockPos pos) {
		if (player.isSecondaryUseActive() || startsAt(ropes, pos)) {
			return InteractionResult.PASS;
		}

		Component problem = tieProblem(level, ropes, pos);
		if (level.isClientSide()) {
			return problem == null ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}

		if (problem != null) {
			player.sendOverlayMessage(problem);
		} else {
			LAST_ROPE_CLICK.put(player, level.getGameTime());
		}

		return InteractionResult.PASS;
	}

	/**
	 * The player is carrying one or more ropes and clicked a knot on the fence at {@code pos}. Clicking a knot never places a block, so
	 * here a rope that can't be tied cancels the click: otherwise vanilla's knot, having tied nothing, would hand the player the ropes
	 * tied to it. That includes the knot a rope started from, which just ignores the click, so the rope stays in hand.
	 */
	private static InteractionResult tieAtKnot(Player player, Level level, List<RopeKnotEntity> ropes, BlockPos pos) {
		if (level.isClientSide()) {
			return InteractionResult.PASS;
		}

		if (startsAt(ropes, pos)) {
			return InteractionResult.FAIL;
		}

		Component problem = tieProblem(level, ropes, pos);
		if (problem != null) {
			player.sendOverlayMessage(problem);
			return InteractionResult.FAIL;
		}

		LAST_ROPE_CLICK.put(player, level.getGameTime());
		return InteractionResult.PASS;
	}

	private static boolean startsAt(List<RopeKnotEntity> ropes, BlockPos pos) {
		for (RopeKnotEntity rope : ropes) {
			if (rope.getPos().equals(pos)) {
				return true;
			}
		}

		return false;
	}

	/** Why one of the carried {@code ropes} can't be tied to the fence at {@code pos}, or null if they all can. */
	private static @Nullable Component tieProblem(Level level, List<RopeKnotEntity> ropes, BlockPos pos) {
		for (RopeKnotEntity rope : ropes) {
			Component problem = tieProblem(level, rope.getPos(), pos);
			if (problem != null) {
				return problem;
			}
		}

		return null;
	}

	/** Whether this click is too soon after the player started or tied a rope to be a new one, rather than the held button repeating. */
	private static boolean isRepeatClick(Player player, Level level) {
		Long last = LAST_ROPE_CLICK.get(player);
		return last != null && level.getGameTime() - last < REPEAT_TICKS;
	}

	/**
	 * Why a rope can't run from the fence at {@code from} to the fence at {@code to}, as the message to show, or null if it can. The
	 * height is checked first, since it is the more specific complaint ("too high" or "too low" is about the fence being connected to),
	 * then the reach, then whether the two fences are already tied.
	 */
	public static @Nullable Component tieProblem(Level level, BlockPos from, BlockPos to) {
		int up = to.getY() - from.getY();
		if (up > MAX_HEIGHT_DIFFERENCE) {
			return Component.translatable("message.bettervanillabuilding.rope_too_high");
		}
		if (up < -MAX_HEIGHT_DIFFERENCE) {
			return Component.translatable("message.bettervanillabuilding.rope_too_low");
		}

		if (isBeyondReach(from, to)) {
			return Component.translatable("message.bettervanillabuilding.rope_too_far");
		}

		if (alreadyTied(level, from, to)) {
			return Component.translatable("message.bettervanillabuilding.rope_already_tied");
		}

		return null;
	}

	/** Whether the block {@code to} is further from the fence at {@code from} than a rope reaches, measured flat (height is its own rule). */
	public static boolean isBeyondReach(BlockPos from, BlockPos to) {
		double dx = to.getX() - from.getX();
		double dz = to.getZ() - from.getZ();
		return dx * dx + dz * dz > MAX_DISTANCE * MAX_DISTANCE + 1.0E-6;
	}

	/** Whether a rope already runs between these two fences, whichever end it starts from. */
	private static boolean alreadyTied(Level level, BlockPos a, BlockPos b) {
		return ropeRunsFromTo(level, a, b) || ropeRunsFromTo(level, b, a);
	}

	private static boolean ropeRunsFromTo(Level level, BlockPos from, BlockPos to) {
		for (RopeKnotEntity rope : level.getEntitiesOfClass(RopeKnotEntity.class, new AABB(from))) {
			if (rope.getPos().equals(from) && rope.getLeashHolder() instanceof LeashFenceKnotEntity holder && holder.getPos().equals(to)) {
				return true;
			}
		}

		return false;
	}
}
