package com.boaringpanda.bettervanillaqol.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.boaringpanda.bettervanillaqol.BetterVanillaQOL;

// Swords are weapons: they only break cobwebs, and can't hit decorations (item frames, armor stands, paintings, ...).
// Any #minecraft:swords item counts, other mods' swords too.
@Mixin(Player.class)
public abstract class PlayerMixin {
	// The only blocks a sword can break (cobwebs).
	private static final TagKey<Block> SWORDS_CAN_BREAK = TagKey.create(Registries.BLOCK, BetterVanillaQOL.id("swords_can_break"));

	// Entities a sword hit does nothing to.
	private static final TagKey<EntityType<?>> SWORDS_CANT_BREAK = TagKey.create(Registries.ENTITY_TYPE, BetterVanillaQOL.id("swords_cant_break"));

	// This is vanilla's adventure-mode "can't break this" check, used by both the client and the server, so a sword on any other
	// block gets no cracks and no break, exactly like adventure mode.
	@ModifyReturnValue(method = "blockActionRestricted", at = @At("RETURN"))
	private boolean bettervanillaqol$swordsOnlyBreakCobwebs(boolean restricted, Level level, BlockPos pos, GameType gameType) {
		return restricted || ((Player) (Object) this).getMainHandItem().is(ItemTags.SWORDS) && !level.getBlockState(pos).is(SWORDS_CAN_BREAK);
	}

	// Player.attack does nothing at all when this is true (client and server), so the item frame keeps its item, the armor stand
	// doesn't wobble and nothing breaks. The swing still animates. Has to answer before vanilla's own check runs, because that
	// check (skipAttackInteraction) is what breaks item frames, paintings and cushions.
	@Inject(method = "cannotAttack", at = @At("HEAD"), cancellable = true)
	private void bettervanillaqol$swordsCantBreakDecorations(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (((Player) (Object) this).getMainHandItem().is(ItemTags.SWORDS) && entity.is(SWORDS_CANT_BREAK)) {
			cir.setReturnValue(true);
		}
	}

	// A sweep (swords only) would still hurt an armor stand or mannequin next to the mob that was hit, so skip those. Returning
	// false ("not hurt") also skips the knockback that follows.
	@WrapOperation(
			method = "doSweepAttack",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean bettervanillaqol$sweepSkipsDecorations(
			LivingEntity nearby, ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		return !nearby.is(SWORDS_CANT_BREAK) && original.call(nearby, level, source, damage);
	}
}
