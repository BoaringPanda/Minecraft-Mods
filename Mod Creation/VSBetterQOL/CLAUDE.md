# VSBetterQOL

Fabric mod (display name "VS Better QOL"; mod id `vsbetterqol`, package `com.boaringpanda.vsbetterqol`, jar
`BPsBetterQOL-<version>.jar` via `base.archivesName` in build.gradle) of vanilla quality-of-life tweaks. Sister mod to
VSBetterBuilding and VSLumberjackMod, set up with the same stack and build script. Was "BetterVanillaFeatures"
(`bettervanillafeatures`) until 2026-09-30, then "BetterVanillaQOL" (`bettervanillaqol`, "BP's Better Vanilla QOL") until
Dylan renamed it 2026-10-04 to match the VS series, before any release. Worlds from before the rename lose the
`placed_logs` attachment (its id changed), so logs placed then count as natural.

## Stack
- Minecraft 26.3 (date-based versions, **Mojang mappings**: `net.minecraft.resources.Identifier`, `net.minecraft.client.Minecraft`),
  Fabric Loader 0.19.5, Fabric API 0.160.7+26.3, Loom 1.18.2, Gradle wrapper 9.7.1, Java 25. Versions live in `gradle.properties`.
- Split source sets: `src/main` (common + server) and `src/client` (client only: screens, HUD, tooltips).
- Mixins: `vsbetterqol.mixins.json` (common, `mixin/`) and `vsbetterqol.client.mixins.json` (client only,
  `src/client/.../client/mixin/`, listed in `fabric.mod.json` with `"environment": "client"`).
- Game source for reading: `./gradlew genSources`, then the `*-sources.jar` files under `.gradle/loom-cache/minecraftMaven/net/minecraft/`.

## Commands
Run from this folder (where `gradlew` is), not the repo root. `JAVA_HOME` must point at the JDK 25 install
(`C:\Program Files\Java\jdk-25.0.2`).
- `./gradlew build` builds the mod and copies the jar into `jars/` (git-ignored, jars never go to GitHub).
- `./gradlew runClient` starts a dev game with the mod.

## Farmland can't be trampled (Dylan, 2026-09-26, empty farmland too since 2026-09-30)
Jumping or falling onto farmland never tramples it to dirt, with or without a crop on it, for players and mobs. It started as
crops-only (a `protects_farmland` block tag); Dylan then asked for empty farmland too, so the tag is gone. Farmland still turns to
dirt the other vanilla ways (dried out with nothing planted, a solid block placed on top), and with a hoe (below).
- `mixin/FarmlandBlockMixin` `@WrapWithCondition`s the `turnToBaseBlock` call in `FarmlandBlock.fallOn` with `false`, so only the
  trampling is skipped (fall damage and the rest of `fallOn` still run). No config or tag controls trampling in vanilla, hence the
  mixin.

## No tempt delay (Dylan, 2026-09-26)
Animals follow food the moment it's held, and again straight after it's put away and taken back out (vanilla waits 5 s). All tempting
mobs, which includes strider and sulfur cube since they use `TemptGoal` too. Vanilla had three delays:
- `mixin/TemptGoalMixin` (goal-based: chicken, cow, pig, sheep, rabbit, horses, llamas, panda, turtle, bee, cat, ocelot, strider, sulfur
  cube): `TemptGoal.stop` sets `calmDown` to 0 instead of `reducedTickDelay(100)`. **Exception, Dylan's pick:** a spooked wild ocelot
  or stray cat (player moved/turned fast within 6 blocks, `canScare`) keeps the vanilla wait, since standing still is how they're
  tamed. Spook detection: `canContinueToUse` sets `spooked` at HEAD, and the wrapped `canUse()` call in it clears it. Only the spook
  checks return before that call, so `spooked` is still true in `stop` only after a spook.
