package com.boaringpanda.extrablocks.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An unwaxed copper lightning rod on a lily pad, which keeps aging like a real one. Mirrors vanilla's own
 * {@code WeatheringLightningRodBlock} (checked by disassembly). The stages themselves come from Fabric's
 * {@code OxidizableBlocksRegistry} (see {@link com.boaringpanda.extrablocks.block.LilyPadAccessories}), so vanilla's
 * aging, honeycomb waxing, axe scraping and lightning's copper-cleaning all treat it like the real rod.
 */
public class LilyPadWeatheringLightningRodBlock extends LilyPadLightningRodBlock implements WeatheringCopper {
	private final WeatherState weatherState;

	/** @param accessory the real vanilla rod for this stage - its own stage is this block's too. */
	public LilyPadWeatheringLightningRodBlock(Properties properties, Block accessory) {
		super(properties, accessory);
		this.weatherState = ((WeatheringCopper) accessory).getAge();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		this.changeOverTime(state, level, pos, random);
	}

	/** Only a stage with a next stage ticks - the fully oxidized rod has nothing left to become. */
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return WeatheringCopper.getNext(state.getBlock()).isPresent();
	}

	@Override
	public WeatherState getAge() {
		return this.weatherState;
	}
}
