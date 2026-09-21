package com.boaringpanda.bpsbettervanillabuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.boaringpanda.bpsbettervanillabuilding.block.custom.MixedSlabBlock;

/**
 * Plays the place sound of the slab that was just placed when it makes a {@link MixedSlabBlock}, instead of the combined
 * block's. Vanilla's {@code BlockItem.place} plays the placed <em>block state's</em> place sound (via {@code getPlaceSound}),
 * and a combined block is one block that can only have one sound, so without this a stone slab placed on a wood slab
 * made the wood sound. {@code this.getBlock()} here is the block of the item in the player's hand, which is exactly the slab
 * that was placed, so it is the sound to play.
 * <p>
 * Common code, since the placing client plays its own sound and the server plays it for everyone nearby. Anything that isn't a
 * combined block being placed from one of its own two slabs falls through to vanilla. Volume and pitch still come from the
 * combined block's sound type, which is close to the same for every slab.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "getPlaceSound", at = @At("HEAD"), cancellable = true)
	private void bpsbettervanillabuilding$placeSoundOfThePlacedSlab(BlockState state, CallbackInfoReturnable<SoundEvent> cir) {
		Block held = ((BlockItem) (Object) this).getBlock();
		if (state.getBlock() instanceof MixedSlabBlock combo && (held == combo.bottomSlab() || held == combo.topSlab())) {
			cir.setReturnValue(held.defaultBlockState().getSoundType().getPlaceSound());
		}
	}
}
