package com.boaringpanda.bpsbettervanillabuilding.block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.boaringpanda.bpsbettervanillabuilding.entity.RopeKnotEntity;

/**
 * Lets a player tie a rope (a lead) from one fence to another with no animal, up to {@value #MAX_DISTANCE} blocks apart.
 * <ol>
 *   <li>Right-click a fence with a lead: the rope <em>starts</em> there (the action bar says so). Right-click the same fence again
 *       to cancel.</li>
 *   <li>Right-click a second fence with a lead within reach: the rope is tied and one lead is used (not in creative). If it is
 *       too far the rope keeps waiting.</li>
 * </ol>
 * The waiting start is only remembered on the server, per player, and forgotten after {@code WAIT_TICKS}, on leaving the
 * game or changing dimension. Nothing is spent until the rope is actually tied.
 * <p>
 * A tied rope is a {@link RopeKnotEntity} at the first fence, leashed to a vanilla knot at the second, so vanilla draws it, saves it and
 * drops a lead when it breaks. Everything else is vanilla too: a knot right-clicked with an empty hand hands its rope to the player
 * (which is how a rope is moved), and it is tied to a fence with a lead by vanilla's own {@code LeadItem}. This class only steps in
 * to enforce the distance for that case, and otherwise leaves anything that isn't "a lead on a fence with nothing tied to
 * the player" to vanilla.
 * <p>
 * Distance is between the centres of the two fence blocks, so two fences 5 apart in a line are fine, a (4, 3) diagonal is exactly
 * 5 and fine, and (4, 4) is not.
 */
public class FenceRopeInteraction {
	private static final double MAX_DISTANCE = 5.0;
	/** How many blocks higher or lower than the first fence the second may be. */
	private static final int MAX_HEIGHT_DIFFERENCE = 1;
	private static final long WAIT_TICKS = 600;

	private record Waiting(ResourceKey<Level> dimension, BlockPos pos, long gameTime) {
	}

	private static final Map<UUID, Waiting> WAITING = new HashMap<>();

	public static void initialize() {
		UseBlockCallback.EVENT.register(FenceRopeInteraction::onUseBlock);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> WAITING.remove(handler.getPlayer().getUUID()));
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hit.getBlockPos();
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(Items.LEAD) || !level.getBlockState(pos).is(BlockTags.FENCES)) {
			return InteractionResult.PASS;
		}

		// Something is already tied to the player: an animal, or a rope picked up off a knot. Vanilla ties it to this fence, and all
		// this adds is the distance rule for a rope.
		List<Leashable> carried = Leashable.leashableLeashedTo(player);
		if (!carried.isEmpty()) {
			for (Leashable leashable : carried) {
				if (leashable instanceof RopeKnotEntity rope) {
					Component problem = reachProblem(rope.getPos(), pos);
					if (problem != null) {
						player.sendOverlayMessage(problem);
						return InteractionResult.FAIL;
					}
				}
			}

			return InteractionResult.PASS;
		}

		UUID id = player.getUUID();
		Waiting waiting = WAITING.get(id);
		if (waiting != null && (!waiting.dimension().equals(level.dimension()) || level.getGameTime() - waiting.gameTime() > WAIT_TICKS)) {
			WAITING.remove(id);
			waiting = null;
		}

		if (waiting == null) {
			WAITING.put(id, new Waiting(level.dimension(), pos, level.getGameTime()));
			player.sendOverlayMessage(Component.translatable("message.bpsbettervanillabuilding.rope_started"));
			return InteractionResult.SUCCESS_SERVER;
		}

		BlockPos start = waiting.pos();
		if (start.equals(pos)) {
			WAITING.remove(id);
			player.sendOverlayMessage(Component.translatable("message.bpsbettervanillabuilding.rope_cancelled"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (!level.getBlockState(start).is(BlockTags.FENCES)) {
			WAITING.remove(id);
			player.sendOverlayMessage(Component.translatable("message.bpsbettervanillabuilding.rope_start_gone"));
			return InteractionResult.SUCCESS_SERVER;
		}

		Component problem = reachProblem(start, pos);
		if (problem != null) {
			player.sendOverlayMessage(problem);
			return InteractionResult.SUCCESS_SERVER;
		}

		if (alreadyTied(level, start, pos)) {
			player.sendOverlayMessage(Component.translatable("message.bpsbettervanillabuilding.rope_already_tied"));
			return InteractionResult.SUCCESS_SERVER;
		}

		WAITING.remove(id);
		tie(level, player, held, start, pos);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** A rope knot on {@code start}, leashed to the knot on {@code end} (vanilla makes that one, or finds the one already there). */
	private static void tie(Level level, Player player, ItemStack lead, BlockPos start, BlockPos end) {
		RopeKnotEntity rope = RopeKnotEntity.create(level, start);
		level.addFreshEntity(rope);

		LeashFenceKnotEntity target = LeashFenceKnotEntity.getOrCreateKnot(level, end);
		rope.setLeashedTo(target, true);
		rope.playPlacementSound();
		level.gameEvent(GameEvent.BLOCK_ATTACH, end, GameEvent.Context.of(player));

		if (!player.hasInfiniteMaterials()) {
			lead.shrink(1);
		}
	}

	/**
	 * Why a rope can't run from the fence at {@code from} to the fence at {@code to}, as the message to show, or null if it can. The height
	 * is checked first, since it is the more specific complaint: the second fence may be at most {@value #MAX_HEIGHT_DIFFERENCE} block up or
	 * down from the first ("too high" or "too low" is about the fence being connected to). Then the distance between the fences' centres.
	 */
	@Nullable
	private static Component reachProblem(BlockPos from, BlockPos to) {
		int up = to.getY() - from.getY();
		if (up > MAX_HEIGHT_DIFFERENCE) {
			return Component.translatable("message.bpsbettervanillabuilding.rope_too_high");
		}
		if (up < -MAX_HEIGHT_DIFFERENCE) {
			return Component.translatable("message.bpsbettervanillabuilding.rope_too_low");
		}
		if (Vec3.atCenterOf(from).distanceToSqr(Vec3.atCenterOf(to)) > MAX_DISTANCE * MAX_DISTANCE + 1.0E-6) {
			return Component.translatable("message.bpsbettervanillabuilding.rope_too_far");
		}

		return null;
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
