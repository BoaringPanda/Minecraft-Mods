package com.boaringpanda.bettervanillabuilding.entity;

import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.decoration.Cushion;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.boaringpanda.bettervanillabuilding.BetterVanillaBuilding;

/**
 * Cushions the Builder Stick has set fading like a sheep named jeb_ (the blocks' version is {@code block/Rainbow}). A cushion is an entity,
 * so it's a Fabric attachment, saved with it and sent to clients, where {@code CushionRendererMixin} draws it with the rainbow cushion
 * texture. Breaking it drops its own colour.
 */
public final class RainbowCushions {
	/** Set (true) only on a rainbow cushion; removed when it's turned off, so plain cushions save nothing extra. */
	private static final AttachmentType<Boolean> RAINBOW = AttachmentRegistry.create(BetterVanillaBuilding.id("rainbow"),
			builder -> builder.persistent(Codec.BOOL).syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	private RainbowCushions() {
	}

	/** Loads the class, so the attachment is registered while the mod starts. */
	public static void initialize() {
	}

	public static boolean isRainbow(Cushion cushion) {
		return cushion.hasAttached(RAINBOW);
	}

	/** Turns a cushion's fade on or off (server side; clients are sent the change). */
	public static void setRainbow(Cushion cushion, boolean on) {
		if (on) {
			cushion.setAttached(RAINBOW, true);
		} else {
			cushion.removeAttached(RAINBOW);
		}
	}
}
