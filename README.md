# Minecraft Mods

My Minecraft mods. Each mod has its own folder with all of its code, and its own `jars/` folder for finished jars.

| Mod | Folder | What it is |
| --- | --- | --- |
| BP's Better Vanilla Building | [BPsBetterVanillaBuilding/](BPsBetterVanillaBuilding/) | Fabric mod for Minecraft 26.3: mixed slabs and lily pad accessories. |

## How the repo is laid out

```
Minecraft-Mods/
├── README.md                 this file
├── .github/workflows/        automatic builds (GitHub only reads this from the repo root)
└── <ModName>/                one folder per mod
    ├── src/, build.gradle, gradlew, ...   the mod's code and build setup
    └── jars/                 finished jars (on my PC only, never uploaded)
```

Jars are deliberately not in the repo: `.gitignore` excludes `jars/` and `*.jar`.

## Adding another mod

1. Make a new folder next to the existing one, with its own `build.gradle` and `gradlew`.
2. Copy the `build` job in `.github/workflows/build.yml` and change the folder name.
3. Add a row to the table above.
