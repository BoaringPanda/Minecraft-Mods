# VSLumberjackMod

Fabric mod (display name "VS Lumberjack Mod"; mod id `vslumberjackmod`, package `com.boaringpanda.vslumberjackmod`, jar
`BPsLumberjackMod-<version>.jar` via `base.archivesName` in build.gradle; renamed from LumberjackMod 2026-10-04) for
lumberjack-themed additions. Sister mod to VSBetterBuilding and VSBetterQOL, set up with the same stack and build
script. Scaffolded 2026-10-01; each feature's `register()` is called from `VSLumberjackMod.onInitialize`.

## Stack
- Minecraft 26.3 (date-based versions, **Mojang mappings**: `net.minecraft.resources.Identifier`, `net.minecraft.client.Minecraft`),
  Fabric Loader 0.19.5, Fabric API 0.160.7+26.3, Loom 1.18.2, Gradle wrapper 9.7.1, Java 25. Versions live in `gradle.properties`.
- Split source sets: `src/main` (common + server) and `src/client` (client only: screens, renderers). Client entrypoint:
  `VSLumberjackModClient` (`src/client`).
- Mixins: `vslumberjackmod.mixins.json` (common, package `mixin/`). Client-only mixins would go in a separate
  `vslumberjackmod.client.mixins.json` with `"environment": "client"`, the way VSBetterQOL does.
- Game source for reading: `./gradlew genSources`, then the `*-sources.jar` files under `.gradle/loom-cache/minecraftMaven/net/minecraft/`.

## Commands
Run from this folder (where `gradlew` is), not the repo root. `JAVA_HOME` must point at the JDK 25 install
(`C:\Program Files\Java\jdk-25.0.2`).
- `./gradlew build` builds the mod and copies the jar into `jars/` (git-ignored, jars never go to GitHub).
- `./gradlew runClient` starts a dev game with the mod.

## Tree Falling enchantment (Dylan, 2026-10-01)
Axe enchant, max level 1. Chopping any natural log of a tree fells the whole tree at once; leaves (and Nether fungus caps) follow in
a quick wave. Book uses the normal enchanted book texture.
- **Data:** `data/vslumberjackmod/enchantment/tree_falling.json` (`supported_items` `#minecraft:axes`, no `effects`: the behaviour is
  Java). Lang key `enchantment.vslumberjackmod.tree_falling`.
