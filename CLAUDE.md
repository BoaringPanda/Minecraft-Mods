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

Follow the pattern in `block/ExtraBlocksBlocks.java` (one static field per block, a private
`register()` helper that does `Registry.register` for both the block and its `BlockItem`,
plus a public `initialize()` that's called once from `ExtraBlocks.onInitialize()` — that
call is what forces the class to load and the registration to actually run).

Per-block assets needed for a block to look and behave right in-game (all keyed by the same
block ID, e.g. `testblock1`):

- `assets/extra_blocks/blockstates/<id>.json` — which model to render
- `assets/extra_blocks/models/block/<id>.json` — usually `"parent": "minecraft:block/cube_all"`
  + a texture reference
- `assets/extra_blocks/textures/block/<id>.png` — the actual texture; missing = purple/black
  checkerboard placeholder, everything else still works
- `assets/extra_blocks/items/<id>.json` — the client item, so it renders in inventory/hand
- `assets/extra_blocks/lang/en_us.json` — `"block.extra_blocks.<id>": "Display Name"`
- `data/extra_blocks/loot_tables/blocks/<id>.json` — drop itself when broken (otherwise no drop)
- `data/minecraft/tags/mineable/<tool>.json` — add `"extra_blocks:<id>"` to `pickaxe`/`axe`/
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
- `block/MixedSlabBlocks.java` — the curated `MATERIALS` list and the registration loop.
  Generates one block per *ordered* pair (order matters — oak-bottom/stone-top ≠
  stone-bottom/oak-top), skipping a material paired with itself. No `BlockItem` — these are
  never obtainable directly, only ever the result of combining two slabs.
- `block/MixedSlabInteraction.java` — the `UseBlockCallback` that detects the combine click
  and swaps the single slab for the right registered combo block.

**To add a material** (e.g. a modded slab, or a vanilla one not on the list yet): add its
`Blocks.*_SLAB` constant + a short id to `MATERIALS` in `MixedSlabBlocks.java`, then
regenerate for every OTHER existing material (both orders):
- one blockstate file per new pairing at
  `assets/extra_blocks/blockstates/mixed_slab_<bottom>_bottom_<top>_top.json`:
  ```json
  {
    "multipart": [
      { "apply": { "model": "minecraft:block/<bottom>_slab" } },
      { "apply": { "model": "minecraft:block/<top>_slab_top" } }
    ]
  }
  ```
  (works for any vanilla slab that follows the standard `<material>_slab` /
  `<material>_slab_top` model naming — true for every material currently on the list; a
  modded slab needs checking, and one with direction-dependent textures like sandstone's
  distinct top/side/bottom faces needs its own model rather than reusing this generic pair)
- add `"extra_blocks:mixed_slab_<bottom>_bottom_<top>_top"` to
  `data/minecraft/tags/mineable/pickaxe.json` for each new pairing

The current 16-material list is 16×15 = 240 generated blockstate files. Writing that many by
hand isn't the move — generate them (a short loop, in whatever tool is to hand) rather than
typing each one out.

## Lily pad accessories (torch/lantern standing on a lily pad)

Same technique as mixed slabs, much smaller scope (currently 2 combos, not 240): right-clicking
a placed lily pad's top face with a torch or lantern combines them into one
`LilyPadAccessoryBlock` occupying the lily pad's own space, instead of the item placing normally
in the block above.

- `block/custom/LilyPadAccessoryBlock.java` — the block. No baked model of its own; the
  blockstate JSON's `multipart` layers the *unmodified* vanilla `lily_pad` model with the
  accessory's own unmodified model. Collision/outline shape is copied from the real vanilla
  `LilyPadBlock` (`Block.column(14.0, 0.0, 1.5)` — verified from the compiled class, not
  guessed) so it stands on exactly like a normal lily pad.
- `block/LilyPadAccessories.java` — registers one block per accessory and the
  `Block -> LilyPadAccessoryBlock` lookup. Properties (light level, hardness, tool
  requirement) are copied from the accessory's own vanilla values, not the lily pad's.
- `block/LilyPadAccessoryInteraction.java` — the `UseBlockCallback` that detects the combine
  click (top face only) and swaps the lily pad for the right combo block.

No manual pixel offset in the model: checked against the actual vanilla files, `torch`/`lantern`
both render from y=0 of their own cell already, and `lily_pad`'s render plane is at y=0.25 (out
of 16) — close enough to flush that layering both unmodified already looks right. If it ever
looks off in-game, nudge the accessory model's Y in the blockstate (`"apply": {"model": ...,
"x"/"y": ...}` doesn't do vertical offsets — that needs a tiny wrapper model with a `"y"`-shifted
element, not a blockstate-level trick).

**Preserving the lily pad's random rotation:** vanilla `lily_pad.json` picks one of 4 unweighted
`y: 0/90/180/270` model variants per block, and that pick is a hash of the block's *position*
only (`BlockBehaviour.getSeed` defaults to `Mth.getSeed(pos)`, ignoring the block/state — checked
against the compiled game; neither `LilyPadBlock` nor `VegetationBlock` override it). So our
multipart's first entry uses that exact same 4-variant array (copied verbatim from
`lily_pad.json`, not simplified to one fixed rotation) — same position, same hash, same rotation
picked before and after combining. `multipart`'s `apply` accepts an array the same way `variants`
does (confirmed against vanilla's own `chorus_plant.json`); simplifying this to a single
unrotated entry is what caused the lily pad to visibly snap to a fixed orientation on combine.

**To add another accessory** (e.g. a soul lantern): add a `register(...)` call in
`LilyPadAccessories.java` with properties copied from that block's own vanilla values, and a
`lily_pad_with_<name>.json` blockstate following the pattern above (multipart, first entry the
4-variant lily pad array, second the accessory's own model). Add to
`data/minecraft/tags/mineable/pickaxe.json` only if that accessory itself needs a pickaxe.

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
