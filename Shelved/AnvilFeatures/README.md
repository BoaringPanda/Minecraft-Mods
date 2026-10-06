# Shelved: anvil features (from VSBetterQOL, 2026-10-07)

Built and tested in VSBetterQOL on 2026-10-06 (Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.160.7+26.3), then taken out on
2026-10-07 because Dylan wants them in a different mod. Everything here worked in-game. The files keep VSBetterQOL's folder layout,
package (`com.boaringpanda.vsbetterqol`) and mod id (`vsbetterqol`). Rename those to the new mod's when moving them in.

## What's in it
1. **Anvil durability**: 24 uses (8 per stage), a bar + `left/24` above the anvil window.
2. **Broken anvils**: a worn-out anvil turns into `broken_anvil` (unusable, drops itself). Right-click with an iron block → new anvil.
3. **Free name tags and repairs**: renaming a name tag is free. Material repairs cost no XP but take 2 anvil uses, and show "Free".

## Files
- `src/main/java/.../AnvilDurability.java`: wear per chunk (data attachment `anvil_wear`), synced uses-left data slot, `View` interface.
- `src/main/java/.../BrokenAnvilBlock.java`, `BrokenAnvils.java`: the block, its registration and the creative tab entry.
- `src/main/java/.../mixin/AnvilMenuMixin.java`: wear counting, broken-anvil swap + sound, free jobs, extra repair wear.
- `src/main/java/.../mixin/LevelChunkMixin.java`: **VSBetterQOL's whole file, which also has the placed-logs code.** Only the
  `BlockTags.ANVIL` → `AnvilDurability.forget` part belongs to the anvil features. Copy just that into the new mod's own
  `LevelChunkMixin` (or make a small one that only does that).
- `src/client/java/.../client/mixin/AnvilScreenMixin.java`: the durability bar and the "Free" label.
- Assets: `broken_anvil` blockstate, item model definition, block model, 2 textures (`broken_anvil.png`, `broken_anvil_top.png`).
- Data: `broken_anvil` loot table, `minecraft:mineable/pickaxe` tag entry.

## Wiring it into a mod
- Mod initializer: `AnvilDurability.register();` and `BrokenAnvils.register();`.
- Common mixins json: `"AnvilMenuMixin"` (+ `"LevelChunkMixin"` if the new mod doesn't have one).
- Client mixins json: `"AnvilScreenMixin"`.
- Lang (`en_us.json`):
  ```json
  "block.vsbetterqol.broken_anvil": "Broken Anvil",
  "container.vsbetterqol.repair.free": "Free",
  "message.vsbetterqol.anvil_broken": "This anvil is broken. Repair it with an Iron Block",
  ```
- Adds a registered block, so clients need the mod to join a server running it.

## How it works (the docs from VSBetterQOL's CLAUDE.md)

### Anvil durability (Dylan, 2026-10-06)
Anvils last a fixed 24 uses instead of vanilla's 12% chance per use (Dylan's pick): 8 per stage, so the 8th use turns an anvil Chipped,
the 16th Damaged, the 24th turns it into a **Broken Anvil** (below) instead of destroying it. Creative doesn't wear them (vanilla skips creative too). The anvil
screen shows a bar above the window plus `uses left/24`. Any `#minecraft:anvil` block; ones vanilla's `AnvilBlock.damage` doesn't
know count as one stage (8 uses).
- `AnvilDurability` keeps a per-chunk map of anvil position (`BlockPos.asLong`) → uses taken in the current stage, as the persistent
  attachment `vsbetterqol:anvil_wear` (same replace-don't-mutate trick as VSBetterQOL's `PlacedLogs`). Uses left = stages left × 8 − that.
- `mixin/AnvilMenuMixin`: `@WrapOperation`s the `nextFloat()` roll in `lambda$onTake$0` (the `access.execute` block in `onTake`) to
  return 0 (damage now) when the stage is used up, else 1. Vanilla only rolls outside creative and on an anvil, so each call is one use.
  It also `addDataSlot`s `AnvilDurability.usesLeftSlot` at the constructor's TAIL: the server works it out from the anvil every tick,
  the client keeps the synced value (-1 = no anvil, bar hidden). The menu implements `AnvilDurability.View` for the screen.
