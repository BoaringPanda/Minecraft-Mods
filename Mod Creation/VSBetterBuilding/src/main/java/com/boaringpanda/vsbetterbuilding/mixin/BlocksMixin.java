package com.boaringpanda.vsbetterbuilding.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.boaringpanda.vsbetterbuilding.block.UprightPillarBlock;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registers ancient debris and reinforced deepslate as {@link UprightPillarBlock}s instead of plain blocks, so they have an {@code axis}
 * for the builder stick to turn. Their properties (hardness, sound, loot) are vanilla's own. Every {@code Blocks.register} overload ends
 * in this one.
 */
@Mixin(Blocks.class)
public class BlocksMixin {
	// No static field for the ids: a mixin's static fields are set at the end of Blocks' static init, after every block is registered.
	@ModifyVariable(method = "register(Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Lnet/minecraft/world/level/block/Block;",
			at = @At("HEAD"), argsOnly = true)
	private static Function<BlockBehaviour.Properties, Block> vsbetterbuilding$giveAxis(Function<BlockBehaviour.Properties, Block> factory,
			ResourceKey<Block> id) {
		Identifier name = id.identifier();
		boolean turnable = name.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
				&& (name.getPath().equals("ancient_debris") || name.getPath().equals("reinforced_deepslate"));
		return turnable ? UprightPillarBlock::new : factory;
	}
}
