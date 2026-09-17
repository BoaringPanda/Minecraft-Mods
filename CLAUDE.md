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
