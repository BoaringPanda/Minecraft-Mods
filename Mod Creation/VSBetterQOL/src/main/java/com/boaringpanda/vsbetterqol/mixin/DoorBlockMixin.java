package com.boaringpanda.vsbetterqol.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;

import com.boaringpanda.vsbetterqol.ServerConfig;

// Right-clicking one door of a double door opens or closes the other one with it (sneak-click: just the one). Any two hand-openable
// doors count, facing the same way with hinges on opposite sides, which is how vanilla hinges a door placed next to another.
// Follows ServerConfig's double_doors (the client side goes by what the server sent, so both sides move the same doors).
@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {
	// TAIL is the final return, reached only after vanilla toggled a hand-openable door (iron doors return PASS earlier).
	@Inject(method = "useWithoutItem", at = @At("TAIL"))
	private void vsbetterqol$openPartner(BlockState oldState, Level level, BlockPos pos, Player player, BlockHitResult hitResult,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (player.isSecondaryUseActive() || !ServerConfig.on(ServerConfig.DOUBLE_DOORS, level)) {
			return;
		}

		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof DoorBlock)) {
			return;
		}
		// A door placed with another door on its counter-clockwise side gets a right hinge (DoorBlock.getHinge).
		Direction facing = state.getValue(DoorBlock.FACING);
		DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
		BlockPos partnerPos = pos.relative(hinge == DoorHingeSide.RIGHT ? facing.getCounterClockWise() : facing.getClockWise());
		BlockState partner = level.getBlockState(partnerPos);
		if (partner.getBlock() instanceof DoorBlock partnerDoor
				&& partnerDoor.type().canOpenByHand()
				&& partner.getValue(DoorBlock.FACING) == facing
				&& partner.getValue(DoorBlock.HALF) == state.getValue(DoorBlock.HALF)
				&& partner.getValue(DoorBlock.HINGE) != hinge) {
			partnerDoor.setOpen(player, level, partner, partnerPos, state.getValue(DoorBlock.OPEN));
		}
	}
}
