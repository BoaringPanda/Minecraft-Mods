package com.boaringpanda.vsbetterqol.client.mixin;

import java.util.Arrays;
import java.util.Map;

import com.boaringpanda.vsbetterqol.VSBetterQOL;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

// Each worn armor piece tints the armor points it gives in its material's colour (1 armor = half an icon), helmet first from the
// left like vanilla fills the bar. Points from anything else (modded armor, /attribute) stay vanilla.
@Mixin(Hud.class)
public abstract class ArmorBarMixin {
	@Unique
	private static final Identifier ARMOR_LEFT = VSBetterQOL.id("hud/armor_left");
	@Unique
	private static final Identifier ARMOR_RIGHT = VSBetterQOL.id("hud/armor_right");
	@Unique
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	@Unique
	private static final int UNTINTED = -1;
	// The sprites' body is white, so these are the colours that show. Dyed leather stays the normal leather brown.
	@Unique
	private static final Map<ResourceKey<EquipmentAsset>, Integer> COLORS = Map.of(
			EquipmentAssets.LEATHER, DyedItemColor.LEATHER_COLOR,
			EquipmentAssets.CHAINMAIL, 0x7D7D85,
			EquipmentAssets.IRON, 0xB8B9C4,
			EquipmentAssets.GOLD, 0xFFD83D,
			EquipmentAssets.DIAMOND, 0x4AEDD9,
			EquipmentAssets.NETHERITE, 0x625A62,
			EquipmentAssets.TURTLE_SCUTE, 0x5FC85F,
			EquipmentAssets.COPPER, 0xF2945E);
	// Waiting longer than this means the armor value isn't going to change, so the new colours are shown anyway.
	@Unique
	private static final long SYNC_TIMEOUT_MS = 500L;

	// Armor is only recalculated on the server, so after a piece is put on, the client's armor value (and vanilla's bar) lags a tick
	// or two behind the worn pieces. The colours last drawn are kept until it catches up, so they change in the same frame as the bar.
	@Unique
	private static int[] vsbetterqol$syncedColors = new int[0];
	@Unique
	private static int vsbetterqol$syncedItemArmor;
	@Unique
	private static int vsbetterqol$syncedArmor = -1;
	@Unique
	private static long vsbetterqol$waitingSince = -1L;

	// Same spots as vanilla's icons. Only reached with armor above 0, vanilla returns before the tail otherwise.
	@Inject(method = "extractArmor", at = @At("TAIL"))
	private static void vsbetterqol$tintArmor(GuiGraphicsExtractor graphics, Player player, int yLineBase, int numHealthRows, int healthRowHeight, int xLeft, CallbackInfo ci) {
		int armor = player.getArmorValue();
		int[] colors = new int[Math.min(armor, 20)];
		int itemArmor = 0;
		for (EquipmentSlot slot : SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			int start = itemArmor;
			itemArmor += vsbetterqol$armor(stack, slot);
			// Untinted pieces still take up their points, so the next piece's colour lands on its own points.
			Arrays.fill(colors, Math.min(start, colors.length), Math.min(itemArmor, colors.length), vsbetterqol$color(stack));
		}
		Arrays.fill(colors, Math.min(itemArmor, colors.length), colors.length, UNTINTED);

		long now = Util.getMillis();
		boolean waiting = itemArmor != vsbetterqol$syncedItemArmor && armor == vsbetterqol$syncedArmor;
		if (!waiting) {
			vsbetterqol$waitingSince = -1L;
		} else if (vsbetterqol$waitingSince < 0L) {
			vsbetterqol$waitingSince = now;
		}
		if (!waiting || now - vsbetterqol$waitingSince > SYNC_TIMEOUT_MS) {
			vsbetterqol$syncedColors = colors;
			vsbetterqol$syncedItemArmor = itemArmor;
			vsbetterqol$syncedArmor = armor;
		}

		int y = yLineBase - (numHealthRows - 1) * healthRowHeight - 10;
		for (int point = 0; point < vsbetterqol$syncedColors.length; point++) {
			int color = vsbetterqol$syncedColors[point];
			if (color != UNTINTED) {
				Identifier half = point % 2 == 0 ? ARMOR_LEFT : ARMOR_RIGHT;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, half, xLeft + point / 2 * 8, y, 9, 9, 0xFF000000 | color);
			}
		}
	}

	// The armor this piece adds in this slot, read from the same modifiers vanilla's armor value comes from.
	@Unique
	private static int vsbetterqol$armor(ItemStack stack, EquipmentSlot slot) {
		double[] armor = {0.0};
		stack.forEachModifier(slot, (attribute, modifier) -> {
			if (attribute.equals(Attributes.ARMOR) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				armor[0] += modifier.amount();
			}
		});
		return Math.max((int) armor[0], 0);
	}

	@Unique
	private static int vsbetterqol$color(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null || equippable.assetId().isEmpty()) {
			return UNTINTED;
		}
		return COLORS.getOrDefault(equippable.assetId().get(), UNTINTED);
	}
}