- `mixin/FollowTemptationMixin` (brain-based: goat, camel, frog, tadpole, axolotl, armadillo, sniffer, happy ghast, nautiluses):
  `FollowTemptation.stop` never sets `TEMPTATION_COOLDOWN_TICKS`. The `CountDownCooldownTicks` behaviours in the `*Ai` classes then
  have nothing to count.
- `mixin/TemptingSensorMixin` + `mixin/SensorAccessor`: `TemptingSensor` scans every tick instead of `Sensor`'s default 20, so brain
  animals notice food at once, like goal-based ones (checked every 2 ticks).

## Pickaxes break glass faster (Dylan, 2026-09-26)
All glass and glass panes (plain, stained, tinted) break faster with a pickaxe. Wooden is 2x hand speed and each tier is 1.5x the one
before: wood 2, stone 3, copper 3.5, iron 4.5, diamond 6.75, netherite 10.125 (instant), and gold matches netherite. Drops don't change
(still Silk Touch only).
- `ToolSpeedRules` uses Fabric's `DefaultItemComponentEvents.MODIFY` to put a `Tool.Rule.overrideSpeed` in front of each vanilla
  pickaxe's `minecraft:tool` rules (speed only, like vanilla's sword-on-cobweb rule), so Efficiency, Haste etc. still stack through
  vanilla's mining code. No mixin.
