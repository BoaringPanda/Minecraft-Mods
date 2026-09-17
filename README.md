# Extra Blocks

A Minecraft mod for **Fabric**, targeting Minecraft 26.3.

## Requirements

- JDK 25 (`C:\Program Files\Java\jdk-25.0.2`)
- Fabric Loader 0.19.5+ and [Fabric API](https://modrinth.com/mod/fabric-api) in-game

## Development

```sh
./gradlew runClient   # launch a dev client with the mod loaded
./gradlew runServer   # launch a dev dedicated server
./gradlew build       # build the jar into build/libs/
```

## Installing the built mod

1. `./gradlew build`
2. Copy `build/libs/extra-blocks-<version>.jar` into your Minecraft instance's `mods/` folder.
3. Make sure Fabric API is in `mods/` too.

## License

MIT — see [LICENSE](LICENSE).
