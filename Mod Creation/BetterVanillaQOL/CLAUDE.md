# BetterVanillaQOL

Fabric mod (display name "BP's Better Vanilla QOL"; mod id `bettervanillaqol`, package `com.boaringpanda.bettervanillaqol`) of
vanilla quality-of-life tweaks. Sister mod to BetterVanillaBuilding, set up with the same stack and build script. Was
"BetterVanillaFeatures" (`bettervanillafeatures`) until Dylan renamed it 2026-09-30, before any release.

## Stack
- Minecraft 26.3 (date-based versions, **Mojang mappings**: `net.minecraft.resources.Identifier`, `net.minecraft.client.Minecraft`),
  Fabric Loader 0.19.5, Fabric API 0.160.7+26.3, Loom 1.18.2, Gradle wrapper 9.7.1, Java 25. Versions live in `gradle.properties`.
- Split source sets: `src/main` (common + server) and `src/client` (client only: renderers).
- Mixins: `bettervanillaqol.mixins.json` (common, `mixin/`) and `bettervanillaqol.client.mixins.json` (client only,
  `src/client/.../client/mixin/`, listed in `fabric.mod.json` with `"environment": "client"`).
- Game source for reading: `./gradlew genSources`, then the `*-sources.jar` files under `.gradle/loom-cache/minecraftMaven/net/minecraft/`.

## Commands
Run from this folder (where `gradlew` is), not the repo root. `JAVA_HOME` must point at the JDK 25 install
(`C:\Program Files\Java\jdk-25.0.2`).
- `./gradlew build` builds the mod and copies the jar into `jars/` (git-ignored, jars never go to GitHub).
- `./gradlew runClient` starts a dev game with the mod.

## Crops protect farmland (Dylan, 2026-09-26)
Jumping or falling onto farmland with a crop growing on it no longer tramples it to dirt, so the crop doesn't break. Empty farmland
still tramples like vanilla.
- `mixin/FarmlandBlockMixin` `@WrapWithCondition`s the `turnToBaseBlock` call in `FarmlandBlock.fallOn`, so only the trampling is
  skipped (fall damage and the rest of `fallOn` still run). No config or tag controls trampling in vanilla, hence the mixin.
- Which blocks protect it: block tag `bettervanillaqol:protects_farmland` (`data/bettervanillaqol/tags/block/`):
  `#minecraft:crops` (so other mods' crops count too) plus the grown stages vanilla's tag leaves out: attached pumpkin/melon stems
  and a grown torchflower.

## No tempt delay (Dylan, 2026-09-26)
Animals follow food the moment it's held, and again straight after it's put away and taken back out (vanilla waits 5 s). All tempting
mobs, which includes strider and sulfur cube since they use `TemptGoal` too. Vanilla had three delays:
- `mixin/TemptGoalMixin` (goal-based: chicken, cow, pig, sheep, rabbit, horses, llamas, panda, turtle, bee, cat, ocelot, strider, sulfur
  cube): `TemptGoal.stop` sets `calmDown` to 0 instead of `reducedTickDelay(100)`. **Exception, Dylan's pick:** a spooked wild ocelot
  or stray cat (player moved/turned fast within 6 blocks, `canScare`) keeps the vanilla wait, since standing still is how they're
  tamed. Spook detection: `canContinueToUse` sets `spooked` at HEAD, and the wrapped `canUse()` call in it clears it. Only the spook
  checks return before that call, so `spooked` is still true in `stop` only after a spook.
- `mixin/FollowTemptationMixin` (brain-based: goat, camel, frog, tadpole, axolotl, armadillo, sniffer, happy ghast, nautiluses):
  `FollowTemptation.stop` never sets `TEMPTATION_COOLDOWN_TICKS`. The `CountDownCooldownTicks` behaviours in the `*Ai` classes then
  have nothing to count.
- `mixin/TemptingSensorMixin` + `mixin/SensorAccessor`: `TemptingSensor` scans every tick instead of `Sensor`'s default 20, so brain
  animals notice food at once, like goal-based ones (checked every 2 ticks).

## Pickaxes break glass faster (Dylan, 2026-09-26)
All glass and glass panes (plain, stained, tinted) break faster with a pickaxe. Wooden is 2x hand speed and each tier is 1.5x the one
before: wood 2, stone 3, copper 3.5, iron 4.5, diamond 6.75, netherite 10.125 (instant), and gold matches netherite. Drops don't change
(still Silk Touch only).
- `ToolSpeedRules` uses Fabric's `DefaultItemComponentEvents.MODIFY` to put a `Tool.Rule.overrideSpeed` in front of each vanilla
  pickaxe's `minecraft:tool` rules (speed only, like vanilla's sword-on-cobweb rule), so Efficiency, Haste etc. still stack through
  vanilla's mining code. No mixin.
