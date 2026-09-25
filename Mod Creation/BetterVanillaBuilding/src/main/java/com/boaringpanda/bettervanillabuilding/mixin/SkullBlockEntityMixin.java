package com.boaringpanda.bettervanillabuilding.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.boaringpanda.bettervanillabuilding.block.StackedHeads;
import com.boaringpanda.bettervanillabuilding.block.TopHeadHolder;

/**
 * A head's block entity also holds the head stacked on top of it ({@link StackedHeads}), saved with the block. Vanilla's update tag is
 * this same saved data, so clients get it too.
 */
@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin implements TopHeadHolder {
	@Unique
	private StackedHeads.@Nullable Head bettervanillabuilding$topHead;

	@Override
	public StackedHeads.@Nullable Head bettervanillabuilding$getTopHead() {
		return bettervanillabuilding$topHead;
	}

	@Override
	public void bettervanillabuilding$setTopHead(StackedHeads.@Nullable Head top) {
		bettervanillabuilding$topHead = top;
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void bettervanillabuilding$saveTopHead(ValueOutput output, CallbackInfo ci) {
		output.storeNullable(StackedHeads.TAG, StackedHeads.Head.CODEC, bettervanillabuilding$topHead);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void bettervanillabuilding$loadTopHead(ValueInput input, CallbackInfo ci) {
		bettervanillabuilding$topHead = input.read(StackedHeads.TAG, StackedHeads.Head.CODEC).orElse(null);
	}
}
