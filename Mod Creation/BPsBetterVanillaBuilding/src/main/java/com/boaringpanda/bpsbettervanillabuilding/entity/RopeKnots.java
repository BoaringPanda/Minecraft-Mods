package com.boaringpanda.bpsbettervanillabuilding.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.boaringpanda.bpsbettervanillabuilding.BPsBetterVanillaBuilding;

/**
 * Registers the rope knot entity. Sized and tracked like vanilla's own leash knot, but <b>saved</b> (vanilla's knot isn't, since a
 * leashed animal recreates it), because a rope knot carries the rope. Not summonable, since it only makes sense on a fence.
 */
public class RopeKnots {
	public static final EntityType<RopeKnotEntity> ROPE_KNOT = register();

	private static EntityType<RopeKnotEntity> register() {
		Identifier id = BPsBetterVanillaBuilding.id("rope_knot");
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		EntityType<RopeKnotEntity> type = EntityType.Builder.<RopeKnotEntity>of(RopeKnotEntity::new, MobCategory.MISC)
				.noSummon()
				.sized(0.375F, 0.5F)
				.clientTrackingRange(10)
				.build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
	}

	/** Loading the class is what registers the type; this just makes the call site say so. */
	public static void initialize() {
	}
}
