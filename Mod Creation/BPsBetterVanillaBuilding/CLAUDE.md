# BP's Better Vanilla Building — Fabric mod

## Stack
- Minecraft **26.3** (date-based versioning; the 1.x scheme ended at 1.21.11)
- Fabric Loader 0.19.5, Fabric API `0.160.7+26.3`, Loom `1.18.2`, Gradle 9.7.1
- **Java 25** — `sourceCompatibility`/`targetCompatibility` and mixin `compatibilityLevel` are all `JAVA_25`
- **Mojang official mappings (mojmap), not Yarn.** Yarn stopped at 1.21.11. Class names follow
  mojmap: `net.minecraft.client.Minecraft` (not `MinecraftClient`),
  `net.minecraft.resources.Identifier`, etc. Do not copy Yarn names from old tutorials.

## Layout
`splitEnvironmentSourceSets()` is on, so there are two source sets:

- `src/main/` — common code, runs on client **and** dedicated server.
  Entrypoint: `com.boaringpanda.extrablocks.ExtraBlocks`
- `src/client/` — client-only code (rendering, screens, keybinds). Never referenced from `src/main`.
  Entrypoint: `com.boaringpanda.extrablocks.client.ExtraBlocksClient`

Registering blocks/items/recipes = `src/main`. Anything touching `Minecraft.getInstance()`,
rendering or GUI = `src/client`.

## Conventions
- The mod is called **BP's Better Vanilla Building**. That's the `name` in `fabric.mod.json`, and the jar is
  `BPsBetterVanillaBuilding-<version>.jar`, named by `rootProject.name` in `settings.gradle`. It started out as
  "Extra Blocks", and the internal names **deliberately keep that**: the mod ID `extra_blocks`, the `assets/extra_blocks/`
  folder, the package and the project folder. Worlds store the mod's blocks by ID, so renaming the ID would
  turn every placed lily pad accessory and mixed slab into air. Only rename it if the user accepts losing them.
- Mod ID is `extra_blocks` (snake_case). Java package is `com.boaringpanda.extrablocks` (no underscore).
- Build an `Identifier` with the helper `ExtraBlocks.id("some_path")` rather than by hand.
- Assets live under `src/main/resources/assets/extra_blocks/`.
- Data (recipes, loot tables, tags) under `src/main/resources/data/extra_blocks/`.
- The mod has two mixins:
  - `client/mixin/ClientLevelMixin` (client-only) makes mining particles and hit sounds come from the part of
    a lily pad combo being mined. See "Particles, sounds and middle-click come from the part you aim at".
    Its config is `src/client/resources/extra_blocks.client.mixins.json`.
  - `mixin/PoiTypesInvoker` (common) lets lily-pad lightning rods register as vanilla's lightning-rod point
    of interest. See "Lightning rods and chains on a lily pad". Its config is
    `src/main/resources/extra_blocks.mixins.json`.

  Neither has a Fabric hook to use instead, and both sections explain why. Both configs are listed under
  `"mixins"` in `fabric.mod.json`, the client one with `"environment": "client"`. A new config has to be
  listed there too, or it won't be applied. Prefer the Fabric API event hooks over a mixin when one exists.
  MixinExtras (`@ModifyExpressionValue`, `@Local`, ...) comes with Fabric Loader and is already on the classpath.
- **Tabs, not spaces** (matches the Fabric codestyle).

## Registering blocks/items

For a plain standalone block with an item (one a player can hold and place from their
inventory): one static field per block, a private `register()` helper that does
`Registry.register` for both the block and its `BlockItem`, plus a public `initialize()` that's
called once from `ExtraBlocks.onInitialize()` — that call is what forces the class to load and
the registration to actually run. `block/MixedSlabBlocks.java`'s `register()` is close to this
shape, minus the `BlockItem` half — these combo blocks are deliberately never held/placed
directly, so there's no current example in this repo that registers both; add the `BlockItem`
half back in following the pattern fabric-docs uses (`Registry.register` on
`BuiltInRegistries.ITEM` with a `new BlockItem(block, ...)`) if a future block needs one.

Per-block assets needed for a block to look and behave right in-game (all keyed by the same
block ID):

- `assets/extra_blocks/blockstates/<id>.json` — which model to render
- `assets/extra_blocks/models/block/<id>.json` — usually `"parent": "minecraft:block/cube_all"`
  + a texture reference
- `assets/extra_blocks/textures/block/<id>.png` — the actual texture; missing = purple/black
  checkerboard placeholder, everything else still works
- `assets/extra_blocks/items/<id>.json` — the client item, so it renders in inventory/hand
- `assets/extra_blocks/lang/en_us.json` — `"block.extra_blocks.<id>": "Display Name"`
- `data/extra_blocks/loot_tables/blocks/<id>.json` — drop itself when broken (otherwise no drop)
- `data/minecraft/tags/block/mineable/<tool>.json` — add `"extra_blocks:<id>"` to `pickaxe`/`axe`/
  `shovel`/`hoe` so the intended tool is effective (`values` is a flat list of item IDs)

## Mixed slabs (combine two different slabs into one block)

Right-clicking a placed single slab's exposed flat face with a *different* slab item
combines them into one full-size `MixedSlabBlock` — bottom material on the bottom half,
top material on top. Same click spot as merging two same-type slabs into a vanilla
double slab (`Direction.UP` on a bottom half, `Direction.DOWN` on a top half); a side-face
click is left alone so vanilla just places the new slab in the adjacent space as usual.

- `block/custom/MixedSlabBlock.java` — the block itself. Purely visual: no baked model
  of its own, `RenderShape` stays the default (`MODEL`) and the blockstate JSON layers two
  *existing vanilla* half-slab models on top of each other via `multipart` (see below) — no
  BlockEntity, no custom renderer needed. `strength`/`sound` are one generic value shared by
  every combo (not per-material); `requiresCorrectToolForDrops()` is always on, per the pickaxe
  rule below.
- `block/MixedSlabBlocks.java` — the `MATERIALS` list (every vanilla slab, currently 101 - one
  entry per `assets/minecraft/blockstates/*_slab.json` in the game, id = filename minus
  `_slab`) and the registration loop. Generates one block per *ordered* pair (order matters —
  oak-bottom/stone-top ≠ stone-bottom/oak-top), skipping a material paired with itself: 101×100
  = 10,100 blocks. Each material is resolved to its actual `Block` via a registry lookup on
  `minecraft:<material>_slab`, **not** a `Blocks.*` constant — wool, concrete and copper slabs
  aren't individual `Blocks` fields (they're `ColorCollection`/`WeatheringCopperCollection`
  entries), so a lookup by ID is what handles every material uniformly without needing to know
  which. No `BlockItem` — these are never obtainable directly, only ever the result of combining
  two slabs.
- `block/MixedSlabInteraction.java` — the `UseBlockCallback` that detects the combine click
  and swaps the single slab for the right registered combo block.

