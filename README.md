# BP's Better Vanilla Building

A Minecraft mod for **Fabric**, targeting Minecraft 26.3. (Its internal mod ID is still `extra_blocks`, from its
original name, so worlds made with earlier builds keep their blocks.)

## Requirements

- JDK 25 (`C:\Program Files\Java\jdk-25.0.2`)
- Fabric Loader 0.19.5+ and [Fabric API](https://modrinth.com/mod/fabric-api) 0.160.7+ in-game

## Development

```sh
./gradlew runClient   # launch a dev client with the mod loaded
./gradlew runServer   # launch a dev dedicated server
./gradlew build       # build the jar into build/libs/
```

## Installing the built mod

1. `./gradlew build`
2. Copy `build/libs/BPsBetterVanillaBuilding-<version>.jar` (not the `-sources` one) into your Minecraft
   instance's `mods/` folder.
3. Make sure Fabric API is in `mods/` too.

## License

MIT — see [LICENSE](LICENSE).