- `LevelChunkMixin` forgets the entry when an anvil turns into a non-anvil (mined, fell, broke, commands).
- `client/mixin/AnvilScreenMixin`: TAIL of `AnvilScreen.extractBackground` draws the villager trade screen's XP bar sprites
  (`container/villager/experience_bar_background`/`_current`, 102 px), centred with the number 9 px above `topPos`, in vanilla's XP
  level green.
- Limits: mining or dropping an anvil keeps its stage (vanilla item/falling block) but resets the uses inside it, e.g. Chipped 10/24 comes
  back 16/24. Anvils from before the feature start at the top of their stage.

### Broken anvils + iron block repair (Dylan, 2026-10-06)
A worn-out anvil becomes `vsbetterqol:broken_anvil` (same facing, vanilla's anvil-destroy sound, menu closes). It has no menu: right-clicking
shows an actionbar hint (`message.vsbetterqol.anvil_broken`). Right-click with an iron block → brand-new `minecraft:anvil` (24/24), uses 1 iron
block (free in creative, `ItemStack.consume`), iron golem repair sound. Sneaking places the iron block instead (vanilla sneak rule). It mines with
a pickaxe and drops itself, so it stays broken when moved. Falls and hurts mobs like an anvil but is never damaged by falling. **Falling stays
vanilla (Dylan's pick):** a damaged anvil can still be destroyed by landing; only use-wear makes broken anvils. In creative Functional Blocks
after Damaged Anvil; no recipe.
- `BrokenAnvilBlock extends AnvilBlock` (shape, facing, placement, falling): `useItemOn` (iron block repair), `useWithoutItem` (hint),
  `getMenuProvider` null. Not in `#minecraft:anvil`, which is what keeps it out of `AnvilMenu.stillValid`, `FallingBlockEntity`'s fall damage
  and the wear tracking. `BrokenAnvils` registers block + item (`ofFullCopy(DAMAGED_ANVIL)`) and the creative tab entry.
- `AnvilMenuMixin` also wraps the `AnvilBlock.damage` call in `lambda$onTake$0` (null → broken anvil with FACING, so vanilla's `setBlock`
  branch runs), and the `levelEvent` calls there (`SOUND_ANVIL_USED` at a now-broken anvil → `SOUND_ANVIL_BROKEN`).
- Assets: `models/block/broken_anvil.json` is vanilla's `template_anvil` with the top split up: north horn snapped to a stub, south-east corner
  knocked down to y 13. Textures `broken_anvil_top.png` / `broken_anvil.png` are vanilla's `damaged_anvil_top` / `anvil` darkened 15% with
  black cracks + light edge pixels added by a script (Dylan can repaint them). Loot table drops self (`survives_explosion`), and it's in
  `#minecraft:mineable/pickaxe`.

### Free name tags and repairs (Dylan, 2026-10-06)
Renaming a name tag (nothing in the second slot) costs no XP; renaming anything else is vanilla. Material repairs (ingots, diamonds, planks…
on a damaged item, vanilla's `isValidRepairItem` branch) cost no XP but take **2 anvil uses**. Repair + rename together costs only what the
rename alone would. Combining two of the same item stays vanilla (Dylan's pick). Free repairs don't raise the item's `REPAIR_COST`, so they
never lead to Too Expensive. Free jobs show "Free" in green where the cost line goes.
- `AnvilMenuMixin`: `@ModifyExpressionValue` on the `Mth.clamp` in `createResult`'s price (`@Local long tax`): name tag → 0; repair
  (`repairItemCountCost > 0`) → the naming part `price - tax - repairItemCountCost`, priced like a vanilla rename (`tax + n`, max 39) or 0.
  `@WrapOperation` skips `calculateIncreasedRepairCost` for repairs. `@ModifyReturnValue` on `mayPickup` allows cost 0 with a result (vanilla
  never has one, so it means a free job, on client and server). `onTake` HEAD adds 1 extra wear (`AnvilDurability.addWear`) for repairs outside
  creative. `AnvilDurability.use` carries wear past a stage's end into the next stage, so 2-use jobs are exact across stages.
- `AnvilScreenMixin`: TAIL of `extractLabels` draws "Free" (`container.vsbetterqol.repair.free`) with vanilla's cost-line box when cost is 0
  and there's a result.