**Don't assume a slab's model is named `<material>_slab`/`<material>_slab_top` — check.** True
for most, but not all: waxed copper slabs (`waxed_oxidized_cut_copper_slab`, etc.) reuse their
*unwaxed* model verbatim (waxing doesn't change appearance), so
`waxed_oxidized_cut_copper_slab.json`'s own blockstate points at
`minecraft:block/oxidized_cut_copper_slab` / `..._top`, not at a `waxed_oxidized_...` model that
doesn't exist. Generating this from an assumed naming pattern silently produces a block that
looks broken for exactly the materials where the assumption is wrong. Instead, read each real
material's own blockstate JSON and take its `type=bottom`/`type=top` model paths verbatim:

```
grep -A1 '"type=bottom"' assets/minecraft/blockstates/<material>_slab.json | grep model
grep -A1 '"type=top"'    assets/minecraft/blockstates/<material>_slab.json | grep model
```

**To add a material** (a modded slab, or a future vanilla one not on the list yet): add its id
to `MATERIALS` in `MixedSlabBlocks.java`, then regenerate for every OTHER existing material
(both orders) - one blockstate file per new pairing at
`assets/extra_blocks/blockstates/mixed_slab_<bottom>_bottom_<top>_top.json`, using each
material's *real* extracted bottom/top model paths (see above), e.g.:
```json
{
  "multipart": [
    { "apply": { "model": "<bottom material's own type=bottom model>" } },
    { "apply": { "model": "<top material's own type=top model>" } }
  ]
}
```
and add `"extra_blocks:mixed_slab_<bottom>_bottom_<top>_top"` to
`data/minecraft/tags/block/mineable/pickaxe.json` for each new pairing.

**This does not reach modded slabs automatically.** `MATERIALS` is a fixed list decided at
build time, not a live scan of the block registry — a slab added later by installing another
mod won't get mixed-slab support until it's added here and the game is rebuilt.

At the current 101-material scale (10,100 generated blockstate files, ~700KB tag file), a
manual per-file loop like the one above is far too slow to run one-by-one - script the
generation (load each material's id + extracted model paths into parallel arrays/a lookup, loop
the pairs) rather than looping `grep` per material one at a time.

**Startup time cost:** this scale measurably slows the dev client's first launch after a
rebuild - about 65s from process start to being in-world in testing (roughly 2× a plain
Fabric+Fabric-API load), from registering/loading ~10,100 extra blocks and resources. Not a
correctness problem, just worth knowing before assuming a slow launch is a bug.

## Lily pad accessories (torch/lantern standing on a lily pad)

Same technique as mixed slabs, much smaller scope (currently 2 combos, not 240): right-clicking
a placed lily pad's top face with a torch or lantern combines them into one
`LilyPadAccessoryBlock` occupying the lily pad's own space, instead of the item placing normally
in the block above.

- `block/custom/LilyPadAccessoryBlock.java` — the block. No custom renderer; the blockstate
  JSON points at one hand-built model per combo (`models/block/lily_pad_with_<name>.json`) whose
  `elements` are just the vanilla `lily_pad` element and the accessory's own template elements
  copied in side by side (see "Building the combined model" below - **not** a `multipart` of two
  separate model files; that was tried and reverted, see the rotation note). Collision/outline
  shape is copied from the real vanilla `LilyPadBlock` (`Block.column(14.0, 0.0, 1.5)` — verified
  from the compiled class, not guessed) so it stands on exactly like a normal lily pad.
- `block/LilyPadAccessories.java` — registers one block per accessory and the
  `Block -> LilyPadAccessoryBlock` lookup. Light level is copied from the accessory's own
  vanilla value; hardness and tool requirement are *not* (see below - anything on a lily pad
  is instant-break, no tool required, regardless of what the accessory itself normally needs).
- `block/LilyPadAccessoryInteraction.java` — the `UseBlockCallback` that detects the combine
  click (top face only) and swaps the lily pad for the right combo block.

**Every combo block needs the lily pad's green tint registered separately, client-side** -
`client/block/LilyPadAccessoryColors.java`, called from `ExtraBlocksClient`. The lily pad model's
`tintindex` is per-*model*, but the actual tint *color* is looked up per-*Block instance* by the
renderer, so a new combo block with no registration for it just renders with no tint at all (a
flat white/grey pad instead of green) - this happened for real, more than once, when a hand-typed
list of blocks there fell behind as more accessories were added. Fixed structurally, not just
patched: `LilyPadAccessories.all()` returns every registered combo block regardless of category,
and `LilyPadAccessoryColors` iterates that instead of naming blocks - so a new accessory is
covered automatically as long as it's registered through the normal `register`/`registerSimple`/
etc. helpers (which all funnel into `BY_ACCESSORY` or `POTTED`, both included in `all()`). The
one exception is the potted fern, which needs a *second* tint index for its own color - see the
flower pot section below for why that one still needs its own separate registration call.

**Preserving the lily pad's random rotation — why `multipart` doesn't work here:**
`lily_pad.json`'s blockstate picks one of 4 unweighted `y: 0/90/180/270` variants per block, and
in isolation that pick is a hash of the block's *position* alone (`BlockBehaviour.getSeed`
defaults to `Mth.getSeed(pos)`, ignoring the block/state - checked against the compiled game).
The first attempt at this feature kept the lily pad and the accessory as two separate models
layered via a `multipart` blockstate, on the theory that reusing the same position would
reproduce the same pick. It didn't: `MultiPartModel.collectParts` (checked by disassembling the
compiled game, not guessed) calls `random.nextLong()` *once* up front and re-seeds with that
derived value for every part, rather than passing the position-derived seed straight through to
each part's own weighted pick. A plain (non-multipart) `variants` block never takes that detour.
So a `multipart`-nested weighted pick and a top-level one are both deterministic, but by
*different* transforms of the same position - they don't agree, and from the outside it looks
like ~random rotation (in testing, a ~25% match rate - exactly chance across 4 options).

The fix is for the combined block to go through the *exact same* code path as a plain lily pad:
one model, referenced by a plain `variants` block with the identical 4-entry array vanilla uses
(`assets/extra_blocks/blockstates/lily_pad_with_<name>.json`, mirroring
`minecraft:blockstates/lily_pad.json` structurally, just pointing at our model id) - no
`multipart` anywhere. Since both go through the same `SimpleModelSelectors`/`WeightedVariants`
path with the same input seed, the same position now picks the same index whether or not an
accessory is on top.

**Building the combined model** (`models/block/lily_pad_with_<name>.json`): copy the `elements`
array from `minecraft:models/block/lily_pad.json` (one thin quad) and from the accessory's own
*template* model (`minecraft:models/block/template_torch.json` /
`.../template_lantern.json` - the template, not `torch.json`/`lantern.json`, which just point at
the template with concrete textures) into one model's `elements`, giving each its own texture
variable (`#pad`, `#torch`/`#lantern`) declared in one shared `textures` block. The blockstate's
per-variant `y` rotation then rotates the *whole* merged model at once - fine here because both
the torch's and the lantern's own geometry are already rotationally symmetric about Y (checked:
torch is a centered square post; the lantern's two diagonal loop-handle elements together cover
both diagonals, so the shape as a whole is unchanged by a 90° turn), so only the lily pad's
texture orientation actually changes.

No manual pixel offset needed: checked against the actual vanilla files, `template_torch`/
`template_lantern` both render from y=0 of their own cell already, and `lily_pad`'s own element
sits at y=0.25 (out of 16) — close enough to flush that copying both in unmodified already looks
right.