- Which blocks: block tag `bettervanillaqol:breaks_faster_with_pickaxe` = `#c:glass_blocks` + `#c:glass_panes` (Fabric convention
  tags, so other mods' glass counts too if they tag it). Only the 7 vanilla pickaxes get it, because item tags aren't loaded when the
  event runs. The same class adds the axe bamboo rule (below).

## Rename name tags in hand (Dylan, 2026-09-27)
Right-clicking with a name tag opens a rename screen (text box, Accept, Cancel). Accept renames the tag (the whole stack, like the
anvil) and uses one ink sac from anywhere in the inventory. Creative needs no ink sac. With no ink sac the screen doesn't open and the
actionbar says "Ink sac needed" (Dylan's pick). Accept is greyed out for a blank or unchanged name, so names can't be removed this way
(the anvil still does that).
- Client: `BetterVanillaQOLClient` registers a `UseItemCallback` (client side only; it also fires for the singleplayer server).
  "Use item" only runs after mob/block interactions, so a named tag still names a mob and chests still open. Does nothing (vanilla
  behaviour) if the server can't receive the packet, i.e. doesn't have the mod. `NameTagScreen` is modelled on vanilla's
  `DirectJoinServerScreen`.
- Server: `NameTagRenaming` registers the `rename_name_tag` payload (hand + name) and re-checks everything: name tag in that hand,
  `canRename` (creative or has an ink sac), anvil name rules (`StringUtil.filterText`, not blank, max 50, text filter). Plays
  `INK_SAC_USE` via `level().playSound(null, ...)` (`Player.playSound` would skip the player who renamed).
- Text: `assets/bettervanillaqol/lang/en_us.json`.

## Swords are weapons, axes cut bamboo (Dylan, 2026-09-27)
Swords hit mobs through grass/flowers/small plants, can't break any block except cobwebs, and axes break bamboo instantly instead.
All `#minecraft:swords` items count (other mods' swords too).
- Can't break: `mixin/PlayerMixin` `@ModifyReturnValue`s `Player.blockActionRestricted` (vanilla's adventure-mode check, used by
  client and server start/continue/destroy) to also return true for a sword on anything outside block tag
  `bettervanillaqol:swords_can_break` (= cobweb). So a sword gets no cracks and no break, like adventure mode. The sword's vanilla
  bamboo/leaves speed rules are never reached. Creative: vanilla swords break nothing there (`Tool.canDestroyBlocksInCreative`
  false), so `ToolSpeedRules` sets it true on the 7 vanilla swords (item tags aren't loaded for that event) and cobwebs break in
  creative too; everything else is still stopped by `blockActionRestricted`, which vanilla checks before the creative break.
- Can't hit decorations: `PlayerMixin` also `@Inject`s at HEAD of private `Player.cannotAttack` (checked at the top of
  `Player.attack`, client and server) for a sword on entity-type tag `bettervanillaqol:swords_cant_break` (cushion, painting,
  item frame, glow item frame, armor stand, mannequin), so the hit does nothing at all (no item pop, no wobble, no break). HEAD,
  not the return value, because vanilla's `skipAttackInteraction` inside that check is what breaks item frames, paintings and
  cushions. A `@WrapOperation` on `hurtServer` in `doSweepAttack` returns false for them, so sweeps don't hurt or knock them back
  either. Creative included. They still block the crosshair (no hit-through). Hand/other tools are vanilla.
- Hit through: `client/mixin/LocalPlayerMixin` on `LocalPlayer.pick` (the crosshair pick: nearest block first, then entities only
  closer than it). With a sword, the `Entity.pick` block ray is swapped for a clip that treats block tag
  `bettervanillaqol:swords_hit_through` as empty, so vanilla's own entity search reaches the mob behind the plant. If no
  entity is found, the normal (plant) block hit is returned, so the outline is vanilla. Client only: the server's `handleAttack`
  only checks distance, not line of sight. The tag: grass/ferns (incl. dry grass), `#minecraft:small_flowers` + tall flowers,
  petals/wildflowers, bushes, leaf litter, `#minecraft:saplings`, `#minecraft:crops`, sweet berries, sugar cane, seagrass, nether
  roots/sprouts/fungi/wart, weeping + twisting vines (both the tip and plant blocks), hanging roots, mushrooms. Wither rose
  comes in via small_flowers. Also glow berry vines (`#minecraft:cave_vines`), vine, pale hanging moss, spore blossom, cactus
  flower, kelp (+ kelp_plant), and all coral plants/fans/wall fans, live (`#minecraft:corals`, `#minecraft:wall_corals`) and dead
  (listed one by one, vanilla has no dead-coral tag). Coral blocks are left out (solid blocks). Plus red shrub, big dripleaf
  (+ its stem) and small dripleaf.
- Bamboo: `ToolSpeedRules` puts `Tool.Rule.overrideSpeed(#bettervanillaqol:axe_instantly_mines, Float.MAX_VALUE)` (the rule
  vanilla's sword has) in front of the 7 vanilla axes' rules. Tag = bamboo + bamboo sapling. Speed only, drops unchanged.

## Right-click harvest (Dylan, 2026-09-27)
Right-clicking a fully grown crop drops what breaking it would (Fortune on the held item counts) and replants it at stage 0. Wheat,
carrots, potatoes, beetroots (any `CropBlock`, so other mods' crops too), nether wart and cocoa take the replant seed out of their own
drops. A grown pitcher plant and torchflower drop no seed, so they use a pitcher pod / torchflower seed from anywhere in the inventory
(creative: free); with none, you still get the flower and the spot is left empty (Dylan's pick). Torchflowers are only picked where a
torchflower seed could be planted (farmland), so decorative ones aren't touched.
- `mixin/BlockBehaviourMixin` `@Inject`s at HEAD of `BlockBehaviour.useWithoutItem`, the step sweet berry bushes are picked in. None of
  these crops override it. Vanilla only calls it for the main hand, not while sneaking with an item, and before the held item's use,
  so all of that works like sweet berries. Client just returns SUCCESS (arm swing); the server does the work.
- `CropHarvesting` finds the harvest (pitcher: either half, drops come from the lower one) and does the drops
  (`Block.getDrops` with player + main-hand item), `spawnDestroyParticles` (vanilla break sound + particles), replant and
  `GameEvent.BLOCK_CHANGE`. Pitcher's lower half is replanted before the upper is removed, because vanilla breaks a grown lower half
  whose top goes missing. No tool damage (vanilla doesn't damage hoes on crops).

## Fast leaf decay (Dylan, 2026-09-27)
Leaves cut off from their logs (whole trunk broken) are all gone within 3 s, in a random wave, instead of waiting for a random tick
(~68 s on average in vanilla). Drops are vanilla's. Player-placed leaves are `PERSISTENT`, so vanilla's `decaying()` is false and
they never decay, same as vanilla (custom trees are safe). Which blocks hold leaves up is vanilla's `#minecraft:prevents_nearby_leaf_decay`.
- `mixin/LeavesBlockMixin` on `LeavesBlock.tick` (the scheduled tick vanilla uses to recompute `DISTANCE`, which spreads through the
  tree at one block per tick). TAIL: if the leaf is now `decaying()`, schedule another tick 2-50 ticks later. HEAD: if it's still
  `decaying()` after a fresh `updateDistance` (both `@Shadow`ed), `dropResources` + `removeBlock` like `randomTick`, and cancel.
  Only a natural log growing next to it in the meantime saves it. Only one tick per pos+block can be pending (`LevelChunkTicks.schedule`),
  so neighbours' 1-tick updates can't bring the decay forward.
- **Player-placed logs never hold up natural leaves** (Dylan's pick, placed before or after the trunk is broken). `PlacedLogs` keeps a
  per-chunk set of placed-log positions (`BlockPos.asLong`) as a persistent Fabric data attachment `bettervanillaqol:placed_logs`
  (immutable set, always replaced so `setAttached` marks the chunk unsaved; empty → `removeAttached`). Server only.
  - Remember: `mixin/BlockItemMixin` wraps the `setPlacedBy` call in `BlockItem.place` (only reached after a real placement).
  - Forget: `mixin/LevelChunkMixin` wraps `LevelChunkSection.setBlockState` in `LevelChunk.setBlockState` (every block change in a
    loaded chunk): old state a log and new state not → forget. So no stale spots (a sapling growing where a placed log burned is
    natural). Log → log (axe stripping) stays remembered.
  - Leaves: `LeavesBlockMixin` wraps `getDistanceAt` inside `updateDistance` (`@Local` the `MutableBlockPos` neighbour) and returns
    `DECAY_DISTANCE` for a remembered log, so it counts as nothing.
  - Limits: logs placed before this feature, via `/setblock` `/fill` WorldEdit, or pushed by a piston count as natural.

## Shovel turns paths back to dirt (Dylan, 2026-09-27)
Right-clicking a dirt path with a shovel turns it back into dirt, the reverse of making a path: same flatten sound, 1 durability, not
from the bottom face, and only with air above (all like vanilla's path-making). All shovels, since it's vanilla's own shovel action.
- No code: shovel right-click is data in 26.x. Shovels carry the `minecraft:block_transformer` item component pointing at the
  `minecraft:shovel` entry of the `block_transformer` data registry. The mod ships its own `data/minecraft/block_transformer/shovel.json`
  (overrides vanilla's): vanilla's rule copied as-is plus a second rule, `minecraft:dirt_path` + air above -> `minecraft:dirt`.
  If vanilla changes its `shovel.json` in an update, copy the change into ours (it replaces vanilla's whole file).
