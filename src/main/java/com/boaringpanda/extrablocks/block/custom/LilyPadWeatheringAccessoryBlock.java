package com.boaringpanda.extrablocks.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A decorative copper accessory (currently the unwaxed copper chains) on a lily pad that keeps aging like the
 * real block. Mirrors vanilla's own {@code WeatheringCopperChainBlock} (checked by disassembly). The stages
 * come from Fabric's {@code OxidizableBlocksRegistry} (see {@link com.boaringpanda.extrablocks.block.LilyPadAccessories}),
 * so vanilla's aging, honeycomb waxing and axe scraping all work on it.
 * <p>
 * The copper lanterns aren't registered this way - they stay whatever stage they were placed as - but this
 * class is all it would take if they should age too.
 */
public class LilyPadWeatheringAccessoryBlock extends LilyPadAccessoryBlock implements WeatheringCopper {
	private final WeatherState weatherState;

	/** @param accessory the real vanilla block for this stage - its own stage is this block's too. */
	public LilyPadWeatheringAccessoryBlock(Properties properties, Block accessory) {
		super(properties, accessory);
		this.weatherState = ((WeatheringCopper) accessory).getAge();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		this.changeOverTime(state, level, pos, random);
	}

	/** Only a stage with a next stage ticks - the fully oxidized one has nothing left to become. */
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return WeatheringCopper.getNext(state.getBlock()).isPresent();
	}

	@Override
	public WeatherState getAge() {
		return this.weatherState;
	}
}
