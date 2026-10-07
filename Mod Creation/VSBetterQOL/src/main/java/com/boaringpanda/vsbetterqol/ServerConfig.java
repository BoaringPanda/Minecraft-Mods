package com.boaringpanda.vsbetterqol;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.netty.buffer.ByteBuf;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.LevelAccessor;

import com.boaringpanda.vsbetterqol.config.ConfigFile;
import com.boaringpanda.vsbetterqol.config.Option;

// World/server settings, in config/vsbetterqol-server.properties (singleplayer: the player's own; a server: the server's, for everyone).
// Read again whenever a world or server starts. Swords and double doors also run in each player's game, so the server sends its
// settings on join (SyncPayload) and the client side goes by those: both sides always agree, and on a server without the mod the
// client side stays vanilla.
public final class ServerConfig {
	public static final Option SWORDS_ARE_WEAPONS = new Option("swords_are_weapons",
			"true: swords can't break blocks (except cobwebs and bamboo) and hit mobs straight through grass, flowers and other plants. false: swords break blocks and hit plants like normal.");
	public static final Option FAST_LEAF_DECAY = new Option("fast_leaf_decay",
			"Leaves cut off from their tree are gone within about 1.5 seconds, and logs placed by players don't keep natural leaves alive. Off: leaves decay like vanilla.");
	public static final Option DOUBLE_DOORS = new Option("double_doors",
			"Opening or closing one door of a double door moves the other one too (sneak to move just one). Off: doors work like vanilla.");
	public static final Option DURABILITY_WARNING = new Option("durability_warning",
			"Warns players above the hotbar, with a sound, when an item drops to 7 durability or less. Off: no warning.");

	public static final List<Option> ALL = List.of(SWORDS_ARE_WEAPONS, FAST_LEAF_DECAY, DOUBLE_DOORS, DURABILITY_WARNING);

	private static final ConfigFile FILE = new ConfigFile("vsbetterqol-server.properties", List.of(
			"VS Better QOL: world and server settings.",
			"true = on, false = off (yes/no works too). Changes apply the next time the world or server starts.",
			"Singleplayer uses this file. On a server only the server's copy counts, and it applies to every player."), ALL);

	// Keys of the settings the connected server has on, as it sent them. Client side only; null until the server sends them (never, on a
	// server without the mod).
	private static volatile @Nullable Set<String> clientView;

	private ServerConfig() {
	}

	// Sent to each player who joins: the keys of the settings that are on.
	public record SyncPayload(List<String> on) implements CustomPacketPayload {
		public static final Type<SyncPayload> TYPE = new Type<>(VSBetterQOL.id("server_settings"));
		public static final StreamCodec<ByteBuf, SyncPayload> CODEC =
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(SyncPayload::new, SyncPayload::on);

		@Override
		public Type<SyncPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);
		// Once at launch, so the file is there to edit before any world or server starts, then again at every start to pick up edits.
		FILE.load();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> FILE.load());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler.player, SyncPayload.TYPE)) {
				sender.sendPacket(new SyncPayload(ALL.stream().filter(option -> option.on).map(option -> option.key).toList()));
			}
		});
	}

	// Whether a setting is on for this side: the server's own file on the server, what the server sent on the client.
	public static boolean on(Option option, LevelAccessor level) {
		Set<String> view = clientView;
		return level.isClientSide() ? view != null && view.contains(option.key) : option.on;
	}

	// Client side: the connected server has the mod (it sent its settings). Features that need the mod on both sides check this.
	public static boolean clientServerHasMod() {
		return clientView != null;
	}

	// Client side: a new connection clears it (VSBetterQOLClient), then the server's SyncPayload fills it.
	public static void clearClientView() {
		clientView = null;
	}

	public static void setClientView(Collection<String> on) {
		clientView = Set.copyOf(on);
	}
}
