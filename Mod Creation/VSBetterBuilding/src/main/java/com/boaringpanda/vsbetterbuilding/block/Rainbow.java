package com.boaringpanda.vsbetterbuilding.block;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Dyed blocks the Builder Stick has set fading through every colour like a sheep named jeb_: wool, wool stairs and slabs, carpets, stained
 * glass and panes, beds and banners (cushions are entities: {@code RainbowCushions}). {@code BlockMixin} adds {@link #RAINBOW} to every
 * block {@link #has} accepts, off by default, so hand placement, worldgen and old saves stay exactly as vanilla, and each still drops its
 * own colour.
 * <p>
 * The fade is only a texture: the rainbow models (and the rainbow banner base and cushion the client mixins draw) use animated textures
 * whose 16 frames are the vanilla textures of each colour, in jeb_'s colour order ({@code ColorLerper.Type.SHEEP}), 70 ticks a colour
 * (jeb_ is 25). Every animated texture counts the same client ticks, so everything rainbow fades in step.
 */
public final class Rainbow {
	public static final BooleanProperty RAINBOW = BooleanProperty.create("rainbow");
	/** What follows the colour in the id of every dyed block that can fade (wall banners are banners too; carpets aren't moss ones). */
	private static final Set<String> KINDS = Set.of("wool", "wool_stairs", "wool_slab", "carpet", "stained_glass", "stained_glass_pane", "bed",
			"banner", "wall_banner");

	private Rainbow() {
	}

	/** A vanilla {@code <colour>_<kind>} block of one of the {@link #KINDS}. */
	public static boolean has(@Nullable ResourceKey<Block> id) {
		if (id == null || !id.identifier().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
			return false;
		}
		String path = id.identifier().getPath();
		for (DyeColor color : DyeColor.values()) {
			if (path.startsWith(color.getName() + "_") && KINDS.contains(path.substring(color.getName().length() + 1))) {
				return true;
			}
		}
		return false;
	}
}