**To add another purely-decorative accessory** (a `LilyPadAccessoryBlock` - see "Where this stops
working" below for which ones qualify): add a `register(...)` call in `LilyPadAccessories.java`,
a merged model per the pattern above, and a `variants`-style (not `multipart`) blockstate with
the same 4-entry rotation array.

Properties are a deliberate exception to "copy the accessory's own vanilla values", not the rule:
anything standing on a lily pad breaks instantly with no tool required (`strength(0.0f)`, no
`requiresCorrectToolForDrops()`), regardless of what the accessory itself normally needs when
placed on solid ground - lantern's own vanilla pickaxe requirement is deliberately dropped for
this reason. Match that (`strength(0.0f)`, no tool requirement, no tag entry) rather than the
new accessory's own harvesting rules. Light level and sound are still worth copying from the
accessory, though — those aren't part of this exception. Currently registered this way: torch,
soul torch, copper torch, lantern, soul lantern, copper lantern (all 8 variants: 4 oxidation stages + their waxed versions, one combo each so the exact item drops back, textures in `models/block/lily_pad_with_<variant>.json`, waxed reusing the unwaxed texture), redstone torch (always lit, a real
power source - see below), end rod (always "facing=up" - it's already the base, unrotated model), and the
flower pot (always empty - see below). Chains are decorative too, but registered separately because the copper
ones age (see the lightning rods and chains section). The 13 wood signs used to be here too (one fixed model,
riding the lily pad's own rotation) but aren't anymore - see the signs section below for why they
now need their own block class instead.

**Verify light levels rather than guess them** - checked each of these against the wiki before
using it, since getting one wrong is an easy, easy-to-miss mistake: torch/end rod 14, copper
torch 14 (copper doesn't oxidize on a torch - "used as fuel, not the base", per the wiki), lantern/
copper lantern 15 (copper lantern's brightness is oxidation-independent, unlike copper bulbs),
soul torch/soul lantern 10, redstone torch 7.

**Redstone torch is a real power source, but always lit.** `block/custom/LilyPadRedstoneTorchBlock`
(extends `LilyPadAccessoryBlock`) copies the signal rules of vanilla's lit `RedstoneTorchBlock`, checked
by disassembly: weak 15 to every side except the one it stands on (`getSignal` returns 0 for
`Direction.UP`, i.e. when asked by the block below), strong 15 to the block above (`getDirectSignal`
answers only for `Direction.DOWN`), `isSignalSource` true. `onPlace` and `affectNeighborsAfterRemoval`
(skipped when moved by a piston) notify all six neighbours via `updateNeighborsAt` with an
`ExperimentalRedstoneUtils` orientation, as vanilla does - both fire for our combo<->lily pad swaps, since
`LevelChunk.setBlockState` calls `affectNeighborsAfterRemoval` whenever the block type changes. What is *not*
copied: a real torch turns off when the block below is powered (the `LIT` property, the burnout counter, the
2-tick delay); here that block is water, which can't be powered, so the model stays the lit one
(`minecraft:block/redstone_torch`, the 7-element glowing-tip version - not `redstone_torch_off`'s plain
stick) and there is no state. Also not copied: the redstone dust particles vanilla spawns in `animateTick`.

## Shapes on a lily pad combo (what you bump into, aim at, and click)

Each combo has three shapes, built once in its constructor (`block/custom/LilyPadShapes`):
- `accessory` - the item on its own, from the *real accessory block's* `getShape`. Used only to tell
  "aiming at the item" from "aiming at the pad" (see the breaking section).
- `outline` - pad + item: `getShape`, what the crosshair can hit and the wireframe draws.
- `collision` - pad + the real accessory's own `getCollisionShape`: `getCollisionShape`. Vanilla blocks
  with no collision (torches, signs, banners, `noCollision()` - checked in `Blocks`' bytecode) report an empty
  shape, so those combos are solid only as far as the pad. Lantern, end rod, candles, sea pickle,
  flower pots and heads do have collision, so they get it. No per-block list to maintain.

So a torch or sign is *aimable* (needed to click or mine it on its own) without being *solid*. That is
also what makes right-clicking a sign edit it: the crosshair now lands on the sign, `onUseBlock` returns
`PASS` for an existing sign, and vanilla's inherited `SignBlock` handling runs.

- `LilyPadAccessoryBlock` computes them from `shapeSource.defaultBlockState()`. Usually the accessory
  itself; `LilyPadPottedPlantBlock` passes `Blocks.FLOWER_POT` instead (its `accessory` is the plant, but a
  potted plant's shape is the pot's).
- Candles and sea pickles change shape with their count, so `LilyPadCandleBlock`/`LilyPadSeaPickleBlock`
  precompute 4 (via `super.getShape`) and index by `CANDLES`/`PICKLES`; outline == collision.
- `LilyPadSignBlock` builds its `accessory`/`outline` from `super.getShape` (vanilla's fixed post box) and an
  empty collision.
- `LilyPadSkullBlock`/`LilyPadBannerBlock` build theirs from the real vanilla head's or banner's default state,
  **not** `super.getShape` (see the heads and banners section for why).
- **Candle rotation caveat:** the blockstate spins the whole model 0/90/180/270 by the pad's position-seeded pick,
  which Java can't cheaply reproduce, and 2-4 candles aren't square. So candle shapes are the union of all four
  90-degree turns (`Shapes.rotateHorizontal`) - a few pixels generous, never wrong-way-round. Sea pickle shapes
  are square, so they need no such workaround.

## Candles and sea pickles on a lily pad (real stacking, not just a picture)

Unlike the purely-decorative accessories above, candles and sea pickles keep their real vanilla
behavior - stacking up to 4, lighting/extinguishing (candles), and the dead/alive appearance
(sea pickles) - because `LilyPadCandleBlock extends CandleBlock` and
`LilyPadSeaPickleBlock extends SeaPickleBlock` directly (`block/custom/`), inheriting their real
`CANDLES`/`LIT`/`PICKLES`/`WATERLOGGED` blockstate properties instead of having none.

**What subclassing gets you for free, and what it doesn't - check method bodies, don't assume:**
disassembling the compiled game (not guessing) showed:
- Lighting with flint and steel/fire charge: **free**. `FlintAndSteelItem.useOn` calls the public
  static `CandleBlock.canLight(state)`, which checks `state.is(BlockTags.CANDLES, ...)` - a *tag*
  check, not `instanceof`/exact-class. Our combo blocks just need to be in
  `data/minecraft/tags/block/candles.json` (added, `replace: false`) for this to work.
- Extinguishing by hand: **free**. `CandleBlock.useItemOn`'s empty-hand branch only checks
  `state.getValue(LIT)` and calls the public static `AbstractCandleBlock.extinguish(...)` - no
  reference to `this` at all.
- **Stacking another candle/pickle by right-clicking with one: *not* free**, despite looking like
  the same kind of check. `CandleBlock`/`SeaPickleBlock.canBeReplaced` compares the held item
  against `this.asItem()` - and a block with no registered `BlockItem` has `asItem() == AIR`, so
  it can never match a real candle/pickle item. Worse, even overriding just `canBeReplaced` isn't
  enough on its own: the vanilla `BlockItem` placement pipeline would then call
  `getStateForPlacement` on the *held item's own* block (the real vanilla candle), which checks
  `existingState.is(this)` - false, since `existingState` is our combo, not that real block -  so
  it would compute a *fresh* placement and silently replace our combo (losing the lily pad
  underneath) instead of incrementing it. Stacking is instead handled entirely by our own
  `LilyPadAccessoryInteraction`, matching by `instanceof LilyPadCandleBlock`/
  `LilyPadSeaPickleBlock` + `accessory() == heldBlock`, short-circuiting before any of that
  vanilla logic runs - see `tryStack` there.

**Light level for candles**: `CandleBlock.LIGHT_EMISSION` is a public static
`ToIntFunction<BlockState>` that already implements the real "scales with lit candle count"
formula - reuse it directly (`.lightLevel(CandleBlock.LIGHT_EMISSION)`) rather than picking a
fixed number. Sea pickles aren't a light source in vanilla; no `lightLevel` call needed.

**Building the models**: same merged-model technique as the decorative accessories, but there
are more of them since blockstate properties multiply:
- Candles: `models/block/lily_pad_with_<color>_<count>_<lit|unlit>.json` - 17 colors (16 dye
  colors + plain `candle`) × 4 counts × 2 lit states = 136 files, using vanilla's own
  `template_candle`/`template_two_candles`/`template_three_candles`/`template_four_candles`
  elements (color/lit is *only* a texture swap - same 4 element sets reused for all 136 - so
  don't regenerate the geometry per color, generate it once per count and just vary the
  `textures` block). No random rotation needed on top - unlike lily pads, candles have no
  position-seeded rotation of their own to preserve, so the blockstate is a plain
  `"candles=N,lit=B"` → 4-entry-rotated-model map (rotation still comes from the lily pad half).
- Sea pickles: `models/block/lily_pad_with_sea_pickle_<1-4>_<wet|dry>.json` - 8 files, using
  vanilla's `sea_pickle`/`two_sea_pickles`/`three_sea_pickles`/`four_sea_pickles` (alive/glowing)
  and `dead_sea_pickle`/`two_dead_sea_pickles`/etc. (dry) elements - genuinely different geometry
  per state here (dead ones drop the glow-tendril elements), not just a texture swap.

Always placed/combined at count 1, unlit, non-waterlogged (`LilyPadAccessoryInteraction.combine`
sets this explicitly rather than trusting `defaultBlockState()`).

## Signs on a lily pad (real writable text, real player-facing rotation)

Real, writable text, and a real 16-value `rotation` matching wherever the player was facing when
they placed it - same as a sign placed on any other block, not one fixed look and not blank. This
is the mod's first (and so far only) use of a real `BlockEntity`.

**`block/custom/LilyPadSignBlock.java` extends the real vanilla `StandingSignBlock` directly** -
same "subclass the real vanilla block" pattern as `LilyPadCandleBlock`/`LilyPadSeaPickleBlock` -
to inherit `ROTATION`/`WATERLOGGED`, and (critically) the real text-edit interaction and
dye/glow-ink/wax handling (`useWithoutItem`/`useItemOn`), instead of reimplementing any of it. Its
constructor computes `WoodType.getWoodType(accessory)` (actually `SignBlock.getWoodType(accessory)`
- a public static helper) and passes that to the `StandingSignBlock(WoodType, Properties)` super
constructor.

**Why signs have their own `BlockEntityType`**: vanilla's own `BlockEntityTypes.SIGN`
is a frozen `Set<Block>` allow-list of the 26 literal vanilla sign blocks - confirmed by disassembly,
`isValid(state)` is a plain `Set.contains(state.getBlock())`, never an `instanceof` check. So no custom
block satisfies it on its own, whatever it extends. Fabric API's `addValidBlock` can add one; heads
and banners do that (see their section). Signs were built before that was found and keep their own type,
which works just as well. `block/LilyPadSignBlockEntities.java`
registers its own `BlockEntityType<LilyPadSignBlockEntity>` (a plain public constructor,
`new BlockEntityType<>(factory, validBlocks)` - there's no builder in this version), with all 13
registered `LilyPadSignBlock` instances (`LilyPadAccessories.SIGN_BLOCKS`) as its valid blocks.
**Load-order matters here**: that valid-blocks set must be fully populated before this class loads,
so `ExtraBlocks.onInitialize()` calls `LilyPadAccessories.initialize()` before
`LilyPadSignBlockEntities.initialize()` - get the order backwards and it silently registers an
incomplete (possibly empty) valid-blocks set, no crash, just broken block-entity loading for
whichever signs registered after it.

`block/custom/LilyPadSignBlockEntity.java` just extends vanilla's `SignBlockEntity`, pointed at our
type instead of `SIGN` (mirrors how vanilla's own `HangingSignBlockEntity` forwards to
`SignBlockEntity`'s typed constructor with `HANGING_SIGN` instead) - text storage/persistence, the
edit-lock, click commands are all inherited unchanged, no overrides needed.

**Three things the inherited `StandingSignBlock` behavior gets wrong for a lily pad, all overridden
on `LilyPadSignBlock`:**
- `canSurvive` - the inherited version requires a solid block below; a lily pad floats on water
  (not solid), and every other accessory combo already "always survives" (default `Block`
  behavior) - override to unconditionally `return true`, matching that instead of the real sign's
  rule. Skipping this is a real regression, not just a missed nice-to-have: the next water update
  below the lily pad would otherwise silently pop the whole combo to air, bypassing
  `LilyPadAccessoryBreaking` entirely (no drop, no particles, it just vanishes).
- `newBlockEntity` - the inherited version constructs a plain vanilla `SignBlockEntity` typed to
  vanilla's own frozen `BlockEntityType.SIGN`, which this block can never satisfy - override to
  return `new LilyPadSignBlockEntity(pos, state)`.
- `getTicker` - the inherited version checks reference-equality against vanilla's own
  `BlockEntityType.SIGN` and would otherwise always return `null` for our type, silently skipping
  `SignBlockEntity.tick`'s stale-edit-lock cleanup forever - override using the same
  `createTickerHelper(type, LilyPadSignBlockEntities.LILY_PAD_SIGN, SignBlockEntity::tick)` pattern
  `SignBlock` itself uses.

**Rotation and opening the text editor** happen in `LilyPadAccessoryInteraction.combine`, since this
block is swapped in directly rather than placed through the normal `BlockPlaceContext` pipeline that
would compute both automatically for a real sign:
- Rotation: `RotationSegment.convertToSegment(player.getYRot() + 180.0F)`, the *exact* vanilla
  formula, checked by disassembling `StandingSignBlock.getStateForPlacement` and
  `UseOnContext.getRotation()` rather than guessed (the `+ 180` matters - it's the player's own
  facing rotated to match which way the sign's text side ends up pointing).
- Opening the editor: call `signBlock.setPlacedBy(level, pos, placedState, player, heldStack)`
  directly right after `setBlockAndUpdate` - `SignBlock.setPlacedBy` (public) already does exactly
  the right thing (server-side check, not-waxed check, editable-text check, then opens the editor
  for the placer), and takes a `LivingEntity`, so `Player` satisfies it with no adapting needed.
  Reuse it rather than reimplementing its checks.
- Right-clicking an *already-placed* lily-pad sign needs no dedicated branch in `onUseBlock` at
  all: it already falls through to `PASS` (no branch matches an existing `LilyPadSignBlock`), and
  Fabric's `UseBlockCallback` returning `PASS` lets vanilla's own block dispatch
  (`useWithoutItem`/`useItemOn`, inherited once `LilyPadSignBlock` extends `StandingSignBlock`) run
  afterward - confirmed by checking the Fabric mixin that fires this event only overrides the
  result when non-`PASS`.

**Rendering**: `ExtraBlocksClient` reuses vanilla's own `StandingSignRenderer` directly for the new
`BlockEntityType`, via `BlockEntityRendererRegistry.register` (Fabric API - deprecated in this
version with no replacement shipped yet, but still the only working entrypoint for late
registration into vanilla's renderer map; the warning is expected, not a mistake). No custom
renderer class was needed: `StandingSignRenderer`/`AbstractSignRenderer` only draw the sign's
*text* (`SubmitNodeCollector.submitText(...)`, confirmed by disassembly - real vanilla signs never
draw their own post/board mesh procedurally either) - the wood geometry is 100% the ordinary baked
block model, same as every other block, so vanilla's renderer works unmodified against our
`LilyPadSignBlockEntity` (it only needs `SignBlockEntity`, which we extend).

**Building the models - fully baked rotation, no blockstate `"y"`:** a real sign's own blockstate
(vanilla's `oak_sign.json`, checked directly) maps `rotation=N` to 4 base models (`_rot_(N % 4)`)
*plus* a whole-model `"y": 90 * (N / 4)` transform. That works for a plain sign, but **breaks here**:
our merged model also bakes in the lily pad's own quad element (`#pad`, `tintindex: 0`), and that
`"y"` transform rotates the *entire* model, including the pad's quad, which has no way to
counter-rotate within the same file - the pad's texture visibly spun to match whatever the sign's
facing was. The fix: bake all 16 rotation values directly into 16 distinct models per wood, each
with a single combined angle (`"rotation": {"angle": -22.5 * N, "axis": "y", "origin": [8,0,8]}`,
same key already used up to `-67.5°` in earlier iterations of this feature - proof it accepts a
continuous angle, not a restricted enum) applied to *both* the post and board elements, and leave
the lily pad's own quad element free of the sign's angle (only its own pad-only rotation, see below) in every one of
the 16 models. The blockstate then maps `rotation=0..15` straight to 16 models with **no `"y"` key
anywhere** - full decoupling. `N=0` keeps the original near-zero `0.0001°` on the board only (not
the post), the same deliberate z-fighting workaround as before; `N=1..15` apply their angle to both
elements.

**The pad keeps its own position-seeded rotation too (`_pad_0..3`):** each `rotation=N` blockstate
entry is a 4-element unweighted array (same shape as vanilla's `lily_pad.json`), pointing at
`lily_pad_with_<wood>_sign_rot_<N>_pad_<K>`, where K=0..3 differs only in the pad quad's own
`"rotation": {"angle": -90 * K, "axis": "y", ...}` (K=0 has no key). That's 16 x 4 = 64 models per
wood (832 total), generated by script from the `_rot_N` ones. This works because it's a plain
`variants` block going through the same `WeightedVariants` path as a bare lily pad - same position
seed, same index picked - so the pad faces exactly the way it did before the sign went on
(the same reasoning as the `multipart` note above). The `-90 * K` sign matches the existing
convention here (blockstate `"y": 90` == element angle `-90`, as in the sign's own `_rot_4`).

## The flower pot on a lily pad (real potting, only with what a real pot accepts)

Unlike signs, this one *does* work close to the real thing: right-clicking the empty
`lily_pad_with_flower_pot` with a plant swaps it for the matching potted combo (39 of them - every
plant a real flower pot accepts), exactly mirroring vanilla's own rule (`FlowerPotBlock.useItemOn`,
checked by disassembly): `POTTED_BY_CONTENT.getOrDefault(heldBlock, AIR)` - if the held item isn't
a real plant/pot pairing (sugarcane, say), nothing happens and the click falls through to normal
placement, same as vanilla's own `TRY_WITH_EMPTY_HAND` case. What's still descoped: you can't
later swap the plant for a different one (matches vanilla - breaking is the only way), and it's
still a plain `LilyPadAccessoryBlock`-family class, not a real `FlowerPotBlock`, so nothing here
is BlockEntity-backed.

- `block/custom/LilyPadPottedPlantBlock.java` (extends `LilyPadAccessoryBlock`) - only
  difference from the plain class: a real potted plant drops *two* items (the pot and the plant),
  not one, so this adds the flower pot item on top of what the parent's `playerDestroy` already
  drops (lily pad + the plant, via `accessory`).
- `block/LilyPadAccessories.java`'s `POTTED_PLANTS` map and `registerPotted` - deliberately
  **not** added to `BY_ACCESSORY` (the map the initial lily-pad combine step reads): a potted
  plant is only reachable by planting into an already-placed empty pot, never directly onto a
  bare lily pad. `LilyPadAccessoryInteraction.plant()` is the separate step that checks
  `existingState.getBlock() == LILY_PAD_WITH_FLOWER_POT` first.
- Empty pot model: `models/block/lily_pad_with_flower_pot.json`, vanilla's own `flower_pot.json`
  elements (5 - four rim pieces + a dirt-topped block) copied in verbatim, texture vars
  `#flowerpot`/`#dirt` kept as named (only `#pad` is our own addition). Every potted-plant model
  reuses these same 5 elements plus the specific plant's own (most plants' own vanilla model
  already includes copies of these 5 - reuse the *whole* model's elements verbatim rather than
  re-adding the rim yourself, or the pot renders doubled).

**Don't assume the plant's block id matches the combo/texture naming - check `getPotted()`'s
actual mapping, not the potted block's own name.** Two real gotchas found doing this: the plant
you hold to get `potted_azalea_bush` is `azalea` (not `azalea_bush` - that name only exists for
the potted model/texture, `azalea_bush`/`flowering_azalea_bush` aren't real placeable blocks);
and `crimson_roots`/`warped_roots` keep their own block id but use a *different* texture file
(`crimson_roots_pot`/`warped_roots_pot`) than their standalone appearance. Neither is guessable
from the name - check `assets/minecraft/models/block/potted_<x>.json`'s own `textures` block, and
verify the plant id by finding which real block a `FlowerPotBlock` instance's `getPotted()`
actually returns (or just check `assets/minecraft/blockstates/<id>.json` exists as a normal
placeable block, which `azalea_bush` doesn't).

**Fern needs a second, separate tint index.** Its plant element uses `tintindex: 1` (not 0, which
is already the lily pad's own), registered as its own two-entry `BlockColorRegistry.register`
call in `LilyPadAccessoryColors` (`constant(...)` at index 0, `BlockTintSources.grass()` at index
1, matching vanilla's own registration for `Blocks.FERN`) - excluded from the shared one-entry
call every other combo uses, since registering the same block twice would just overwrite the
first with the second rather than combining them.

## Breaking a lily pad accessory: aim at the item or at the pad

What a break removes depends on where the crosshair is (`block/LilyPadTarget`):
- **Aiming at the accessory:** removes just the accessory (dropping it, leaving a plain lily pad behind).
  Mining time and tool rules are the accessory's own; a *second* hit, now on an ordinary lily pad with no
  code of ours involved, removes that too. Which tool actually works, and whether the wrong one still lets
  the accessory drop, varies by accessory. Only the lantern family, chains and lightning rods need a pickaxe
  (stone or better for rods, as in vanilla). Signs, banners, heads, candles, torches, end rod, sea pickle,
  flower pot and potted plants drop with any tool (an axe is just faster for signs and banners), matching
  vanilla.
- **Aiming at the lily pad:** breaks the pad, which takes the accessory with it. Instant, no tool needed,
  and both drop, like a torch popping off when its block is broken. In creative only the accessory drops (not
  the pad) - it isn't "mined", it loses its support, so it drops even there.

**How aim is worked out:** the server is never told where on a block the player was aiming when they break
it (`ServerboundPlayerActionPacket` carries only the position). So `LilyPadTarget.aimedAtAccessory` casts the
player's own view ray (`eye -> eye + view * (blockInteractionRange + 1)`) against the pad shape and the
accessory shape separately with `VoxelShape.clip` and takes the nearer hit (falling back to "accessory" if
neither is hit, e.g. lag - the old behavior). The same test runs on the client and the server so they agree.

**The breaker's client predicts the pad staying** (`client/block/LilyPadBreakPrediction`). A client doesn't wait
for the server when its player breaks a block: `MultiPlayerGameMode.destroyBlock` predicts the result, and the
prediction is always "the whole block becomes air". The server's lily pad only arrives a tick later, so on its
own the pad blinks out and back in.
- Fabric's `ClientPlayerBlockBreakEvents.AFTER` fires right after that predicted removal (it's injected before
  `Block.destroy`, while the prediction is still open). For an accessory-only break, by the same aim test, it
  puts the lily pad straight back with `UPDATE_ALL_IMMEDIATE`.
- Nothing redraws when the server answers. The server's block update goes out in the level tick, before the
  acknowledgement goes out in the connection tick, so the acknowledgement syncs to the lily pad already shown.
- The blink only became visible once break particles came from the accessory. The old green lily-pad burst
  had covered it.

**Instant pad = a `getDestroyProgress` override.** Mining time is per block state, not per part, so each combo
class overrides the protected `getDestroyProgress` to return `1.0F` when the pad is aimed at, else the
normal value (`LilyPadTarget.destroyProgress`). Both `ClientPlayerGameMode` and `ServerPlayerGameMode` call it.
**Only the aimed part is outlined.** The game uses the same `getShape` call both to cast the crosshair ray and
to draw the wireframe (`LevelExtractor.extractBlockOutline` -> `state.getShape(level, pos,
CollisionContext.of(camera entity))`), and passes the looking player as the context. So each combo's `getShape`
goes through `LilyPadTarget.outline`, which - when the context is an `EntityCollisionContext` for a `Player` -
returns just the accessory shape or just the pad shape for the aimed part (the whole pad + item outline for any
other caller, or when the ray hits neither part). Because the ray cast then only ever sees that one part, the
crosshair and the wireframe can't disagree. Known limits: the aim test uses the player's current eye position and
view, while the game's own ray uses the frame-interpolated ones, so right on the boundary between pad and item the
wireframe can briefly pick the wrong part; and if the client and server disagree by a hair about which part is
aimed at when breaking, the break is rolled back and re-synced.

- `block/LilyPadAccessoryBreaking.java` - hooks `PlayerBlockBreakEvents.BEFORE`, **not**
  `Block.playerWillDestroy`. Checked by disassembly before writing any of this: `playerWillDestroy`'s
  return value is captured by the caller but never written back to the world - the block gets
  removed to air immediately afterward regardless of what it returns - so it's structurally
  useless for "become something else instead of vanishing." `PlayerBlockBreakEvents.BEFORE`
  returning `false`, by contrast, is confirmed (same way - disassembling the Fabric API mixin that
  implements it) to skip vanilla's *entire* destroy sequence before any of it runs: no removal, no
  drops, no XP. That clean slate is what gets substituted with our own outcome (accessory only, or
  pad + accessory, per the aim test above).
- `block/custom/LilyPadCombo.java` - a small interface (`accessoryDrops(BlockState)`, `accessoryShape(BlockState)`) every combo
  block implements, since they don't share a common superclass to hang this on (`LilyPadAccessoryBlock`
  directly, `LilyPadCandleBlock extends CandleBlock`, `LilyPadSeaPickleBlock extends SeaPickleBlock`).
  Lets `LilyPadAccessoryBreaking` treat all of them the same way.
- Because that hook cancels vanilla's destroy sequence for every combo, `Block.playerDestroy` (its only caller is
  `ServerPlayerGameMode.destroyBlock`, checked by scanning the game jar) is never reached for a player break -
  so the `LilyPad*Block` classes deliberately don't override it.
- Mining *speed* (and whether a tool is required at all for the drop) is unrelated to that class -
  it's governed entirely by each combo's own `strength()`/`requiresCorrectToolForDrops()`/mineable
  tag in `LilyPadAccessories`, same as any other block. `PlayerBlockBreakEvents.BEFORE` only fires
  once mining has already finished (confirmed by where the Fabric mixin injects - inside
  `ServerPlayerGameMode.destroyBlock`, called only after mining reaches 100%), so it has no say in
  *how long* that took.

**Don't assume hardness implies a tool is required for drops - checked the wiki per accessory,
several surprised me.** Real vanilla candles (hardness 0.1), signs (hardness 1, axe is *fastest*),
end rods, sea pickles and flower pots all drop with literally any tool or bare hands - the named
tool is only ever a speed bonus, never a requirement. Even lanterns - soul and copper lantern
*and* the regular one - work the same way: "any tool, pickaxe is fastest," not "pickaxe or
nothing." So `requiresCorrectToolForDrops()` on the lantern family (`LILY_PAD_WITH_LANTERN`/`SOUL_LANTERN`/all 8
copper lantern variants, pickaxe) is a **deliberate deviation from vanilla fidelity**, not a mistake -
it's what was asked for (removing a lantern should require "the right tool"). Signs were gated behind an
axe the same way at first, but that was reverted: a sign dropped nothing by hand, which read as a bug, so
signs now match vanilla (any tool drops, axe is faster via `mineable/axe`). Hardness values themselves *do* match vanilla exactly
(lantern family 3.5, chain 5, lightning rod 3, sign/head/banner 1, candle 0.1, everything else 0) - only the
lantern tool-gating is the deliberate part. Chains and lightning rods need a pickaxe because real ones do.
Keep this distinction in mind before assuming any other accessory needs the same
treatment - signs/banners/heads/candles/torches/end rod/sea pickle/flower pot/potted plants are *correctly* left
without `requiresCorrectToolForDrops()`, matching real vanilla exactly, not an oversight.

## Particles, sounds and middle-click come from the part you aim at

A combo's own state looks and sounds like a lily pad: its model's particle texture, its green tint and its
`SoundType.LILY_PAD`. Any effect that reads that state therefore shows lily-pad bits and makes lily-pad noises,
even when the thing being broken is a sign. So effects use **the real vanilla state of the aimed part**
instead:
- `LilyPadCombo.accessoryState(state)` gives the accessory on its own:
  - `shapeSource.defaultBlockState()` for the decorative ones, which for potted plants is the flower pot,
    whose particles and sound real potted plants share;
  - the vanilla candle or sea pickle with the same count (and lit state), since the count decides where the
    particles appear;
  - the vanilla sign, head or banner.
- `LilyPadTarget.partState` picks between that and a plain `Blocks.LILY_PAD` state by aim. Aiming at neither
  counts as the accessory, as for breaking.

Heads show soul-sand particles and banners oak planks. That's vanilla: it's what `skull.json`/`banner.json` name.

The three effect paths, all checked by disassembly:
- **Break burst on the breaker's screen:** the client predicts the break. `MultiPlayerGameMode.destroyBlock`
  calls `playerWillDestroy`, then `spawnDestroyByEntityParticles`, which every combo overrides to call
  `LilyPadTarget.spawnDestroyParticles`. That sends level event `2001` with the accessory's state when only
  the accessory went, or the pad's **and** the accessory's when the pad went. `Level.destroyBlock`
  (`spawnDestroyParticles`, no entity) lands there too and gets both.
- **Break burst for everyone else:** `LilyPadAccessoryBreaking.playBreakEffects` calls the same
  `spawnDestroyByEntityParticles`. On the server, `levelEvent(player, ...)` sends it to everyone nearby except
  the breaker, who already had it. **Don't use `globalLevelEvent` for this.** Clients ignore `2001` as a
  global event (`LevelEventHandler.globalLevelEvent` only handles 1023/1028/1038), which is how other
  players used to see and hear nothing.
- **Cracks and hit sound while mining:** 26.3 sends these as level events `2019`/`2020` from
  `ServerPlayerGameMode.tick()`, to everyone nearby. The first hit comes from
  `MultiPlayerGameMode.startDestroyBlock`. Both end in `ClientLevel.addBreakingBlockEffects`, which reads the
  state at the position. `ClientLevelMixin` swaps in `LilyPadTarget.partState(...)` with the local player's
  aim.
  - **Why a mixin:** the only related Fabric hook, `FabricBlockStateModel.particleMaterial`, swaps just the
    texture and would need every combo model wrapped. It wouldn't fix the hit sound, and the combo's green
    tint would still be applied to the particles.
  - **Limit:** the events don't say who is mining. When someone else mines a combo, your client uses your own
    aim if you're looking at it, otherwise the accessory.

**Middle-click (pick block):**
- Every combo's `getCloneItemStack` returns the accessory's item, following vanilla's own rules: one candle,
  one sea pickle, and the plant for a potted plant (as `FlowerPotBlock` does).
- `LilyPadAccessoryPicking` (Fabric's `PlayerPickItemEvents.BLOCK`, which gets the server player) returns a
  lily pad when the pad is aimed at, and `null` otherwise. `null` lets vanilla run the combo's
  `getCloneItemStack` and then its ctrl+pick data copy, which is how a player head keeps its skin. Fabric skips
  that copy for a stack the event returns itself, so the accessory must not come from the event.

## Lightning rods and chains on a lily pad (working rods; copper keeps aging)

All 8 lightning rods (4 copper stages, plus waxed), the regular chain (26.3 calls it `iron_chain`) and all 8
copper chains. Registered by `registerLightningRod`/`registerChain` in `LilyPadAccessories`, from vanilla ids.
Models are the pad element plus the vanilla template's own elements: `template_lightning_rod`,
`template_chain`. Both templates look the same when turned 90°, so the pad's rotation can turn the whole
model. Waxed blockstates point at the unwaxed models, as vanilla's do.

**Chains are decorative:** `LilyPadAccessoryBlock`, standing upright as a chain placed on top of a block does.
They're in `minecraft:chains`, which vanilla's `mineable/pickaxe` includes, and nothing else reads that tag at
runtime.

**Lightning rods work** (`block/custom/LilyPadLightningRodBlock extends LightningRodBlock`).
- It inherits everything a rod does: `POWERED`, `onLightningStrike` (glow, redstone pulse, spark event,
  unpowered 8 ticks later), the redstone signals and the thunderstorm sparks. None of that checks the block's
  identity, and `LightningBolt.powerLightningRod` powers anything `instanceof LightningRodBlock`.
- **How lightning finds a rod:** `ServerLevel.findLightningRod` searches for the `PoiTypes.LIGHTNING_ROD` point
  of interest within 128 blocks, on the highest block of its column, and strikes the block above.
- **Registering the combos as that point of interest:**
  - which states count comes only from `PoiTypes.TYPE_BY_STATE` (nothing reads `PoiType.matchingStates`);
  - that map is filled by the private `PoiTypes.registerBlockStates`;
  - Fabric's `PoiHelper` can only create new types;
  - so `mixin/PoiTypesInvoker` (a static `@Invoker`) lets `registerLightningRod` add every state of each rod
    combo to vanilla's lightning-rod type.
- **The rods must be in `minecraft:lightning_rods`,** not just for the tool rule (`mineable/pickaxe` plus
  `needs_stone_tool`):
  - `ServerLevel.tickThunder` checks that tag under a strike. Without it, the strike can become a
    skeleton-horse trap whose bolt is visual only and never powers the rod.
  - The Channeling enchantment checks the same tag to let a trident call lightning on a rod.

**Copper stages keep aging, through vanilla's own code** (Fabric's `OxidizableBlocksRegistry`):
- `registerCopperAging` registers two kinds of pairs:
  - next-stage pairs (`registerNextStage`, into `WeatheringCopper.NEXT_BY_BLOCK`, which Fabric makes mutable,
    and it also refreshes the random-tick cache);
  - waxable pairs (`registerWaxable`, into `HoneycombItem.WAXABLES`).
- Vanilla then handles aging, honeycomb, the axe's scrape and wax-off, and lightning resetting a struck unwaxed
  rod to fresh copper (`LightningBolt.clearCopperOnLightningStrike`).
- The unwaxed stages are `LilyPadWeatheringLightningRodBlock`/`LilyPadWeatheringAccessoryBlock`, which mirror
  vanilla's `WeatheringLightningRodBlock`/`WeatheringCopperChainBlock`:
  - `getAge()` returns the stage;
  - `randomTick` calls `changeOverTime`;
  - `isRandomlyTicking` is true only if there's a next stage.
- Each stage is its own combo, so drops and middle-click give the current stage's item.
- **The copper lanterns don't age.** They stay whatever stage they were placed as. Registering them the same
  way with `LilyPadWeatheringAccessoryBlock` is all it would take.

## Heads and banners on a lily pad (vanilla's own renderer and block entity)

All 7 floor heads (the `minecraft:skulls` tag: skeleton, wither skeleton, zombie, player, creeper, dragon,
piglin) and all 16 standing banners. They behave as they do on a normal block: player skins, 16-way
rotation, collision (heads), custom names, the dragon/piglin animation when powered, banner patterns, the
waving flag and map markers.

**Nothing is merged into a model here, the opposite of every other accessory.** `skull.json`/`banner.json`
are empty (particle texture only), because heads and banners are drawn entirely by vanilla's block entity
renderers. Those renderers only need the block to *be* a head or a banner (checked by disassembly):
- `SkullBlockRenderer` reads `((AbstractSkullBlock) block).getType()` and `SkullBlock.ROTATION` (or checks
  `instanceof WallSkullBlock`).
- `BannerRenderer` checks `instanceof BannerBlock` and reads `BannerBlock.ROTATION`. It takes the base
  colour from `BannerBlockEntity.getBaseColor()`, which comes from the block's `getColor()`.

So `block/custom/LilyPadSkullBlock` extends `SkullBlock`, `LilyPadBannerBlock` extends `BannerBlock`, and
their blockstates (`lily_pad_with_<id>.json`) are verbatim copies of vanilla's `lily_pad.json`: four
`minecraft:block/lily_pad` variants under `""`. `""` matches every `ROTATION`/`POWERED` state, as in
vanilla's own `player_head.json`. The pad keeps its position-seeded rotation, and the head or banner rotates
separately in the renderer. There are no merged models, no custom renderer and no client code; the tint comes
through `LilyPadAccessories.all()` as usual.

**They use vanilla's own block entity type, through Fabric's `addValidBlock`.** The block entity is a plain
vanilla `SkullBlockEntity`/`BannerBlockEntity`, from the inherited `newBlockEntity`. Their types are
`BlockEntityTypes.SKULL`/`BANNER`; in 26.3 the vanilla constants live in `BlockEntityTypes`, not
`BlockEntityType`. Both have frozen valid-block sets, and the `BlockEntity` constructor throws
`IllegalStateException` for any block not in them. Fabric API's `addValidBlock` is interface-injected on
`BlockEntityType`, and its mixin swaps the frozen set for a `HashSet`. `registerSkull`/`registerBanner` call it
right where each combo is registered. Because the type is vanilla's, vanilla's renderer is already registered
for it.

What inheriting gets wrong, all overridden:
- **Shapes** come from `accessory.defaultBlockState()`, **not** `super.getShape` as in the sign.
  `SkullBlock.getShape` calls the virtual `getCollisionShape`, and our override reads `shapes`, which is still
  null in the constructor. Taking them from the vanilla block also gives the piglin's 10px box and the dragon's
  8.5px outline. Banners have no collision, so they're solid only as far as the pad.
- **Head `getTicker`:** vanilla only animates when `state.is(Blocks.DRAGON_HEAD)`/`PIGLIN_HEAD`/..., which
  compares against the vanilla block, so a combo would never pass. The override checks the skull type instead.
  `POWERED` and `neighborChanged` are inherited and work unchanged.
- **Banner `canSurvive` returns `true`.** A banner needs `isSolid()` below, and its `updateShape` would
  otherwise turn the combo into air, the same trap as signs.
- **`getCloneItemStack` (pick-block):**
  - Heads return the head item. Ctrl+pick in creative makes the server add the block entity's data, the skin.
  - Banners also apply `collectComponents()`, because vanilla's `BannerBlockEntity.getItem()` builds the item
    from the block, and our combo has no item.

**Placement** (`LilyPadAccessoryInteraction.combine`):
- Heads use `RotationSegment.convertToSegment(player.getYRot())`, with **no `+180`**
  (`SkullBlock.getStateForPlacement`), and `POWERED = level.hasNeighborSignal(pos)`.
- Banners use `+180`, like signs.
- `copyItemData` then runs for any combo with a block entity, doing what `BlockItem.place` does:
  `BlockItem.updateCustomBlockEntityTag`, then `applyComponentsFromItemStack` + `setChanged` (vanilla's own
  helper for that is private). It runs before the stack shrinks and in the same tick as the placement, so the
  chunk update that sends the block to clients carries the skin/patterns.

**Drops run vanilla's own loot tables against the block entity** (`block/custom/LilyPadVanillaDrops`). Their
`copy_components` entries decide what carries over:
- player head: `profile`, `note_block_sound`, `custom_name`;
- other heads: `custom_name`;
- banners: `custom_name`, `item_name`, `tooltip_display`, `banner_patterns`, `rarity`, so an ominous banner
  keeps its name.

`accessoryDrops(state, blockEntity)` handles the accessory-only break. A `getDrops(state, LootParams.Builder)`
override returns the lily pad's table plus the accessory's for explosions and pistons. Both of those pass the
block entity, and the combos use `PushReaction.POPPED`, which is what vanilla uses for lily pads and heads.
**The other combos have no loot tables and still drop nothing when blown up.** These two are the exception so
that an explosion can't delete a player head without dropping it.

Breaking the pad takes the head with it, like every other accessory. This was a deliberate choice over
vanilla's rule that a head needs no support and stays floating.

Banner combos are in `minecraft:banners` (`data/minecraft/tags/block/banners.json`). That makes an axe faster,
because vanilla's `mineable/axe` includes `#minecraft:banners`, and lets filled maps mark them, because
`MapItem` checks this tag plus `BannerBlockEntity`.

## Commands
Run from this mod's folder (`BPsBetterVanillaBuilding/`, where `gradlew` lives), not the repo root, which
holds several mods. `JAVA_HOME` must point at the JDK 25 install.

- `./gradlew build` — compile + produce the jar in `build/libs/`, then copy it to `jars/`
- `./gradlew runClient` — launch a dev Minecraft client with the mod loaded
- `./gradlew runServer` — launch a dev dedicated server
- `./gradlew prodClient` / `./gradlew prodServer` — run the **finished jar** the way a player or server
  owner would: the built jar plus the real Fabric API jar (`productionRuntimeMods`, non-transitive), launched
  like a normal install, in `run/prod-client/` and `run/prod-server/`. This is the check before handing a jar
  to anyone, since the dev runs load classes straight from the build folders. The server stops at Minecraft's
  EULA until `run/prod-server/eula.txt` says `eula=true` (https://aka.ms/MinecraftEULA). Accepting it is the
  user's call, so don't set that yourself.
- `./gradlew clean` — wipe build output

The distributable jar is `build/libs/BPsBetterVanillaBuilding-<mod_version>+<minecraft_version>.jar`
(e.g. `BPsBetterVanillaBuilding-1.0.0+26.3.jar`).
Ignore `*-sources.jar` and anything in `build/devlibs/` — those are not for distribution.

`build` also copies the finished jar into `jars/` (the `copyJarToJars` task in `build.gradle`). **Jars stay on
the user's PC only and must never be committed or pushed to GitHub**: `jars/` and `*.jar` are in the repo's
root `.gitignore`. Don't force-add them, don't attach them to a GitHub Release, and don't add a workflow step
that uploads them as artifacts. The user's rule is that jars are never uploaded (see the repo's root CLAUDE.md).

**What a player needs:** Minecraft 26.3, Fabric Loader 0.19.5 or newer, and Fabric API 0.160.7 or newer for
26.3, plus this jar in their `mods` folder. The official launcher supplies Java itself. To play together,
every player needs the mod, and so does the server (dedicated servers need Fabric and Fabric API too). A
LAN world runs on the host's game, so the host needs it as well. Expect a slow first load: about 10,100
mixed-slab blocks are registered at startup.

## Version bumps
Versions live in `gradle.properties`. `mod_version` is the mod's own; `build.gradle` appends `+<minecraft_version>`
to make the jar's and the game's version (e.g. `1.0.0+26.3`). Check https://fabricmc.net/develop for the
current set before changing `minecraft_version` / `fabric_api_version` / `loom_version`, and bump the
`minecraft`, `fabricloader` and `fabric-api` bounds in `src/main/resources/fabric.mod.json` to match. The
`fabric-api` bound is the version built against: the mod uses API added in recent Fabric API releases
(`addValidBlock`, `PlayerPickItemEvents`, `OxidizableBlocksRegistry`, ...). With a lower bound, an older Fabric
API makes the game crash instead of saying "update Fabric API".
