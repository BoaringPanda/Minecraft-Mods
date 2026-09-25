package com.boaringpanda.bettervanillabuilding.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The block's id, which {@code Blocks.register} sets before the block is built, so {@code BlockMixin} can pick blocks by id. */
@Mixin(BlockBehaviour.Properties.class)
public interface BlockPropertiesAccessor {
	@Accessor("id")
	@Nullable ResourceKey<Block> bettervanillabuilding$getId();
}
