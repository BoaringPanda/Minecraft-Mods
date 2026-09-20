package com.boaringpanda.extrablocks.client.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;

import com.boaringpanda.extrablocks.block.LilyPadAccessories;

/**
 * Registers the same green tint vanilla uses for {@code minecraft:lily_pad}
 * on every lily-pad-accessory combo block ({@link LilyPadAccessories#all()} -
 * this must cover every one of them, not a hand-picked subset, or whichever
 * one gets missed renders with no color at all (a flat white/grey pad
 * instead of green) - which is exactly what happened here the first few
 * times new accessories were added without updating this).
 * <p>
 * They reuse the lily pad's own model (which has a {@code tintindex} on its
 * faces), but the tint <em>color</em> for a given tintindex is looked up per
 * {@code Block} instance, not per model. Without this registration a combo
 * block has no registered tint at all for that index.
 * <p>
 * Colors copied from vanilla's own registration for {@code Blocks.LILY_PAD}
 * (checked against the compiled game, not guessed): the first is the color
 * shown on the item in hand/inventory, the second is the color used for the
 * placed block in the world.
 */
public class LilyPadAccessoryColors {
	public static void initialize() {
		List<Block> tintedGreen = new ArrayList<>(LilyPadAccessories.all());

		// The potted fern is a special case: its plant element has its own
		// tintindex (1) for the real grass/foliage color, on top of the lily
		// pad's own tintindex (0) - so it needs its own two-entry registration
		// instead of the shared one-entry list everything else uses.
		Block fernPot = LilyPadAccessories.pottedFor(Blocks.FERN);
		if (fernPot != null) {
			tintedGreen.remove(fernPot);
			BlockColorRegistry.register(
					List.of(BlockTintSources.constant(0xFF71C35C, 0xFF208030), BlockTintSources.grass()),
					fernPot
			);
		}

		BlockColorRegistry.register(
				List.of(BlockTintSources.constant(0xFF71C35C, 0xFF208030)),
				tintedGreen.toArray(new Block[0])
		);
	}
}
