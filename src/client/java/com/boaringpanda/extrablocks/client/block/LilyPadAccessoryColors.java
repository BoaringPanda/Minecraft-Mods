package com.boaringpanda.extrablocks.client.block;

import java.util.List;

import net.minecraft.client.color.block.BlockTintSources;

import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;

import com.boaringpanda.extrablocks.block.LilyPadAccessories;

/**
 * Registers the same green tint vanilla uses for {@code minecraft:lily_pad}
 * on our lily-pad-accessory blocks too.
 * <p>
 * They reuse the lily pad's own model (which has a {@code tintindex} on its
 * faces), but the tint <em>color</em> for a given tintindex is looked up per
 * {@code Block} instance, not per model. Without this registration our new
 * blocks have no registered tint at all, so the lily pad half renders with
 * no color applied - a flat white/grey pad instead of green.
 * <p>
 * Colors copied from vanilla's own registration for {@code Blocks.LILY_PAD}
 * (checked against the compiled game, not guessed): the first is the color
 * shown on the item in hand/inventory, the second is the color used for the
 * placed block in the world.
 */
public class LilyPadAccessoryColors {
	public static void initialize() {
		BlockColorRegistry.register(
				List.of(BlockTintSources.constant(0xFF71C35C, 0xFF208030)),
				LilyPadAccessories.LILY_PAD_WITH_TORCH,
				LilyPadAccessories.LILY_PAD_WITH_LANTERN
		);
	}
}
