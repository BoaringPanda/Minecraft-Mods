# Minecraft Mods

My Minecraft mods. Every mod lives in the `Mod Creation` folder, each in its own subfolder with all of its code and
its own `jars/` folder for finished jars.

| Mod | Folder | What it is |
| --- | --- | --- |
| Better Vanilla Building | [Mod Creation/BetterVanillaBuilding/](<Mod Creation/BetterVanillaBuilding/>) | Fabric mod for Minecraft 26.3 |

## How the repo is laid out

```
Minecraft Mods/
├── README.md                 this file
├── CLAUDE.md                 notes for the AI assistant: the layout and the "jars stay local" rule
├── .github/workflows/        automatic builds (GitHub only reads this from the repo root)
└── Mod Creation/
    └── <ModName>/            one folder per mod
        ├── src/, build.gradle, gradlew, ...   the mod's code and build setup
        └── jars/             finished jars (on my PC only, never uploaded)
```

Jars are deliberately not in the repo: `.gitignore` excludes `jars/` and `*.jar`.

## Adding another mod

1. Make a new folder inside `Mod Creation`, with its own `build.gradle` and `gradlew`.
2. Copy the `build` job in `.github/workflows/build.yml` and change the folder name.
3. Add a row to the table above.
