# BP's Better Vanilla Building

A **Fabric** mod for Minecraft **26.3** that adds small building touches to vanilla, without a big pile of new
item types.

> The mod's internal ID is still `extra_blocks`, from its original name "Extra Blocks". That's deliberate:
> worlds store blocks by ID, so renaming it would turn every placed block into air.

## What it adds

- **Mixed slabs.** Right-click the flat face of a placed slab with a *different* slab and the two merge into one
  full block: one material on the bottom half, the other on top. Works with every vanilla slab.
- **Lily pad accessories.** Right-click the top of a lily pad with one of these to put it on the pad:
  - light sources: torches (normal, soul, copper, redstone), lanterns (including all the copper stages), end rods,
    candles (they stack and light like normal), sea pickles
  - decoration: chains, flower pots (you can plant in them), wood signs (with real text), mob heads, banners
  - lightning rods, which really attract lightning, and the copper ones keep weathering
- **Aim-based breaking.** Look at the accessory to remove only the accessory. Look at the pad to break the pad,
  which takes the accessory with it.

## Playing with it

You need:

1. Minecraft **26.3** with [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.5 or newer
2. [Fabric API](https://modrinth.com/mod/fabric-api) 0.160.7 or newer (for 26.3)
3. The mod's `.jar` file (the one *without* `-sources` in its name)

Put the two `.jar` files (the mod and Fabric API) in your Minecraft `mods` folder, then start the game using the
Fabric profile.

- **Everyone who plays together needs the mod**, and so does the server if you use one. On a LAN world the host
  needs it too.
- **The first load is slow.** About 10,100 mixed-slab blocks are registered at startup.

## What's in this repo

| Folder / file | What it is |
| --- | --- |
| `src/main/` | The mod's code and assets that run everywhere: blocks, textures, models, tags. |
| `src/client/` | Code that only runs on a player's screen (rendering, particles). |
| `build.gradle`, `settings.gradle`, `gradle.properties` | Build settings. `gradle.properties` holds the mod version and the Minecraft version. |
| `gradlew`, `gradlew.bat`, `gradle/` | The build tool's launcher, so nothing needs installing separately. |
| `jars/` | Finished jars, copied here by every build. Kept on the developer's PC only and never uploaded to GitHub. |
| `CLAUDE.md` | Long technical notes for the AI assistant that helps write the code. Not needed to use the mod. |
| `LICENSE` | MIT: anyone may use and change the code, as long as the license notice stays. |

## Branches and versions

- **`main`** is the latest work. Start here.
- **`release-1.0.0`** is a frozen copy of the code as it was when version 1.0.0 was handed out.
- **Tag `v1.0.0`** marks that exact release commit.

## Building it yourself

Needs JDK 25. Run these from this folder (the one containing `gradlew`). The repo's
[`.github/workflows/build.yml`](../.github/workflows/build.yml) makes GitHub build the mod automatically on
every push (see the **Actions** tab).

```sh
./gradlew build       # build the jar into build/libs/ (and copy it to jars/)
./gradlew runClient   # start a test Minecraft with the mod loaded
./gradlew runServer   # start a test server with the mod loaded
```

The finished jar is `build/libs/BPsBetterVanillaBuilding-<version>+<minecraft version>.jar`. Ignore the
`-sources` jar if one appears; it's only a copy of the code.

## License

MIT. See [LICENSE](LICENSE).
