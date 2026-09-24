# Minecraft Mods repo

This repo holds several Minecraft mods for one developer (Dylan). Each mod is a separate Gradle project with its
own CLAUDE.md, so read the CLAUDE.md inside a mod's folder before changing that mod.

## Layout

- Every mod lives in `Mod Creation/<ModName>/`. Put any new mod there, never at the repo root.
- Run `gradlew` from inside a mod's own folder (that's where its `gradlew` is), not from the repo root.
- Only repo-wide files sit at the root and must stay there: `.git`, `.github/` (GitHub only reads workflows from
  here), `.gitignore`, `.gitattributes`, `.vscode/`, `README.md` and this file.
- Mods so far: `Mod Creation/BetterVanillaBuilding/`.

## Jars stay on this PC (hard rule from Dylan)

- **Never commit, push or upload jar files to GitHub**: not in git, not as a GitHub Release, not as a workflow
  artifact. The root `.gitignore` excludes `jars/` and `*.jar` (the one exception is each mod's
  `gradle/wrapper/gradle-wrapper.jar`, the build tool's launcher).
- Every mod keeps its finished jars in its own `jars/` folder. Each mod's `build.gradle` has a `copyJarToJars` task
  that runs after `build` to put the jar there. A new mod needs the same task.
- Don't add an upload-artifact step to the workflow, and don't force-add ignored files.

## Adding a mod

1. Create `Mod Creation/<ModName>/` with its own Gradle project, `jars/` folder and `copyJarToJars` task.
2. Copy the `build` job in `.github/workflows/build.yml` and change its `working-directory`.
3. Add a row to the mod table in `README.md`.