- Which blocks: block tag `vsbetterqol:breaks_faster_with_pickaxe` = `#c:glass_blocks` + `#c:glass_panes` (Fabric convention
  tags, so other mods' glass counts too if they tag it). Only the 7 vanilla pickaxes get it, because item tags aren't loaded when the
  event runs. The same class adds the axe bamboo rule (below).

## Removed: renaming name tags in hand (Dylan, 2026-10-07)
Right-click a name tag → rename screen, costing an ink sac (added 2026-09-27) was taken out in the 2026-10-07 clean-up. Name tags are
vanilla again (anvil only). Don't bring it back unless Dylan asks.

## Swords are weapons, axes cut bamboo (Dylan, 2026-09-27)
Swords hit mobs through grass/flowers/small plants and can't break any block except cobwebs and bamboo. Axes break bamboo instantly
too (since 2026-10-05 swords keep their vanilla bamboo insta-break as well, Dylan's call).
All `#minecraft:swords` items count (other mods' swords too).
- Can't break: `mixin/PlayerMixin` `@ModifyReturnValue`s `Player.blockActionRestricted` (vanilla's adventure-mode check, used by
  client and server start/continue/destroy) to also return true for a sword on anything outside block tag
  `vsbetterqol:swords_can_break` (= cobweb + vanilla's `#minecraft:sword_instantly_mines`, i.e. bamboo + bamboo sapling). So a
  sword gets no cracks and no break, like adventure mode. Bamboo then breaks instantly through the sword's own vanilla tool rule;
  its leaves speed rule is never reached. Creative: vanilla swords break nothing there (`Tool.canDestroyBlocksInCreative`
  false), so `ToolSpeedRules` sets it true on the 7 vanilla swords (item tags aren't loaded for that event) and cobwebs and
  bamboo break in creative too; everything else is still stopped by `blockActionRestricted`, which vanilla checks before the creative break.
- Can't hit decorations: `PlayerMixin` also `@Inject`s at HEAD of private `Player.cannotAttack` (checked at the top of
  `Player.attack`, client and server) for a sword on entity-type tag `vsbetterqol:swords_cant_break` (cushion, painting,
  item frame, glow item frame, armor stand, mannequin), so the hit does nothing at all (no item pop, no wobble, no break). HEAD,
  not the return value, because vanilla's `skipAttackInteraction` inside that check is what breaks item frames, paintings and
  cushions. A `@WrapOperation` on `hurtServer` in `doSweepAttack` returns false for them, so sweeps don't hurt or knock them back
  either. Creative included. They still block the crosshair (no hit-through). Hand/other tools are vanilla.
- Hit through: `client/mixin/LocalPlayerMixin` on `LocalPlayer.pick` (the crosshair pick: nearest block first, then entities only
  closer than it). With a sword, the `Entity.pick` block ray is swapped for a clip that treats block tag
  `vsbetterqol:swords_hit_through` as empty, so vanilla's own entity search reaches the mob behind the plant. If no
  entity is found, the normal (plant) block hit is returned, so the outline is vanilla. Client only: the server's `handleAttack`
  only checks distance, not line of sight. The tag: grass/ferns (incl. dry grass), `#minecraft:small_flowers` + tall flowers,
  bushes, the vanilla saplings one by one (not `#minecraft:saplings`, which has the azaleas), `#minecraft:crops`, sweet berries, sugar cane, seagrass, nether
  roots/sprouts/fungi/wart, weeping + twisting vines (both the tip and plant blocks), hanging roots, mushrooms. Wither rose
  comes in via small_flowers. Also glow berry vines (`#minecraft:cave_vines`), vine, pale hanging moss, spore blossom, cactus
  flower, kelp (+ kelp_plant), and all coral plants/fans/wall fans, live (`#minecraft:corals`, `#minecraft:wall_corals`) and dead
  (listed one by one, vanilla has no dead-coral tag). Coral blocks are left out (solid blocks). Plus red shrub, big dripleaf
  (+ its stem) and small dripleaf. **Taken out (Dylan, 2026-10-05):** azalea, flowering azalea, pink petals, wildflowers, leaf
  litter. Since the saplings are listed one by one, other mods' saplings no longer count.
- Bamboo: `ToolSpeedRules` puts `Tool.Rule.overrideSpeed(#vsbetterqol:axe_instantly_mines, Float.MAX_VALUE)` (the rule
  vanilla's sword has) in front of the 7 vanilla axes' rules. Tag = bamboo + bamboo sapling. Speed only, drops unchanged.
  Swords keep their own vanilla bamboo rule as well (see Can't break).

## Right-click harvest (Dylan, 2026-09-27)
Right-clicking a fully grown crop drops what breaking it would (Fortune on the held item counts) and replants it at stage 0. Wheat,
carrots, potatoes, beetroots (any `CropBlock`, so other mods' crops too), nether wart and cocoa take the replant seed out of their own
drops. A grown pitcher plant and torchflower drop no seed, so they use a pitcher pod / torchflower seed from anywhere in the inventory
(creative: free); with none, you still get the flower and the spot is left empty (Dylan's pick). Torchflowers are only picked where a
torchflower seed could be planted (farmland), so decorative ones aren't touched.
- `mixin/BlockBehaviourMixin` `@Inject`s at HEAD of `BlockBehaviour.useWithoutItem`, the step sweet berry bushes are picked in. None of
  these crops override it. Vanilla only calls it for the main hand, not while sneaking with an item, and before the held item's use,
  so all of that works like sweet berries. Client just returns SUCCESS (arm swing); the server does the work.
- `CropHarvesting` finds the harvest (pitcher: either half, drops come from the lower one) and does the drops
  (`Block.getDrops` with player + main-hand item), `spawnDestroyParticles` (vanilla break sound + particles), replant and
  `GameEvent.BLOCK_CHANGE`. Pitcher's lower half is replanted before the upper is removed, because vanilla breaks a grown lower half
  whose top goes missing. No tool damage (vanilla doesn't damage hoes on crops).

## Fast leaf decay (Dylan, 2026-09-27)
Leaves cut off from their logs (whole trunk broken) are all gone within about 1.5 s, in a random wave, instead of waiting for a random tick
(~68 s on average in vanilla). Drops are vanilla's. Player-placed leaves are `PERSISTENT`, so vanilla's `decaying()` is false and
they never decay, same as vanilla (custom trees are safe). Which blocks hold leaves up is vanilla's `#minecraft:prevents_nearby_leaf_decay`.
- `mixin/LeavesBlockMixin` on `LeavesBlock.tick` (the scheduled tick vanilla uses to recompute `DISTANCE`, which spreads through the
  tree at one block per tick). TAIL: if the leaf is now `decaying()`, schedule another tick 8-30 ticks later
  (was 2-50, then 2-35 from 2026-09-30; 8-30 since 2026-10-04 to match VSLumberjackMod's `FallingCanopy` on purpose, since
  both mods are meant to be installed together: change one, change the other). HEAD: if it's still
  `decaying()` after a fresh `updateDistance` (both `@Shadow`ed), `dropResources` + `removeBlock` like `randomTick`, and cancel.
  Only a natural log growing next to it in the meantime saves it. Only one tick per pos+block can be pending (`LevelChunkTicks.schedule`),
  so neighbours' 1-tick updates can't bring the decay forward.
- **Player-placed logs never hold up natural leaves** (Dylan's pick, placed before or after the trunk is broken). `PlacedLogs` keeps a
  per-chunk set of placed-log positions (`BlockPos.asLong`) as a persistent Fabric data attachment `vsbetterqol:placed_logs`
  (immutable set, always replaced so `setAttached` marks the chunk unsaved; empty → `removeAttached`). Server only.
  - Remember: `mixin/BlockItemMixin` wraps the `setPlacedBy` call in `BlockItem.place` (only reached after a real placement).
  - Forget: `mixin/LevelChunkMixin` wraps `LevelChunkSection.setBlockState` in `LevelChunk.setBlockState` (every block change in a
    loaded chunk): old state a log and new state not → forget. So no stale spots (a sapling growing where a placed log burned is
    natural). Log → log (axe stripping) stays remembered.
  - Leaves: `LeavesBlockMixin` wraps `getDistanceAt` inside `updateDistance` (`@Local` the `MutableBlockPos` neighbour) and returns
    `DECAY_DISTANCE` for a remembered log, so it counts as nothing.
  - Limits: logs placed before this feature, via `/setblock` `/fill` WorldEdit, or pushed by a piston count as natural.

## Durability in tooltips (Dylan, 2026-10-06)
Damaged items show `Durability: X / Y` in their tooltip without F3+H (Advanced Tooltips), coloured like the bar under the item (green →
yellow → red). Undamaged items show nothing, like vanilla (Dylan's pick). F3+H still adds vanilla's item ID and component count; durability
shows once either way. No toggle yet (Dylan may add one later). Client only, so it works on servers without the mod.
- `client/mixin/ItemStackMixin` on `ItemStack.addDetailsToTooltip`: `@Inject` at the `TooltipFlag.isAdvanced()` call (just before
  vanilla's F3+H block) adds vanilla's `item.durability` text `.withColor(getBarColor())` when `isDamaged()` and
  `display.shows(DAMAGE)` (so `tooltip_display`-hidden damage stays hidden). `@ModifyExpressionValue` on the method's only `isDamaged()`
  call → false skips vanilla's own uncoloured line.

## About to break warning (Dylan, 2026-10-07)
When a player's item drops to 7 durability or less, a red "Your axe is about to break" shows above the hotbar (actionbar) with a quiet
note block bass, only for that player. It fires once per drop: only when crossing from above 7 (a hit from 9 to 5 still warns, and it
doesn't repeat at 6, 5...). After Mending or an anvil takes it back above 7, the next drop warns again. Creative never loses durability,
so it never warns. Server side, so it needs the mod on the server. Players without it still read English (`translatableWithFallback`).
- `mixin/ItemStackMixin` (common; the client one with the same name is the tooltip): HEAD of private `ItemStack.applyDamage`, where both
  `hurtAndBreak` and `hurtWithoutBreaking` set the new damage after Unbreaking. Calls `DurabilityWarning.onDamage` when there's a player.
- `DurabilityWarning`: the item's word comes from an ordered list. Item tags come first (axes, pickaxes, shovels, hoes, swords, spears,
  head/chest/leg/foot armor, so other mods' gear counts if tagged), then the other vanilla damageable items one by one (bow, crossbow,
  trident, mace, shield, elytra, fishing rod, shears, flint and steel, brush, carrot/warped fungus on a stick). Anything else gets
  "Your <item name> is about to break". Each type has its own lang key `message.vsbetterqol.about_to_break.<type>`, so the grammar is
  right ("leggings/boots/shears are"). The sound is sent with `ClientboundSoundPacket`, because `Player.playSound` skips that player.
  Wolf armour has no player, so it never warns.

## Sort button (Dylan, 2026-10-07)
A small 10x10 "Sort A-Z" button sits just above the top-right slot, centred on it (`slot.x + 3`, `slot.y - 13`, Dylan's
screenshot 2026-10-07). In the survival inventory it sorts the 27 main slots (hotbar,
armour and offhand stay put, Dylan's pick). Chests, barrels, ender chests, chest minecarts/boats (all `ChestMenu`) and shulker boxes get two:
one above the container's top-right slot, and one for the player's inventory. Order: hover name A-Z ignoring case (renamed items by their
name), then item id, then bigger stacks first. Same item + components merge first, so partial stacks join up and empties end up at the
bottom. Creative gets the player button too (Dylan, 2026-10-07), only on the Survival Inventory tab (other tabs don't show the main
inventory). Needs the mod on the server (the button only shows if it can send the packet).
- Creative: `CreativeModeInventoryScreen`'s menu is client-only, so its button sends `player.inventoryMenu.containerId` (what the server
  has open in creative). On the inventory tab its `SlotWrapper`s use the menu index as the container slot, and 9..35 is the main
  inventory both ways, so `sortableSlots` finds them. Tabs swap the slots without a re-init, so the button is looked up every frame and
  hidden when there's no slot.
- `InventorySorting` (main): `sort_inventory` payload (`containerId`, `playerInventory`). The server re-checks the open menu's id and not
  spectator. `sortableSlots` (shared with the client) = menu slots whose container is the player's `Inventory` with container slot 9..35,
  or for `ChestMenu`/`ShulkerBoxMenu` the other slots. It writes back with `Slot.set`, then `broadcastChanges`.
- `client/SortButtons`: Fabric `ScreenEvents.AFTER_INIT` for `InventoryScreen`/`ContainerScreen`/`ShulkerBoxScreen` adds an `ImageButton`
  (sprites `vsbetterqol:sort_button(_highlighted)`) via `Screens.getWidgets`, above `sortableSlots(...).get(8)`. `beforeExtract` re-places
  it every frame from `client/mixin/AbstractContainerScreenAccessor` (`leftPos`/`topPos`), because the recipe book moves the GUI without
  a re-init.

## Shift-drag quick-move (Dylan, 2026-10-07)
Holding shift and dragging the left mouse across slots quick-moves each one, as if each was shift-clicked (Mouse Tweaks style). Every
container screen. A slot moves each time the cursor enters it (not while it stays inside), so dragging back over where the items went
moves them back without letting go (Dylan, 2026-10-07; it was once per drag before).
Creative: works on the Survival Inventory tab; the item list is skipped (a single shift-click there is still vanilla's full stack on the
cursor), and the hotbar on item tabs gets cleared like vanilla shift-click does there (Dylan's pick). Client only.
- `client/mixin/QuickMoveDragMixin` on `AbstractContainerScreen`: `mouseClicked` HEAD starts a drag (the pressed slot is vanilla's own
  shift-click). `mouseDragged` HEAD, when left button + live `Minecraft.hasShiftDown()` (drag events keep the press's modifiers) + empty
  cursor: walks from the last position to this one in 4px steps (fast swipes skip nothing) and calls the overridable
  `slotClicked(slot, slot.index, 0, QUICK_MOVE)` for each slot it enters (`lastSlot` changes).
- **26.x input is SDL:** left mouse is `InputConstants.MOUSE_BUTTON_LEFT` = 1 (right = 3), not GLFW's 0. The container-click button passed
  to `slotClicked` is still 0 = left (vanilla's `getContainerClickButton` maps it).
- `client/mixin/CreativeModeInventoryScreenInvoker`: calls private `isCreativeSlot` to skip the item list.

## Status effects on the HUD (Dylan, 2026-10-07)
Active effects show in a column in the top right instead of vanilla's rows: each is vanilla's icon box at 3/4 size and the time at half size (Dylan's picks, so more fit; ambient background for beacons,
same last-10-seconds blink) with the time left to its left (the inventory's `m:ss` / infinity text). Good effects first, then the rest
(Dylan's picks: compact icon + time, good first). A full column carries on in a new column to its left, so 40+ effects still fit. Columns
stop 50px above the bottom so they never cover the hotbar/hearts/food. Vanilla's effect list beside the
survival and creative inventories is removed (Dylan: messy), so the column stays visible behind those screens instead. Client only.
- `client/mixin/EffectHudMixin`: HEAD of private `Hud.extractEffects`, always cancelled (replaces the layout), keeping vanilla's early-outs.
  Vanilla's `Ordering.natural().reverse()` order and `showIcon()` filter, then a stable beneficial-first split. Box + icon drawn at vanilla's
  24/18px inside a `pose()` `scale(0.75)` (`ICON_SCALE`), so 19px rows; the time text in its own `scale(0.5)` (`TEXT_SCALE`).
  Each column is as wide as its widest time, next column 4px to its left. Sprites `hud/effect_background(_ambient)`,
  `Hud.getMobEffectSprite`, time `MobEffectUtil.formatDuration(instance, 1, tickrate)`.
- `client/mixin/EffectsInInventoryMixin`: `EffectsInInventory.extractRenderState` cancelled (no list, no hover tooltip) and `canSeeEffects`
  → false. Only `InventoryScreen` and `CreativeModeInventoryScreen` use it; their `showsActiveEffects()` returns `canSeeEffects()`, which
  is vanilla's "hide the HUD effects" check, so the HUD column keeps drawing under them.

## Coloured armor bar (Dylan, 2026-10-07)
Each worn armor piece colours the armor points it gives (1 armor = half an icon) in its material's colour, helmet first from the left
like vanilla fills the bar: leather brown (dye ignored, Dylan's pick), chainmail, iron (vanilla's colour), gold, diamond, netherite, turtle,
copper. Points from anything else (modded armor, `/attribute`) stay vanilla. Client only.
- `client/mixin/ArmorBarMixin`: TAIL of private static `Hud.extractArmor` (same x/y maths as vanilla). Points per piece = the item's
  `ADD_VALUE` `Attributes.ARMOR` modifiers for its slot (`ItemStack.forEachModifier`); colour from `Equippable.assetId()` → `COLORS`.
- Sprites `hud/armor_left.png` / `armor_right.png`: vanilla's armor icon halves (columns 0-4 / 5-8) with a white body, grey shade and no
  highlight (Dylan didn't want the shine), tinted with the ARGB `blitSprite`. Made with a one-off Java program.
- The armor attribute is only recalculated on the server, so after equipping, the client's armor value lags a tick or two behind the
  worn items. The mixin keeps the last colours until the value catches up (or 500 ms pass), so the colours change in the same frame as
  vanilla's bar instead of flashing.

## Saturation display (Dylan, 2026-10-07)
Saturation (hidden in vanilla) shows as a bright green outline around the hunger shanks, AppleSkin-style: 1 saturation = half a shank,
right to left like the shanks. Hovering a food shows two rows of icons under its name: hunger shanks, then saturation as green-outlined empty
shanks (rounded to the nearest half; Dylan picked icons over numbers). If saturation rounds to 0 (cookie, pufferfish...) there's only the
hunger row, and no empty space for the other. Holding food you can eat (main hand, else offhand; `player.canEat(canAlwaysEat)`) slowly flashes
the shanks and outlines eating it would add (alpha 0.2-0.8, 2 s sine), using vanilla's `FoodData.add` maths: food capped at 20, saturation at the new
food level. Client only.
- Sprites `textures/gui/sprites/hud/saturation_full.png` / `saturation_half.png`: vanilla's `hud/food_empty` black border pixels turned
  #55FF55. Half = the border pixels with x + y >= 8 (the lower-right part, like vanilla's half shank), made with a one-off Java program.
- `client/mixin/HudMixin`: TAIL of `Hud.extractFood` draws them at vanilla's shank spots (`xRight - i*8 - 9`, `yLineBase`). Shanks only
  jiggle at 0 saturation, when there's nothing to draw. The eating preview is drawn in the same inject with the ARGB `blitSprite` overload,
  only where the "after" sprite differs from the current one (Hunger effect sprites when the player has Hunger).
- Tooltip: `client/mixin/ItemStackTooltipMixin` `@ModifyReturnValue`s `ItemStack.getTooltipImage` to `client/FoodTooltip` for anything with
  `DataComponents.FOOD`, unless it already has an image. `ClientTooltipComponentCallback` (in `VSBetterQOLClient`) maps it to
  `client/ClientFoodTooltip`, drawn with the HUD sprites. Vanilla puts the image right under the name.

## Enchanted book descriptions (Dylan, 2026-10-06)
Hovering an enchanted book shows "Hold Shift for info". Holding Shift shows what each enchantment does, under its name (dark gray, wrapped at
200 px, indented by a space). Books only, so enchanted gear stays vanilla (Dylan's pick). Descriptions are Dylan's own text with only the typos
fixed. Client only.
- Text: `enchantment.<namespace>.<id>.desc` in `assets/vsbetterqol/lang/en_us.json`, the common convention other mods use. All 43 vanilla 26.3
  enchantments have one. Enchantments with no key show nothing. If a book has no keyed enchantments at all, there's no Shift line either. If an
  update adds an enchantment, add its key.
- `client/mixin/ItemEnchantmentsMixin` on `ItemEnchantments.addToTooltip`: `@WrapOperation` on both `Consumer.accept` calls (vanilla's two
  name loops, `@Local Holder<Enchantment>`) adds the description after the name. A TAIL `@Inject` adds the Shift hint. "Is a book" =
  `components.get(STORED_ENCHANTMENTS) == this`.

## Blast furnaces smelt blocks too (Dylan, 2026-10-07)
Blast furnaces do every non-food furnace recipe (Dylan's list plus the ones he missed, chorus fruit included at his request): stone,
deepslate, cracked bricks/tiles, glass, brick, terracotta + all 16 glazed, nether brick, smooth basalt/stone/sandstones/quartz, charcoal,
sponge, green + lime dye, leaf litter, popped chorus fruit, resin brick. Food stays smoker/furnace only. Same XP and blast furnace speed.
- No code: 39 `data/vsbetterqol/recipe/*_from_blasting.json`, each vanilla's smelting recipe with `type` → `minecraft:blasting`
  (vanilla's 26.3 blasting recipes all use `cookingtime` 200 too). Picked by a one-off script: every vanilla smelting recipe with no
  blasting and no smoking version (smoking = food). Iron nuggets were skipped, since vanilla already blasts them (same items, other order).
- 39 recipe-book unlocks in `data/vsbetterqol/advancement/recipes/<vanilla's folder>/`: vanilla's advancement for the smelting recipe with
  the recipe id swapped, so they show in the blast furnace recipe book when the furnace one unlocks.
- If an update adds or changes a vanilla smelting recipe, regenerate these from the new jar.

## Lapis stays in enchanting tables (Dylan, 2026-10-07)
Lapis left in an enchanting table is still there next time, Bedrock-style. It belongs to the table (Dylan's pick): anyone who opens it gets it,
it's saved with the world, and breaking the table drops it. The enchanted item still comes back to you on close, like vanilla. No hoppers.
- `EnchantingLapis`: persistent Fabric data attachment `vsbetterqol:lapis` (`ItemStack.CODEC`) on the `EnchantingTableBlockEntity`.
  `take` removes and returns it, `store` merges up to a full stack (the leftover stays in the given stack). Both `setChanged()`.
- `mixin/EnchantmentMenuMixin`: constructor TAIL `take`s the table's lapis into `enchantSlots` slot 1, so it lives only in that menu while
  open (a second player opening the same table sees none, no dupes). `@WrapOperation` on `clearContainer` in `lambda$removed$0`
  `store`s slot 1 back first, then vanilla gives the rest to the player (also all of it if the table is gone). Server only: client menus have
  `ContainerLevelAccess.NULL`.
- `mixin/BlockEntityMixin`: HEAD of `BlockEntity.preRemoveSideEffects` (where containers drop their items) drops an enchanting table's stored
  lapis.

## Double doors open together (Dylan, 2026-10-07)
Right-clicking one door of a double door opens or closes the other one too. Sneak-click (empty hand) only moves the clicked door (Dylan's pick).
Any two hand-openable doors pair up, e.g. oak + spruce or wood + copper (Dylan's pick). Iron doors still need redstone. Only right-clicks:
redstone, villagers and wind charges are vanilla.
- `mixin/DoorBlockMixin`: TAIL of `DoorBlock.useWithoutItem` (the final `return SUCCESS`, after vanilla toggled; iron doors `PASS` earlier).
  Partner = `pos.relative(hinge == RIGHT ? facing.getCounterClockWise() : facing.getClockWise())`, which is how vanilla's `getHinge` hinges
  a door placed next to another. It must be a `DoorBlock` with `canOpenByHand`, the same `FACING` and `HALF`, and the opposite `HINGE`. Then
  `setOpen(player, …, newOpen)` (vanilla's sound + game event), so out-of-sync pairs end up matching. Runs client and server like vanilla's
  toggle.

## Shovel turns paths back to dirt (Dylan, 2026-09-27)
Right-clicking a dirt path with a shovel turns it back into dirt, the reverse of making a path: same flatten sound, 1 durability, not
from the bottom face, and only with air above (all like vanilla's path-making). All shovels, since it's vanilla's own shovel action.
- No code: shovel right-click is data in 26.x. Shovels carry the `minecraft:block_transformer` item component pointing at the
  `minecraft:shovel` entry of the `block_transformer` data registry. The mod ships its own `data/minecraft/block_transformer/shovel.json`
  (overrides vanilla's): vanilla's rule copied as-is plus a second rule, `minecraft:dirt_path` + air above -> `minecraft:dirt`.
  If vanilla changes its `shovel.json` in an update, copy the change into ours (it replaces vanilla's whole file).

## Hoe turns farmland back to dirt (Dylan, 2026-09-30)
Right-clicking farmland with a hoe turns it back into dirt, the reverse of tilling: same till sound, 1 durability, not from the bottom
face, and only with air above, so farmland with a crop on it can't be un-tilled. Any moisture. All hoes, since it's vanilla's own hoe
action.
- No code, same as the shovel: the mod ships its own `data/minecraft/block_transformer/hoe.json` (overrides vanilla's): vanilla's
  file copied as-is plus a third rule in the first transform, `minecraft:farmland` + air above -> `minecraft:dirt`.
  If vanilla changes its `hoe.json` in an update, copy the change into ours (it replaces vanilla's whole file).
