package com.boaringpanda.vsbetterbuilding.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.boaringpanda.vsbetterbuilding.block.StackedHeads;
import com.boaringpanda.vsbetterbuilding.block.TopHeadHolder;

/**
 * A head's block entity also holds the head stacked on top of it ({@link StackedHeads}), saved with the block. Vanilla's update tag is
 * this same saved data, so clients get it too.
 */
@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin implements TopHeadHolder {
	@Unique
	private StackedHeads.@Nullable Head vsbetterbuilding$topHead;

	@Override
	public StackedHeads.@Nullable Head vsbetterbuilding$getTopHead() {
		return vsbetterbuilding$topHead;
	}

	@Override
	public void vsbetterbuilding$setTopHead(StackedHeads.@Nullable Head top) {
		vsbetterbuilding$topHead = top;
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void vsbetterbuilding$saveTopHead(ValueOutput output, CallbackInfo ci) {
		output.storeNullable(StackedHeads.TAG, StackedHeads.Head.CODEC, vsbetterbuilding$topHead);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void vsbetterbuilding$loadTopHead(ValueInput input, CallbackInfo ci) {
		vsbetterbuilding$topHead = input.read(StackedHeads.TAG, StackedHeads.Head.CODEC).orElse(null);
	}
}