- **Obtaining (Dylan's pick):** survival = the woodcutter villager's level 5 trade (62 emeralds → book, see below). Also creative
  Ingredients tab (vanilla lists every registered enchantment), commands, anvil. It's in **no** vanilla enchantment tag
  (`in_enchanting_table`, `tradeable`, `on_random_loot`...), so tables, librarians and loot never give it.
- **`TreeFalling`:** `PlayerBlockBreakEvents.BEFORE` (log still there, placed status still known) finds the tree, `AFTER` chops it.
  - Only when: server player, not in creative (does nothing there, like Efficiency - Dylan's call), not sneaking (sneak = one log), block in `#minecraft:logs`, not player-placed, main hand has the enchant.
  - Flood fill over all 26 neighbours (acacia/cherry branches are diagonal), same wood only (vanilla per-wood tags like
    `minecraft:oak_logs`, so stripped pieces join; unknown modded logs: same block), skipping player-placed logs, max 512.
  - It's a tree only if the group touches natural leaves (not `PERSISTENT`) or non-placed `#vslumberjackmod:fungus_caps`
    (wart blocks + shroomlight). Log builds never fall.
  - Each log goes through `player.gameMode.destroyBlock`: vanilla drops/Fortune/Silk Touch, 1 durability per log (Unbreaking
    applies, Dylan's pick), hunger, stats, protection. Stops when the axe breaks. A `felling` flag keeps those breaks from
    re-triggering. The breaker is sent the 2001 break particles/sound per log (vanilla leaves the breaker out of that broadcast).
- **`FallingCanopy`:** flood fills 7 steps (6 directions) from the felled logs through natural leaves / caps and gives each a
  random 8-30 tick timer (per `ServerLevel`, in memory, ticked on `END_LEVEL_TICK`; VSBetterQOL's fast leaf decay uses the same
  8-30, keep them in sync). When due: a leaf at distance 7 gets
  `state.randomTick` (vanilla's own decay + drops); a natural leaf not at 7 yet is re-checked every 5 ticks until 10 s after the
  fall. Distance spreads one block per tick, and where the canopy touches another tree's leaves it climbs to 7 step by step, which
  took longer than the first 30-tick retry window (Dylan saw those leaves fall at vanilla speed). Leaves still not at 7 after 10 s
  are held up by another tree's log and stay, as in vanilla. A cap block is `destroyBlock`ed unless a natural log/stem is
  within 6 steps through caps (a neighbouring fungus). Vines fall off by vanilla's support rules.
- **`PlacedTreeParts`** (port of VSBetterQOL's `PlacedLogs`, own copy so this mod works alone): persistent per-chunk attachment
  `vslumberjackmod:placed_tree_parts` of player-placed `#vslumberjackmod:tree_parts` positions (`#minecraft:logs` + fungus caps).
  `mixin/BlockItemMixin` remembers after `setPlacedBy` in `BlockItem.place`; `mixin/LevelChunkMixin` forgets when the block at a
  remembered spot stops being a tree part (stripping keeps it). Only tracks from install onwards: builds made before that are still
  protected by the natural-leaves check unless natural leaves touch them.

## Woodcutter (Dylan, 2026-10-01)
A stonecutter for wood. Logs only get stripped; planks turn into their stairs, slabs, fence, fence gate, trapdoor, sign, hanging
sign and shelf (no doors, Dylan's call). **Placeholder look** until Dylan draws pixel art: `models/block/woodcutter.json` is `parent: minecraft:block/stonecutter`
with oak planks top/bottom, stripped oak log sides and the vanilla saw; the GUI and take-result sound are the stonecutter's.
- **Reuses vanilla's stonecutter machinery** (no custom recipe type, so no recipe sync): `WoodcutterBlock extends StonecutterBlock`
  (own menu provider + title `container.vslumberjackmod.woodcutter`, no stonecutter interact stat), `WoodcutterMenu extends
  StonecutterMenu` (own `stillValid` block + `getType()` = `Woodcutter.MENU`), client opens vanilla `StonecutterScreen` for it
  (`VSLumberjackModClient`). Registered in `Woodcutter` (block, item, menu type, creative Functional Blocks after the stonecutter).
- **Recipes** are plain `minecraft:stonecutting` JSONs in `data/vslumberjackmod/recipe/woodcutting/` (129: 13 woods × 8 plank
  outputs, + log/wood/stem/hyphae/bamboo block → stripped). Amounts per plank (Dylan, 2026-10-01): stairs 1, slab 2, fence 1, fence
  gate 2, trapdoor 4, sign 4, hanging sign 2, shelf 2; logs → 1 stripped. Generated by a bash loop that checked every id against the game jar's `assets/minecraft/items/` list.
- **`mixin/StonecutterMenuMixin`** splits the shared recipes by input: `#vslumberjackmod:woodcutter_inputs` (logs, planks, bamboo
  blocks) only go in the woodcutter, everything else only in the stonecutter. Wraps `selectByInput` in `setupRecipeList` (recipe
  list, runs on both sides; item tags are synced) and `acceptsInput` in `quickMoveStack` (shift-click).
- Crafting (Dylan's design): `S I S` / `L # L` - sticks, iron ingot, any `#minecraft:logs`, stone. Wooden block like the crafting
  table (no tool needed, axe fastest via `mineable/axe`, burns in lava), strength 3.5, drops itself.
- Recipe viewers (JEI/EMI) would list these as stonecutter recipes.
- Job block of the woodcutter villager (below).

## Woodcutter villager (Dylan, 2026-10-01)
Profession `vslumberjackmod:woodcutter`, shown in game as **"Lumberjack"** (Dylan renamed it 2026-10-02; only the lang value, the id stays
woodcutter so existing villagers keep the job), job site = the woodcutter block. Doesn't spawn on his own yet (planned: his own lumbermill
structure).
- **`WoodcutterVillager`:** POI via Fabric `PoiHelper` (1 ticket, range 1 like vanilla job sites; listed in
  `data/minecraft/tags/point_of_interest_type/acquirable_job_site.json` so jobless villagers take it), profession registered
  straight into `BuiltInRegistries.VILLAGER_PROFESSION` with the mason's work sound (the stonecutter sound). Lang key
  `entity.vslumberjackmod.villager.woodcutter`.
- **Trades are data** (26.3 vanilla format): one trade per file in `data/vslumberjackmod/villager_trade/woodcutter/<level>/`,
  pools in `tags/villager_trade/woodcutter/level_<n>`, sets in `trade_set/woodcutter/`. `level_<n>` = vanilla's random pick
  (`amount` from the pool); `level_<n>_guaranteed` = always given on top, added by `mixin/VillagerMixin` (a trade set only draws
  from one pool; vanilla's wandering trader also stacks several sets). The mixin only runs for levels that have a `level_<n>`
  set (all 5 do); a guaranteed set needs data only. Files: `<item>_emerald` = player sells, `emerald_<item>` = player buys.
- **Trades (Dylan, 2026-10-01):** "6 woods" = oak/spruce/birch/jungle/acacia/dark oak; a random pick gives one type.

  | Level | Always | Random pick |
  |---|---|---|
  | 1 | 4 apples → 1 emerald | 12 logs (6 woods) → 1 emerald |
  | 2 | 2 emeralds → 4 crimson stems | 1 emerald → 4 leaves (6 woods) |
  | 3 | 2 emeralds → 4 warped stems | 10 saplings (6 woods) → 1 emerald |
  | 4 | 10 leaf litter → 1 emerald | 3 emeralds → 1 poplar / cherry sapling, mangrove propagule, pale oak sapling |
  | 5 | (Dylan plans one more trade) | 62 emeralds → Tree Falling book (`level_5`, single trade, no tag) |

  Uses/xp/discount copied from vanilla mason per level (player-sells/player-buys xp: 2/1, 10/5, 20/10, 30/15; 16 uses for 1-3,
  12 for 4; discount 0.05). The book copies vanilla's librarian enchanted book (12 uses, 30 xp, 0.2 discount); it's a plain
  `stored_enchantments` component on `gives`.
- **Placeholder look:** vanilla mason outfit tinted blue, `textures/entity/villager/profession/woodcutter.png`, same file under
  `zombie_villager/profession/` (vanilla's two mason PNGs are identical). Dylan may draw his own.
