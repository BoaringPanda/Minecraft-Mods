# Extra Blocks — Fabric mod

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
- Mod ID is `extra_blocks` (snake_case). Java package is `com.boaringpanda.extrablocks` (no underscore).
- Build an `Identifier` with the helper `ExtraBlocks.id("some_path")` rather than by hand.
- Assets live under `src/main/resources/assets/extra_blocks/`.
- Data (recipes, loot tables, tags) under `src/main/resources/data/extra_blocks/`.
- Mixins must be listed in `extra_blocks.mixins.json` / `extra_blocks.client.mixins.json`
  or they will not be applied. Prefer the Fabric API event hooks over a mixin when one exists.
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
soul torch, copper torch, lantern, soul lantern, copper lantern, redstone torch (always the "lit"
look - see below), end rod (always "facing=up" - it's already the base, unrotated model), the 13
wood signs (blank, no text - see below), and the flower pot (always empty - see below).

**Verify light levels rather than guess them** - checked each of these against the wiki before
using it, since getting one wrong is an easy, easy-to-miss mistake: torch/end rod 14, copper
torch 14 (copper doesn't oxidize on a torch - "used as fuel, not the base", per the wiki), lantern/
copper lantern 15 (copper lantern's brightness is oxidation-independent, unlike copper bulbs),
soul torch/soul lantern 10, redstone torch 7.

**Redstone torch is decorative only, not a real circuit component.** A real one inverts based on
whether the block below is powered; this one is always the "lit" model
(`minecraft:block/redstone_torch`, the 7-element glowing-tip version - not
`redstone_torch_off`'s plain stick). Making it a real power source was explicitly descoped when
this was built (see the git history for that decision) - if that ever changes, it needs real
`BlockState`/neighbor-update logic, not just a texture swap.

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

## Signs on a lily pad (blank/not writable)

These are plain `LilyPadAccessoryBlock`s, same technique as torch/lantern - **not** `SignBlock`
subclasses, and deliberately so: a sign's text is real per-instance BlockEntity data, explicitly
descoped (see "Where this stops working" below). Since we're not preserving that data, there's no
need for the real 16-value `rotation` blockstate property either - one fixed model, just like end
rod's fixed "facing=up".

`models/block/lily_pad_with_<wood>_sign.json`, one per wood type (13 - all the wood slab
materials plus bamboo and poplar), using vanilla's `template_sign_rot_0` elements (post + board) -
**keep the board element's `"rotation": {"angle": 0.0001, ...}`** when copying it in; that's not a
typo in the vanilla file, it's a deliberate near-zero rotation forcing the renderer to treat the
board's two faces distinctly instead of as a z-fighting-prone flat double-sided quad.

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

## Breaking a lily pad accessory: two stages, real tool per accessory

Breaking a combo doesn't destroy the whole thing in one hit - the first hit removes just the
accessory (dropping it, leaving a plain lily pad behind); only a *second* hit, now on an ordinary
lily pad with no code of ours involved, removes that too. Which tool actually works, and whether
the wrong one still lets the accessory drop, varies by accessory - matching what a player expects
for that real thing (pickaxe for lanterns, axe for signs, no tool needed for candles/torches/end
rod/sea pickle/flower pot/potted plants), not a single blanket rule.

- `block/LilyPadAccessoryBreaking.java` - hooks `PlayerBlockBreakEvents.BEFORE`, **not**
  `Block.playerWillDestroy`. Checked by disassembly before writing any of this: `playerWillDestroy`'s
  return value is captured by the caller but never written back to the world - the block gets
  removed to air immediately afterward regardless of what it returns - so it's structurally
  useless for "become something else instead of vanishing." `PlayerBlockBreakEvents.BEFORE`
  returning `false`, by contrast, is confirmed (same way - disassembling the Fabric API mixin that
  implements it) to skip vanilla's *entire* destroy sequence before any of it runs: no removal, no
  drops, no XP. That clean slate is what gets substituted with "drop just the accessory, set the
  block to a plain lily pad, done."
- `block/custom/LilyPadCombo.java` - a small interface (`accessoryDrops(BlockState)`) every combo
  block implements, since they don't share a common superclass to hang this on (`LilyPadAccessoryBlock`
  directly, `LilyPadCandleBlock extends CandleBlock`, `LilyPadSeaPickleBlock extends SeaPickleBlock`).
  Lets `LilyPadAccessoryBreaking` treat all of them the same way.
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
nothing." So `requiresCorrectToolForDrops()` on `LILY_PAD_WITH_LANTERN`/`SOUL_LANTERN`/
`COPPER_LANTERN` (pickaxe) and on every sign (axe) is a **deliberate deviation from vanilla
fidelity**, not a mistake - it's what was actually asked for (removing something should require
"the right tool," pickaxe for lanterns, axe for signs), applied consistently across each whole
category rather than block-by-block. Hardness values themselves *do* match vanilla exactly
(lantern family 3.5, sign 1, candle 0.1, everything else 0) - only the tool-gating is the
deliberate part. Keep this distinction in mind before assuming any other accessory needs the same
treatment - candles/torches/end rod/sea pickle/flower pot/potted plants are *correctly* left
without `requiresCorrectToolForDrops()`, matching real vanilla exactly, not an oversight.

## Where this stops working: player heads and banners

**Not built** - right-clicking a lily pad with a player head or banner currently does nothing
(falls through to normal placement). Checked why before attempting anything: `minecraft:models/
block/skull.json` and `.../banner.json` are both **completely empty** - no `elements` at all,
just a placeholder particle texture. Unlike signs/flower pots (which have real static geometry
for the blank/plain form, with only the *extra* data - text, pattern - being BlockEntity-driven),
heads and banners have **no static geometry to copy at any fidelity**, not even a generic/blank
one - the entire visible shape is drawn procedurally by a `BlockEntityRenderer` reading stored
per-instance data (whose player's profile; the banner's base color and pattern layers - even the
banner's *base color* isn't in any model, only in its BlockEntity). Copying "just the simple
part" the way signs/flower pots allowed isn't available here; going the multipart-`BlockEntity`
route is a much bigger undertaking than everything else in this feature combined, and wasn't
something the earlier decision to keep this feature static-model-only anticipated needing to
weigh for these two specifically. Get a decision from the user before starting that.

## Commands
Run from the project root. `JAVA_HOME` must point at the JDK 25 install.

- `./gradlew build` — compile + produce the jar in `build/libs/`
- `./gradlew runClient` — launch a dev Minecraft client with the mod loaded
- `./gradlew runServer` — launch a dev dedicated server
- `./gradlew clean` — wipe build output

The distributable jar is `build/libs/extra-blocks-1.0.0.jar`.
Ignore `*-sources.jar` and anything in `build/devlibs/` — those are not for distribution.

## Version bumps
Versions live in `gradle.properties`. Check https://fabricmc.net/develop for the current
set before changing `minecraft_version` / `fabric_api_version` / `loom_version`, and bump
the `minecraft` and `fabricloader` bounds in `src/main/resources/fabric.mod.json` to match.
