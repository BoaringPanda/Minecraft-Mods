package com.boaringpanda.vslumberjackmod;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Shape, facing and rotation are the stonecutter's; only the menu differs.
public class WoodcutterBlock extends StonecutterBlock {
	private static final Component CONTAINER_TITLE = Component.translatable("container.vslumberjackmod.woodcutter");

	public WoodcutterBlock(Properties properties) {
		super(properties);
	}

	// The stonecutter's version also counts the stonecutter's "interactions" stat, which would be wrong here.
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.openMenu(state.getMenuProvider(level, pos));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider(
				(containerId, inventory, player) -> new WoodcutterMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), CONTAINER_TITLE);
	}
}
