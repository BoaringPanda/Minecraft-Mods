# BetterVanillaBuilding

Fabric mod (display name "BP's Better Vanilla Building", Dylan's pick 2026-09-28; mod id `bettervanillabuilding`, which never
changes since saved blocks use it; package `com.boaringpanda.bettervanillabuilding`; icon `assets/bettervanillabuilding/icon.png`,
Dylan's own art, scaled to 512×512) that extends vanilla building in
vanilla's own style: no new textures where vanilla's can be reused, and features behave the way the vanilla thing they extend does.

## Stack
- Minecraft 26.3 (date-based versions, **Mojang mappings**: `net.minecraft.resources.Identifier`, `net.minecraft.client.Minecraft`),
  Fabric Loader 0.19.5, Fabric API 0.160.7+26.3, Loom 1.18.2, Gradle wrapper 9.7.1, Java 25. Versions live in `gradle.properties`.
- Split source sets: `src/main` (common + server) and `src/client` (client only: renderers).
- Game source for reading: `./gradlew genSources`, then the `*-sources.jar` files under `.gradle/loom-cache/minecraftMaven/net/minecraft/`.

## Commands
Run from this folder (where `gradlew` is), not the repo root. `JAVA_HOME` must point at the JDK 25 install
(`C:\Program Files\Java\jdk-25.0.2`).
- `./gradlew build` builds the mod and copies the jar into `jars/` (git-ignored, jars never go to GitHub).
- `./gradlew runClient` starts a dev game with the mod.

## Fence ropes (a lead tied between two fences, no animal)
Spec from Dylan (2026-09-24): tie a lead from fence to fence with no animal, using only vanilla textures. The rope attaches one pixel
below the top of the knot at both ends and hangs a little, more on longer ropes, with no warping or stretching. It reaches **at most 7
blocks counting both fences** (Dylan, 2026-09-25, was 5: "5 distance isn't long enough"; fences at most 6 apart, measured flat between
block centres: (6,0) and (4,4) are fine, (5,4) is not) and
goes **at most 3 blocks up or down** (Dylan, 2026-09-25, was 1; `MAX_HEIGHT_DIFFERENCE`). Tying works like leading an animal: a lead on fence 1 starts a rope that follows your hand, and a
click on fence 2 ties it.

- `entity/RopeKnotEntity` extends vanilla's `LeashFenceKnotEntity` and implements `Leashable`: a knot on fence 1 whose own rope is
  held by the knot on fence 2 (or by the player while being carried). So vanilla syncs, saves, restores (`Leashable.tickLeash`), cuts
  (shears), hands back (empty-hand click on the knot), snaps (walking too far) and drops the lead. Overrides: `ROPE_HEIGHT` 7/16 as its
  leash offset, `checkElasticInteractions` returns false (a knot must not be pulled or spun), and lifecycle rules. It is discarded only
  when it has neither a rope of its own nor anything tied to it, and a destroyed knot (`reason.shouldDestroy()`) drops its rope as a lead.
- `entity/RopeKnots` registers `bettervanillabuilding:rope_knot`, sized and tracked like vanilla's knot but **saved** (vanilla's knot
  is `noSave`; this one carries the rope).
- `block/FenceRopeInteraction` uses `UseBlockCallback` and `UseEntityCallback`. A click on a knot counts as a click on its fence,
  because clicks near the top of a post often hit the knot's hitbox. Server only; the client passes so vanilla sends the packet.
  - Start: lead in hand, nothing on a lead to the player, not sneaking. Spawns a rope knot leashed to the player, uses one lead
    (not in creative) and shows "Rope fence started". With a lead, clicking a knot starts a rope rather than vanilla's pick-up; an
    empty hand still picks up.
  - Carrying a rope: it stays in hand until tied (Dylan, 2026-09-24). Clicking the fence it started from ties nothing, and it only lets
    go the vanilla way, snapping and dropping the lead at 12 blocks. There used to be a "click the start fence to take it back"
    rule, but holding right-click repeats the click every 4 ticks, and the repeat hit the new knot and undid the rope within 0.2 s. For
    the same reason a lead click can't *start* a rope within `REPEAT_TICKS` (10) of the player starting or tying one
    (`LAST_ROPE_CLICK`, a `WeakHashMap<Player, Long>`); otherwise holding the button while tying at B started a new rope from B.
  - The rules live in `tieProblem(level, from, to)`: height, then distance (`isBeyondReach`), then `alreadyTied`. Out of reach is
    "Rope limit reached (7 block limit)". **The knot enforces them**: `RopeKnotEntity.canHaveALeashAttachedTo` only accepts the
    player, or a knot on a *different* fence with no `tieProblem`. Vanilla asks this before every tie (`LeadItem.bindPlayerMobs`, the
    knot's `interact`, sneak-clicking a mob in `Entity.interact`), so vanilla itself refuses bad ties.
  - Clicks on a fence block while carrying (`tieAtFence`) are **never cancelled** (Dylan's bug, 2026-09-25: a block placed on the
    start fence vanished, because the old code returned `FAIL` there and the client had already predicted the placement). The server
    shows the problem message, if any, and passes; vanilla ties nothing and the held block places. Sneaking passes straight through
    (vanilla skips the fence when sneaking with an item). On the client, a click that *will* tie returns `SUCCESS` (Fabric still sends
    it), so the client doesn't draw a block the server won't place.
  - Clicks on a knot while carrying (`tieAtKnot`, server) still return `FAIL` on the start knot or a problem, since a knot click never
    places a block and vanilla's knot, having tied nothing, would hand the player the ropes tied to it. If all pass it returns `PASS`
    and vanilla ties it.
  - Walking with a rope (Dylan's request, 2026-09-24): `RopeKnotEntity.warnIfCarriedBeyondReach` shows the same limit message while
    the player's block is out of reach of the start fence (`isBeyondReach`, same rule as clicking). It shows on crossing the limit, then
    every 40 ticks so it stays up. The rope stays in hand and still snaps only at vanilla's 12 blocks.
- `client/entity/RopeKnotRenderer` draws vanilla's knot model/texture. When the holder is a fence knot it draws the rope itself and
  clears `state.leashStates`, because vanilla draws a leash as a straight line with no sag (it only curves when one end is higher) and
  ends it 0.2 above a knot. The rope is inset 3/16 from each knot centre and sits at `ROPE_HEIGHT` at both ends, with a parabolic sag
  of `SAG_PER_BLOCK` (0.06) × the *flat* span (**the knob for "how much hang"**; flat span, not full length, so a steep rope hangs
  less and one between fences stacked in a column hangs straight). `evenlySpaced` measures the curve in 64 straight pieces and places
  the steps every 0.1 blocks *along the rope* (equal steps of the curve's 0-1 are longer where a sloped rope's sag steepens it, which
  stretched the stripes at one end of a 3-up rope). The ribbon cross-section stays perpendicular to the rope's direction, so sloped
  ropes don't warp. While a player carries the rope, vanilla draws it.
- **Rope collision** (Dylan, 2026-09-24): `entity/RopeCollision` plus `mixin/EntityMixin`. A tied rope is an invisible wall of one of
  two heights (`wallHeight`):
  - Unridden mobs in the `penned_by_ropes` entity tag (`RopeCollision.PENNED`, `data/bettervanillabuilding/tags/entity_type/`) get
    1.5 (fence collision height), which they can't jump, so ropes make pens. Dylan's list (2026-09-26): chicken, cow, pig, sheep,
    donkey, horse, mule, goat, llama, panda, mooshroom, sniffer, trader llama. It used to be every `Animal`. Camels were on it but
    step straight over (vanilla step height 1.5 = the wall), so Dylan dropped them to the low wall rather than raise it.
  - Players, every other `Mob` (other animals, hostile, villagers, golems) and player-ridden penned mobs get 14/16 (the rope's height at the knots plus
    its thickness). That's too high to step (0.6) but jumpable. Dylan's spec: solid for players and mobs, "but we can just jump over it".
  - Items, boats, arrows etc. get nothing.
  - The mixin `@WrapOperation`s the `Level.getEntityCollisions` call in `Entity.collide`, so the walls only affect what a moving
    entity bumps into (step-up included). The local player moves client-side, and the client knows both knots, so it collides there
    too; the server's movement check uses the same walls.
  - Wall shape: along the flat line between the fence centres, 0.25 wide (a fence arm), made of small boxes every 0.125 so diagonals
    have no gaps. Each box stands on the line between the two fences' bases (interpolated fence base, up to height), so a sloped rope
    is equally jumpable along its length, and the high end of a rope strung off a pillar can be walked under (Dylan's pick,
    2026-09-25, when ropes went to 3 up; it used to reach down to the lower fence's base). On stepped ground small animals may slip
    under a steep rope where the line is above the ground. The knot search box reaches `MAX_HEIGHT_DIFFERENCE` + 1.5 up and down.
  - Pathfinding only reads blocks, so mobs don't see ropes. `hopIfBlockedByRope` (the `@Inject` at `Entity.move` TAIL) makes a
    non-penned mob on the ground with `horizontalCollision` against a rope jump (`JumpControl.jump()`). Penned mobs just walk up to the
    rope and stop, as at a fence.
- This was first built in the deleted old mod (commit `7b94e6b`). The render fixes above came from Dylan's reports there.
- Known edges: a rope knot can sit on the same fence as a vanilla knot (both draw, overlapping). Vanilla's `getKnot` can return a rope
  knot for an animal tied at that fence, which is harmless because of the lifecycle rules. A carried rope can't be sneak-clicked onto
  a mob or boat (vanilla's leash-to-entity), because `canHaveALeashAttachedTo` only takes fence knots and players. Clicking fence B
  with a block while carrying ties the rope rather than placing the block (as vanilla does for a led animal); sneak to place.
  Multiplayer is untested.

## Placed rods (sticks, blaze rods, breeze rods placed like end rods)
Spec from Dylan (2026-09-24): place sticks, blaze rods and breeze rods like end rods, each looking like a plain rod (an end rod minus its
base), using existing effects. A stick catches fire and burns away near fire, and snaps when walked on. A blaze rod has a fire effect
(small flames along the rod plus the odd smoke, his pick), light 7 (half a torch), and burns you like a campfire. A breeze rod gives
Slowness I while stood on, wearing off about 1 s after stepping off (his pick).

- `block/PlacedRods` registers `stick`, `blaze_rod` and `breeze_rod` as blocks with **no items**, using the End Rod's properties. The
  stick sounds like wood (`SoundType.WOOD`), and the blaze and breeze rods sound like chains in every way (`SoundType.CHAIN`, Dylan). The
  stick is made flammable like short grass (`FlammableBlockRegistry` add(100 burn, 60 spread) = vanilla grass ignite 60 / burn 100).
  `place()` mirrors vanilla `BlockItem.place`: checks, `setBlock` flag 11, `setPlacedBy`, `PLACED_BLOCK` trigger, sound, game event,
  `consume(1)`.
- `mixin/ItemMixin` sits at the head of `Item.useOn`, and only for those three items (they are plain `Item`s whose `useOn` did
  nothing). `useOn` runs after the clicked block's own `useItemOn`/`useWithoutItem`, like a block item, so chests, doors and sneak-to-place
  behave as vanilla. It isn't a `UseBlockCallback`, which would run *before* the block and break that. There's no real `BlockItem`,
  because an unregistered `Item` breaks the registry freeze (intrusive holder).
- `block/PlacedRodBlock` extends vanilla `RodBlock` (FACING, 4-px hitbox, rotate/mirror), copies `EndRodBlock.getStateForPlacement`
  (out from the clicked face, flipped against the same rod facing the same way), and returns its item from `getCloneItemStack`.
  26.3 blocks have no `codec()`.
  - `StickBlock`: snaps once something has stood on it for 30 ticks **in a row** (Dylan, 2026-09-25: "you need to actually be on the
    block for it to break, so if you stand on it for half a second it doesn't break"; it used to snap 1.5 s after the *first* step, even
    after walking off, via a scheduled tick). Vanilla calls `stepOn` every tick while a `LivingEntity` is on the ground on it (from
    `LivingEntity.aiStep` → `applyEffectsFromBlocks`, standing still included), so `stepOn` keeps a server-side record per level
    (`WeakHashMap<Level, Long2ObjectMap<long[]>>`, pos → first and last tick of the current stand). More than `GAP_TICKS` (2) off it
    (stepping off, a jump) starts a new stand; stale records are dropped whenever a new one starts. Not saved. Sneaking doesn't help.
    Snapping is `destroyBlock(pos, false)` (no drop), whose break sound is the block's own `SoundType.WOOD`, the same as a log breaking
    (Dylan asked for a log-break sound in place of the first version's tool-break snap). An old save's pending tick from the first
    version is ignored (no `tick` override any more). Breaking by hand still drops the stick.
  - `BlazeRodBlock`: `animateTick` flames and smoke along the rod's axis. `stepOn` does `hurt(damageSources().campfire(), 1)` to a
    `LivingEntity`, exactly vanilla `CampfireBlock.entityInside` (no sneak exemption, as with a campfire; Dylan asked for campfire damage).
  - `BreezeRodBlock`: `stepOn` Slowness I (20 ticks, refreshed each tick), plus a slight powder-snow chill (Dylan: "slightly shows
    the freezing effect"). It adds 3 to `ticksFrozen` a tick, capped at 42 of the 140 needed to freeze (30%, a faint frost overlay; freeze
    damage only starts at 100%). Vanilla thaws 2 a tick outside powder snow, so the net +1 a tick matches powder snow's build-up. Stepping
    off thaws it the vanilla way. It respects `canFreeze()` (leather armour) and never lowers a colder value from real powder snow.
- Textures: `textures/block/<rod>_{1,2,3}.png`, drawn **only from each vanilla item sprite's own pixel colours**. Dylan said the
  borrowed entity textures (armor stand, blaze and breeze rods) "look weird", then asked for variation and for rods placed end to end
  to join without a seam.
  - Layout: x 0–7 is the four faces' side strips (face n at x 2n..2n+1: a lit column from the item's core colours, then a shade column
    from its mid/edge colours), each 16 tall. Top cap 2×2 at x 8–9, bottom cap at x 10–11 (the item's tip colours).
  - Colours come in short runs (1–3 px) along the rod, like grain, and no row is special, so the last row sits naturally against the
    first row of the next rod.
  - Three variants per rod, chosen at random per block by the blockstate (a model list per facing, like vanilla's random variants).
  - Keep each shade column close in tone to its lit column. A dark shade column (the breeze rod's first version) draws a hard line down
    every corner of the rod, where one face's shade column meets the next face's lit column. Dylan saw it as a seam. Dark colours
    (Dylan asked for more of the items' darker oranges and blues) go in as flecks in *both* columns, never as a column of their own.
  - Made with a throwaway seeded Java/ImageIO script, not kept in the repo; to change them, edit the PNGs.

  Models: one template, `block/placed_rod` (element `[7,0,7]`–`[9,16,9]`, north/east/south/west uv x 0–2/2–4/4–6/6–8, up
  `[8,0,10,2]`, down `[10,0,12,2]`), plus a child per variant that only sets `#rod`. Blockstates are `multipart`: one part per
  facing with vanilla `end_rod.json`'s rotations (3 random variants each), plus the arm parts below. Loot tables drop the vanilla item.
- **Arms** (Dylan, 2026-09-25, from a picture): an upright rod (facing up or down) reaches out to a rod **of the same kind** (his pick)
  lying beside it and pointing at it (its `FACING` axis = that side's axis, either way round). The arm goes from the rod's middle to the
  edge of its block at half height, where the lying rod's end is. The upright rod is the one that reaches out.
  - `PlacedRodBlock` has the fence's `north/east/south/west` booleans (`ARMS` = `CrossCollisionBlock.PROPERTY_BY_DIRECTION`), always
    false unless upright. `getStateForPlacement` and the stick's facing change use `withArms` (all four from the neighbours).
    `updateShape` works out **only the side that changed**, as a fence does. An arm the stick removed would grow back when a matching
    rod is placed on that side (and one it added would go), so a stick-changed rod is locked (Builder Stick → Locked blocks).
  - Hitbox: `getShapeForEachState` of `RodBlock`'s upright column plus a 4-px arm box per arm (`Shapes.rotateHorizontal`, the
    `CrossCollisionBlock` pattern), collision too. `rotate`/`mirror` move the arms with the rod (structures).
  - Models: template `block/placed_rod_arm` (`[7,7,0]`–`[9,9,7]`, toward north; the side-strip UVs, the east/west faces UV-rotated 90 so
    the grain runs along the arm; the outer end has the top cap's uv `[8,0,10,2]` so a stub is closed off, and there's no inner end face) and children `<rod>_arm_{1,2,3}`. Multipart `north=true` etc. with y 0/90/180/270.
  - Builder Stick: on an upright rod the options are facing, north, east, south, west, each a plain on/off toggle **toward anything**
    (Dylan, 2026-09-26: rods don't connect to blocks when placed, "but can you at least allow the player to connect the n, e, s, w to a
    block with the builder stick"). It replaced his first rule, on only toward air or the same kind of rod. The arm runs to the block's
    edge, so it meets a neighbour's face; toward air it's a closed-off stub. Stick changes on rods are quiet (`FLAGS`, never neighbours): they were briefly in `updatesLikePlacing`, and
    Dylan saw that as a bug, because changing one rod re-worked the arm of the upright rod next to it. No lock (his pick): placing or
    breaking next to a rod still connects and disconnects as normal. Known edge: turning a lying rod with the stick leaves the upright
    rod's arm as it was, until something is placed or broken on that side.

## Hanging ladders
Spec from Dylan (2026-09-24): only the top ladder of a column needs a block behind it. If that one loses its support, every ladder
hanging from it breaks and drops. Blocks behind lower ladders are still allowed.

- `mixin/LadderBlockMixin` changes vanilla's own two checks, so placement, survival and drops all go through vanilla:
  - `canSurvive` (`@ModifyReturnValue`): a ladder also survives if the ladder directly above it (same `FACING`, any `LadderBlock`)
    survives. It walks up the column (a loop, not recursion) until it finds one with a sturdy block behind it (the same
    `isFaceSturdy` test vanilla's private `canAttachTo` uses) or the column ends. Placement uses this too (`getStateForPlacement` tries
    `canSurvive`), so you can place a ladder under a hanging one but not on top of an unsupported one.
  - `updateShape` (`@Inject` HEAD): vanilla only re-checks when the block *behind* changes. This also re-checks when the block *above*
    changes and returns air if the ladder can't survive. Vanilla's `updateOrDestroy` turns that into `destroyBlock` with drops, and the
    removal updates the ladder below, so the whole column falls one after another.
- A consequence of "hangs from the one above": breaking a middle ladder by hand drops every ladder below it that has no block of its
  own behind it. A ladder lower down that does have a block behind it holds up everything below it.

## Mixed slabs (any two slabs stacked into one block)
Spec from Dylan (2026-09-24): any slab stacks on any other slab. **Every** slab counts (every `SlabBlock`, vanilla or modded). Walking
on it sounds like the **top** slab; mining and breaking sound like the half **under the cursor**. The **tougher** slab decides mining
speed and the right tool (stone + oak mines like stone, either way up), and each slab keeps its own hardness. **Drops:** each slab
follows its own rule (his pick): oak + stone broken by hand drops only the oak slab. **Pistons** push and pull it (his pick).

- **One block, not one per pair.** The deleted old mod registered 13,806 blocks (one per pair) plus generated models, the "tens of
  thousands of files" that made that project unmanageable. Now `block/MixedSlabs` registers one block, `bettervanillabuilding:mixed_slab`
  (`MixedSlabBlock`, no item), and a block entity (`MixedSlabBlockEntity`) that saves and syncs `MixedSlabs.Halves(bottom, top)`
  (codec `{"bottom": id, "top": id, "bottom_rainbow": bool, "top_rainbow": bool}`, the flags optional/false so older saves load; see
  rainbow things). A slab from a removed mod fails the codec and the block falls back to smooth stone
  (`Halves.FALLBACK`), which is also what a `/setblock`'d one shows.
- **Placing:** `mixin/SlabBlockMixin` widens vanilla's two checks: `canBeReplaced`'s "is the item this slab" (compiled as
  `ItemStack.is(Object)`, the generic `TypedInstance.is(T)`) also accepts any slab item, and `getStateForPlacement` returns the mixed
  block when the clicked half slab is a different slab. Same slab + same slab is still vanilla's double slab. `mixin/BlockItemMixin`
  wraps `placeBlock` to read the old slab first and fill the new block entity (ordered by the old slab's `TYPE`), and swaps the place
  sound to the placed slab's. It runs on the client too, so the client's prediction draws both slabs at once.
- **Toughness** (`Halves.toughest`): needs a tool > harder (`defaultDestroyTime`) > more blast resistant; a full tie goes to the bottom.
  `MixedSlabBlock.getDestroyProgress` returns the toughest slab's own `getDestroyProgress`, so tool tags, the needs-a-tool slow-down,
  efficiency and haste are vanilla's. `ExplosionDamageCalculatorMixin` gives it that slab's blast resistance.
- **Drops:** the block doesn't need a tool, so vanilla always calls `playerDestroy`, which drops each half (its own loot table, same
  tool, so silk touch works) only if that half's tool rule passes. `getDrops` (explosions, pistons crushing it) returns both halves' loot.
- **Sounds and particles:**
  - Top slab: `EntityMixin` changes the state at the start of `walkingStepSound` (before every mob's own `playStepSound` and the
    player's underwater/carpet variants) and in `spawnSprintParticle`; `LivingEntityMixin` does the landing particles in
    `checkFallDamage` and the fall sound in `playBlockFallSound`. `BlockStateBaseMixin`: map colour is the top slab's.
  - Targeted half (`MixedSlabs.targeted`: the hit's y is in the upper half = top slab): `client/mixin/ClientLevelMixin` in
    `addBreakingBlockEffects` (each mining hit's sound and crack particles, from `Minecraft.hitResult`); `MixedSlabBlock.
    spawnDestroyByEntityParticles` for the break (level event 2001, sent as that slab's DOUBLE state so particles fill the block; it
    raycasts with `player.pick`, so client and server agree). Middle-click: `ServerGamePacketListenerImplMixin` picks the targeted slab
    and skips ctrl+pick's block-data copy (it would overwrite the slabs of the next mixed block that item made).
- **Model:** `client/model/MixedSlabModel`, registered in code with `ModelLoadingPlugin.registerBlockStateResolver` (so no blockstate
  JSON). `emitQuads` reads the pair from `getBlockEntityRenderData` and calls the vanilla bottom-slab and top-slab models' own `emitQuads`
  with the mixed block's `cullTest`, dropping the two uncull-tagged faces where the slabs meet. Textures, tints, AO and culling are
  vanilla's. `MixedSlabBlockEntity.loadAdditional` on the client calls `sendBlockUpdated` to redraw when data arrives.
- **Pistons:** vanilla won't move a block entity and a moving block only keeps its state. `PistonBaseBlockMixin` makes a mixed block
  pushable (`isPushable`'s `hasBlockEntity`), reads every pushed mixed block's halves right after `getToPush` (`@Share`d map by start
  position) and gives them to the new `PistonMovingBlockEntity` (it implements `MixedSlabCarrier` via `PistonMovingBlockEntityMixin`,
  saved as `bettervanillabuilding:mixed_slab`). After the moving block's `setBlock` in `tick` / `setBlockAndUpdate` in `finalTick`, the
  halves go into the new block entity. `moveBlocks` also runs on the client, so it carries them too. While moving it's drawn from a
  `MovingBlockRenderState`: `client/mixin/PistonHeadRendererMixin` hands it the halves and `MovingBlockRenderStateMixin` answers
  `getBlockEntityRenderData` with them. A mixed block with an immovable half (the obsidian slab, see "More stairs and slabs") isn't
  pushable (`isPushable` return hook), as that slab alone isn't.
- **Known edges (not done; can be added):** copper halves don't oxidise and an axe won't wax or scrape them; wooden halves don't burn
  (flammability is per block, no position); non-player breaks that call level event 2001 with the mixed state itself (e.g. a piston
  crushing it) show smooth-stone particles and a stone sound; see-through modded slabs are untested (the block is solid and opaque,
  right for every vanilla slab).

## Builder Stick (a survival debug stick for a few blocks and options)
Spec from Dylan (2026-09-24): like the debug stick minus the cheaty parts. Crafted from an amethyst shard above a stick (a 1×2 shaped
recipe, so it fits the 2×2 and 3×3 grids). It works **only** on blocks Dylan names and changes **only** the options he names.
**Don't add blocks or options he hasn't named.** So far:

| Block | Options (left click cycles) | Right click |
|---|---|---|
| fence, glass pane, stained glass pane, iron bars, copper bars | north, east, south, west | on / off |
| wall (every `WallBlock`, this mod's too) | north, east, south, west | on / off |
| fence gate | facing, height (`in_wall`, "slightly up or down") | rotate / up, down |
| door (every `DoorBlock`: wood, iron, copper) | facing, hinge ("flipped") | rotate / left, right |
| trapdoor (every `TrapDoorBlock`) | facing | rotate |
| iron trapdoor (only `Blocks.IRON_TRAPDOOR`; copper ones open by hand) | facing, state | rotate / open, closed |
| stairs (every `StairBlock`, this mod's too) | facing, shape, half ("flipped") | rotate / 5 shapes / top, bottom |
| slab (every `SlabBlock`, this mod's too, not while double; wool slabs see below) | half | top, bottom (never makes a double slab; a double slab is "not allowed") |
| chain, copper chains (`ChainBlock`) | axis | x, y, z |
| every log, wood, stripped log, stripped wood (`#logs`, so nether stems and hyphae too), hay bale, quartz pillar, purpur pillar, polished basalt, deepslate, ancient debris, reinforced deepslate (`isPillar`) | axis | x, y, z |
| stonecutter, grindstone (floats, so any way, on a wall too) | facing | rotate |
| bell on the floor or ceiling | facing | rotate |
| bell on a wall | facing | rotate, onto a side with a wall only; hangs between two walls when the opposite side is solid too |
| end rod, lightning rods, placed stick/blaze rod/breeze rod (`RodBlock`) | facing | all 6 directions |
| upright placed stick/blaze rod/breeze rod | facing, north, east, south, west (its arms) | all 6 directions / on, off (toward anything: a block, air or a rod) |
| piston, sticky piston (`PistonBaseBlock`, retracted only), dispenser, dropper (`DispenserBlock`), observer | facing | all 6 directions |
| hopper | shape, facing | down, side (points the way you look) / turn the side spout (nothing while it points down) |
| comparator, repeater (`DiodeBlock`) | facing | rotate |
| rail (`RailBlock`) | shape, rotation | all 10 rail shapes / vanilla's own rail rotation (`state.rotate`) |
| powered, activator, detector rail | rotation | vanilla's own rail rotation |
| every button (`ButtonBlock`) | facing | rotate, onto supported sides only |
| standing sign, ceiling hanging sign | facing | all 16 directions (a quarter at a time on an unattached ceiling hanging sign) |
| wall sign, wall hanging sign | facing | rotate, onto supported sides only |
| every copper golem statue (`CopperGolemStatueBlock`) | facing, pose | rotate / standing, sitting, running, star |
| standing banner (`BannerBlock`) | facing | all 16 directions |
| wall banner (`WallBannerBlock`) | facing | rotate, onto supported sides only |
| every standing head, player included (`SkullBlock`) | facing | all 16 directions (on a stack of heads, the head under the cursor) |
| every wall head (`WallSkullBlock`) | facing | rotate, only onto a side with a block behind |
| armour stand (an **entity**, not markers) | facing | turns 45° (vanilla's 8 placement directions) |
| item frame, glow item frame (**entities**, `ItemFrame`) | frame | shown / hidden (Dylan, 2026-09-25: vanilla's own `Invisible` flag, which vanilla saves, syncs and draws: the item alone, flat to the wall; an empty hidden frame shows nothing but can still be aimed at) |
| rainbow things: wool, wool stairs, wool slabs, carpets (not moss), stained glass, stained glass panes, beds, banners (standing + wall), cushions (an **entity**) | rainbow, added after the block's own options (stairs: facing, shape, half, rainbow; slabs: half, rainbow; panes: north…west, rainbow; banners: facing, rainbow; everything else, double wool slabs included: rainbow only; a wool slab half of a mixed slab: rainbow only, the half under the cursor, the other half "not allowed") | on / off (fades through every colour like a sheep named jeb_, but slower: 3.5 s a colour) |

Rainbow things (Dylan, 2026-09-25): "on wool blocks, they change colours like sheep when you name them jeb_"; then, after trying it,
"slow down the colour changing" (2x slower, then "tiny bit slower", then "3.5": 70 ticks) and "let the wool stairs and slabs have that
option as well" (both new in 26.3); then "add beds, stained glass, stained glass panes, cushions, carpets, and banners".
- **Blocks:** `block/Rainbow`: a `rainbow` boolean that `BlockMixin` adds (off by default via `offByDefault`, like `locked`) to every
  vanilla block whose id is `<dye colour>_` + one of `wool`, `wool_stairs`, `wool_slab`, `carpet`, `stained_glass`,
  `stained_glass_pane`, `bed`, `banner`, `wall_banner` (so moss carpets and straw beds are out). Picked **by id**, since wool is a plain
  `Block` and the rest are ordinary vanilla classes: `mixin/BlockPropertiesAccessor` reads the private `BlockBehaviour.Properties.id` (set
  by `Blocks.register` before the constructor runs), taken in `addProperties` with `@Local(argsOnly = true)`. Placement, worldgen and old
  saves stay vanilla; loot is vanilla's, so each drops its own colour. (The very first version swapped wool to a `RainbowWoolBlock` class
  in `BlocksMixin`; that couldn't reach stairs/slabs, so it was removed.) `Rainbow` is loaded while vanilla builds its blocks, so it holds
  nothing else (no registrations).
- **Cushions** are entities in 26.3 (`Cushion`, placed on a block and sat on), so `entity/RainbowCushions` keeps a Fabric data attachment
  (`bettervanillabuilding:rainbow`, `Codec.BOOL`, synced to all clients, removed when off). The stick's `onAttackEntity`/`onUseEntity`
  handle them like armour stands (`isStickOnEntity`): left click selects `rainbow`, right click toggles it; the stick's right click runs
  before `Cushion.interact`, so it never sits you down. Breaking one drops its own colour.
- **Stick:** `optionsFor` = `shapeOptionsFor` (the per-block lists) + the shared `RAINBOW` option last when the block has the property.
  Quiet first-batch flags, except banners, which already update like placing. A rainbow toggle never locks a stained glass pane
  (`option != RAINBOW`). A bed's other half gets the same value (`AbstractBedBlock.getConnectedDirection`), like a door's.
- **Looks: only animated textures, everything in step.** Each rainbow texture is 16 frames (vanilla's texture of each colour, top to bottom
  in `DyeColor` order, the order `ColorLerper.Type.SHEEP` steps through) with an `.mcmeta` of `frametime` **70** + `interpolate` (jeb_ is
  25 ticks a colour; Dylan wanted it slower: 3.5 s a colour, 56 s a loop). All animated textures count the same client ticks, so every
  rainbow block, banner and cushion fades together (jeb_ sheep are offset by entity id). Textures, `block/`: `rainbow_wool`,
  `rainbow_stained_glass`, `rainbow_stained_glass_pane_top`, 7 `rainbow_bed_*` (beds are block models in 26.3, 7 textures a colour),
  `rainbow_cushion` (64×64 frames); `entity/banner/rainbow_base`.
  - Blocks: `assets/minecraft/blockstates/` overrides all 16 of each kind except banners: vanilla's cases with `rainbow=false`, then again
    with `rainbow=true` pointing at `bettervanillabuilding:block/rainbow_<rest of vanilla's model name>` (same rotations, uvlock; pane
    multiparts get `"rainbow"` in every `when`). A double wool slab uses `rainbow_wool`. Rainbow models use vanilla's parents; glass ones
    keep vanilla's `force_translucent`. Banner blockstates need no override (`""` matches every state; the look is the renderer's).
  - Banners (drawn by `BannerRenderer`, the base colour being vanilla's white `entity/banner/base` tinted with the dye's
    `getTextureDiffuseColor`): `rainbow_base` is that base pre-tinted with each colour, in the banner pattern atlas (its `entity/banner`
    folder takes every namespace). `client/mixin/BannerRendererMixin` puts the flag in the render state (`RainbowRenderState`, added by
    `RainbowRenderStateMixin`), and while `submit` draws a rainbow banner, `submitPatternLayer`'s base layer (`Sheets.BANNER_PATTERN_BASE`)
    is drawn untinted from `rainbow_base` instead. Patterns stay their own colours on top. Items (`submitSpecial`) aren't changed.
  - Cushions (drawn by `CushionRenderer` from plain, non-atlas textures, which can't animate): `rainbow_cushion` is in the **block** atlas,
    and `client/mixin/CushionRendererMixin` swaps the `submitModel` call for the sprite version (`Sheets.BLOCKS_MAPPER`, the block atlas).
  - The texture strips and the 64 stair/slab/carpet/glass/pane/bed blockstates were generated from the game jar's own files by throwaway
    Java scripts (not kept); the banner strip multiplies `base.png` by each `DyeColor` diffuse colour.
- **Mixed slabs** (Dylan, 2026-09-29: "use the builders stick on a wool slab to make it rainbow if that wool slab is mixed with another
  slab"): `MixedSlabs.Halves` keeps a rainbow flag per half, and `bottomState()`/`topState()` set `rainbow` on a half that has it, so the
  model, particles and sounds need nothing new (the wool slab blockstate overrides draw it). The stick on a mixed block works on
  `MixedSlabs.targeted` (the half under the cursor) with `mixedHalfOptions`: only `RAINBOW`, only on a wool half; a right click writes
  it back with `Halves.withRainbow` + `setHalves` (which syncs). `BlockItemMixin` keeps the fade of a rainbow wool slab something is
  stacked onto; the slab placed is always plain. Pistons carry the flags with the rest of `Halves`.
- Edges: a same-colour wool slab put into a rainbow half makes a rainbow double slab (vanilla's `SlabBlock.getStateForPlacement` keeps
  the existing state). Picking up / breaking anything rainbow gives the plain item.

Fourth batch (Dylan, 2026-09-24): logs/wood (all four kinds), hay, quartz and purpur pillars, polished basalt, deepslate ("directions" =
their axis), stonecutter, bell and grindstone rotation. Ancient debris ("netherite debris") and reinforced deepslate are plain `Block`s
in 26.3 with no direction property, so `mixin/BlocksMixin` swaps the factory in `Blocks.register(ResourceKey, Function, Properties)` (the
overload every other one ends in) for those two ids to `block/UprightPillarBlock`: a `RotatedPillarBlock` whose `getStateForPlacement` is
always the upright default, so hand placement, worldgen and old saves stay exactly as vanilla and only the stick lays them sideways.
Vanilla's properties (hardness, sound, loot) are kept. Their blockstates are overridden in `assets/minecraft/blockstates/` with log-style
axis variants over vanilla's own models (no new textures). Only `DEEPSLATE` itself turns, not cobbled/polished/etc. deepslate (which have no axis).
Bell and grindstone `ATTACHMENT`/`FACE` (floor/wall/ceiling) aren't changed, only facing.

Third batch (Dylan, 2026-09-24): banners, heads and armour stands "can only be rotated", so no patterns, head power or stand poses/arms.

Second batch (Dylan, 2026-09-24): "axis" for pistons, droppers, dispensers and observers meant their facing. An extended piston is "not
allowed" (turning it would leave its head behind). The crafter isn't a `DispenserBlock`, so it isn't included. The statue "stance" he
half-remembered is vanilla's `POSE` (right-clicking a statue by hand cycles it too). "glass" / "stained glass" meant the panes (full
glass blocks have no sides). Not asked for, so not there: door or gate open/close,
trapdoor top/bottom, the wall `up` post. `RodBlock` in 26.3 is exactly end rod, lightning rods (+ weathering) and our `PlacedRodBlock`;
`ChainBlock` is iron + copper chains. The mod's mixed slab isn't a `SlabBlock`, so it's "not allowed".

- `item/BuilderStick` registers the item (stacks to 1, in Tools & Utilities after the brush), the `builder_stick_option` data component
  (a `String`: the selected option's name, shared by every block, where the debug stick remembers one property per block type), and
  the `UseBlockCallback`, `AttackEntityCallback` and `UseEntityCallback`.
- `item/BuilderStickItem` copies `DebugStickItem`. Each block kind has a prebuilt `List<Option>` (`optionsFor`, empty = not allowed).
  An `Option` is a name, a value shown in messages, and a `Change` (level, pos, state, player → new state). If the stored name isn't in
  the clicked block's list, a left click selects its first option and a right click changes its first option (the debug stick's
  "nothing selected" rule). Action-bar messages use the debug stick's wording (`selected "hinge" (left)`, `"facing" to east`). No op
  check, no glint.
  - **It never breaks a block** (Dylan, 2026-09-24): `canDestroyBlock` (left click) is always false. It selects the next option
    (sneak: previous), or on an unsupported block shows "Can not be used on this block" (`.not_allowed`).
  - `getDestroySpeed` is `Float.MAX_VALUE` on every block, so any left click "breaks" (acts) at once in survival instead of after
    seconds of mining. Instant breaks repeat every tick while held, so `REPEAT_TICKS` (5) gates left clicks per player
    (`LAST_LEFT_CLICK`), matching the creative debug stick's hold-to-repeat pace. Hardness -1 blocks (bedrock, barrier) never finish
    "mining" in survival, so they show no message there (still unbreakable).
  - **Every right click goes through `onUseBlock`** (`UseBlockCallback`, which runs *before* the block's own action). Otherwise
    wooden doors, gates and trapdoors would open before the stick got a turn, and on unsupported blocks doors, chests and buttons
    would act (Dylan chose that the stick overrides them, even when sneaking). The rules:
    - Only for the stick in that hand. It passes for spectators and when `!mayBuild()`, because the callback skips vanilla's
      adventure check.
    - Unsupported block: the client shows the message and returns FAIL (no packet, no door-prediction flicker), and the server
      returns FAIL too.
    - Supported block: the client returns SUCCESS, which Fabric's client hook sends on to the server (`consumesAction` →
      `startPrediction`). The server applies the change (see the two update modes below).
    - Horizontal facing turns clockwise (sneak: counter-clockwise). Gates, doors, trapdoors, stairs, diodes, buttons, wall signs,
      statues, stonecutters, grindstones and bells all use `HorizontalDirectionalBlock.FACING`.
    - Wall bells (`WALL_BELL`): `supported(FACING)` (a wall bell's `canSurvive` checks the block it faces), then `ATTACHMENT` is set to
      `DOUBLE_WALL` if the side behind is sturdy too, else `SINGLE_WALL`, the same check `BellBlock.getStateForPlacement` makes. Everything else that's a plain enum (hinge, stair shape/half, chain axis,
      six-way facing, rail shape, statue pose) goes through `cycle`, which steps through the property's values in vanilla's order
      (sneak: backwards) with `Util.findNext/PreviousInIterable`, exactly as the debug stick does.
  - **Two update modes.** The first batch of blocks uses the debug stick's quiet flags (`FLAGS` = `UPDATE_CLIENTS |
    UPDATE_KNOWN_SHAPE`, no neighbour or shape updates), so fence and wall sides stay as set. The second batch (`updatesLikePlacing`:
    pistons, dispensers, observers, hoppers, diodes, rails, buttons, signs, banners, heads, statues) updates the way placing it would, so redstone stays
    right: `setBlock` with `UPDATE_ALL`, `updateNeighborsAt` around each neighbour (whatever the old way round powered is recomputed),
    then `neighborChanged` on the block itself (a repeater re-reads its input, a powered piston extends, a powered rail re-checks its
    power). Observers watching the block pulse, as in vanilla. A change that changes nothing (a down hopper's facing) sets nothing.
  - **Only onto supported sides** (Dylan's pick): `supported` repeats a change until `stays` (the block `canSurvive` there, and a rail
    slope has a block on its rising side, a copy of the private `BaseRailBlock.shouldBeRemoved`). Having come full circle, the block is
    left as it was. Used by buttons, wall signs, wall hanging signs, wall banners, wall heads and both rail options, so the stick never
    pops a block off. Wall heads always `canSurvive` (they float if the wall goes), so `stays` gives them vanilla's placement rule
    instead (`WallSkullBlock.getStateForPlacement`: the block behind can't be `canBeReplaced`), or they could turn to face into air.
  - Statue pose plays vanilla's `COPPER_GOLEM_BECOME_STATUE` sound and `BLOCK_CHANGE` game event (copied from its protected
    `updatePose`). 16-way directions show as compass points (`COMPASS`: `ROTATION_16` 0 = the front faces south, each step clockwise),
    built by `standing(offset)`. Signs and banners use offset 0; heads use 8, because vanilla places them without the sign's half turn
    (`convertToSegment(rot)` vs `rot + 180`), so a head's 0 faces north. Block entities stay through the `setBlock` (same block), so
    banner patterns and player-head skins are kept.
  - **Armour stands** (and item frames and cushions) are entities, so they have their own path (`onAttackEntity`, `onUseEntity`), only for the stick on a non-marker
    `ArmorStand`, not spectating, `mayBuild()`. Every other entity passes to vanilla (the stick still hits mobs, villagers still trade).
    - Left click: Fabric's client hook sends the attack when the callback returns SUCCESS, and on the server a non-PASS result cancels
      `Player.attack`, so the stand is never hit. The server selects `facing` and shows its compass point. Entity attacks don't repeat
      while held, so no `REPEAT_TICKS` gate.
    - Right click: runs before `ArmorStand.interact`, so the stick is never put in the stand's hand. The server rounds the yaw to the
      nearest 45° (`ArmorStandItem` places in 45° steps), turns ±45° and applies it with `forceSetRotation` (vanilla `/rotate`'s path,
      which clients are sent). Yaw 0 faces south, so it shares `COMPASS` (yaw / 22.5).
    - **Item frames** (and glow item frames, a subclass) take the same path (`isStickOnEntity`): left click selects `frame` without
      popping the item out or breaking the frame; right click (before `ItemFrame.interact`, so the stick never goes in and the item
      never turns) flips vanilla's `setInvisible`. Vanilla saves it as `Invisible`, syncs it (an entity shared flag) and draws a hidden
      frame as its item alone, flat to the wall. Cushions take this path too (see rainbow things).
      F3+B hitboxes still show for hidden frames (Dylan, 2026-09-25: so they're not forgotten, being entities that can lag):
      `client/mixin/EntityHitboxDebugRendererMixin` skips vanilla's `isInvisible()` check in `emitGizmos` for `ItemFrame`s only.
  - Doors: after the clicked half, the other half gets the same state with its own `HALF`, the copy vanilla's
    `DoorBlock.updateShape` does.
  - Iron trapdoor state plays vanilla's iron trapdoor open/close sound and `BLOCK_OPEN`/`BLOCK_CLOSE` game event (copied from the
    protected `TrapDoorBlock.playSound`). Every other change is silent, like the debug stick. It stays open until its redstone power
    changes, as in vanilla.
  - Walls: sides are `WallSide`, so on = LOW, then vanilla's own `updateShape(..., Direction.UP, ...)` (its "block above changed"
    path) picks LOW/TALL per side and whether the post shows. The `up` property is never set directly.
- Texture: vanilla `stick.png` blended 60% toward an amethyst purple at each pixel's own brightness (throwaway Java script, not kept).
- **Locked blocks** (Dylan, 2026-09-25, from a picture: glass pane, stained glass panes, iron bars, copper bars, walls, fences, rail,
  powered / detector / activator rail; later stairs, fence gates and placed rods, below). Vanilla used to recompute a stick-set side whenever a neighbour changed (a barrel
  opening next to a pane re-connected it). Now any stick change to one of them locks it (his pick: automatic, no option; break and
  re-place to unlock). `block/LockedBlocks`: a boolean `locked` property that `BlockMixin` adds to `FenceBlock`, `IronBarsBlock`
  (panes, stained panes, iron + copper bars), `WallBlock` and `BaseRailBlock`, forced false in the default state (as with `lily_pad`),
  so placement always starts unlocked. `onUseBlock` sets it when the change changed something.
  - Fences, panes, bars, walls: `BlockStateBaseMixin`'s `updateShape` return hook gives back the unchanged state when locked (vanilla's
    code still runs, so a waterlogged one still schedules its water tick), then the lily-pad check as before. `wallSide` unlocks the
    state for its own `updateShape(UP)` pass, or the lock would keep the old heights.
  - Rails: `mixin/BaseRailBlockMixin` makes `updateDir` a no-op when locked (lever at a T-junction, piston landing, the stick's own
    `neighborChanged`). `mixin/RailStateMixin`: a locked rail's `canConnectTo` is only "already points there" and its `connectTo` is
    cancelled, so a rail placed beside it can't bend it (and only joins it if it already points at the new rail). Power and detector
    presses are separate paths and still work; a rail still pops off without its floor or slope support.
  - Stairs, fence gates, placed rods (Dylan, 2026-09-25: "I altered stairs but as soon as I placed a block near it, it reset it"; he asked
    that everything the stick sets stays put). Checked against 26.3's `updateShape` of every stick block: only these three (plus the
    ones above) have something neighbours recompute: a stair's `shape` (`getStairsShape` on any horizontal update), a gate's `in_wall`,
    an upright rod's arms (`PlacedRodBlock.updateShape`, a turned-off arm grew back toward a matching rod). Same `updateShape` hook.
    Left to vanilla on purpose: doors (each half copies the other; locking would break opening), wall bells (re-hang single/double and
    turn to the remaining wall, which is vanilla keeping them attached), repeater locking, redstone open/powered. Every other stick block
    only schedules water ticks or pops when unsupported. The hook keeps a locked state only while vanilla still returns the same block,
    so anything vanilla removes still goes. Locked gates still open and take redstone (`neighborChanged` / `useWithoutItem` set it
    directly, not through `updateShape`). The rainbow toggle never locks.
- Known edges: a *new* block placed beside a locked one picks its own shape from its neighbours as usual (a new fence can reach an arm
  toward a locked fence whose side is off); the locked block itself stays as set. First-batch changes don't update neighbours, so a
  rotated gate leaves the fence arms beside it as they were.

## Decorations on lily pads
Spec from Dylan (2026-09-24, from a picture): these stand **on** a lily pad, in the pad's own block space, every variant included: glass
and stained glass panes, iron and copper bars, iron and copper chains, candles, standing banners, amethyst cluster and buds, sea pickle,
torch / soul / copper / redstone torch, lanterns, standing signs, end rod, lightning rods, placed stick / blaze rod / breeze rod (his
pick), standing heads, decorated pot, flower pot and every potted plant, turtle egg, buttons, and the armour stand. Added 2026-09-25:
copper golem statues (every stage, waxed too) and item frames / glow item frames lying flat on the pad; later the cactus flower
(`CactusFlowerBlock`; the pad stands in for the cactus or sturdy top vanilla wants under it).
**Breaking goes by aim (his pick):** aim at the
decoration and only it comes off, leaving the pad. Aim at the bare pad edge and the pad breaks, and the decoration drops too (whatever
the tool, as when its support goes).

- **A property, not combo blocks.** The deleted old mod made one block per combination (hundreds of blocks, ~1000 models). Now
  `mixin/BlockMixin` adds a boolean `lily_pad` (`block/LilyPadDecorations.LILY_PAD`) to every block `canSitOnPad` accepts (by class,
  minus the wall subclasses). A torch on a pad is a real torch with `lily_pad=true`, so light, redstone power, candle lighting and
  stacking, sign text, banner patterns, pot storage, copper weathering (`withPropertiesOf` copies it) and the Builder Stick
  (`setValue`) all work unchanged. No blockstate JSON changes: vanilla variant keys match partial property sets.
  - **Default-state trap:** `BooleanProperty`'s first value is true and blocks build defaults from `stateDefinition.any()`, so
    `BlockMixin` forces it false at the head of `registerDefaultState`.
- **Placing:** `BlockStateBaseMixin.canBeReplaced` lets a bare lily pad make room for a held decoration from any side (like adding
  a candle). `BlockItemMixin` (and `PlacedRods.place`) then swap in `block/LilyPadPlaceContext`: face UP and DOWN first in
  `getNearestLookingDirections`, so each block's own placement picks its standing / floor form. The state gets `lily_pad=true`.
  Stacking a second candle / pickle / egg onto one on a pad is plain vanilla (the property is kept).
- **Survival:** `BlockStateBaseMixin.canSurvive` answers "could a lily pad stay here" for pad states, and during placement (the
  pad is still in the world). `updateShape` turns a pad state to air when the pad can't stay, because panes, chains, rods, heads
  and pots never check below.
- **Shapes:** outline = decoration + a 16-wide pad (so a pot's pad still has an edge to aim at). Collision = decoration + vanilla's
  14-wide pad. Cached per shape in `withPad`.
- **Breaking:** `LevelMixin` makes `removeBlock` on a pad state leave `LILY_PAD` (player breaks in survival / creative, turtle eggs
  trampled). `ServerPlayerGameModeMixin` goes round that when `aimsAtPad` (a `player.pick` raycast, outside the decoration's own
  shape) and forces the tool check true. `client/mixin/MultiPlayerGameModeMixin` makes the client's prediction match.
  `getDestroyProgress`, the break particles (`BlockMixin.spawnDestroyByEntityParticles`) and hit particles (`ClientLevelMixin`) are
  the pad's when aimed at the pad.
  - **Drops:** `getDrops` adds the lily pad's own loot only when the block at the loot origin is no longer a lily pad, so the pad
    drops from explosions, lost water, pistons (a pad state is `POPPED`, as a pad) and pad-aimed breaks, but isn't duplicated.
- **Model:** `client/model/LilyPadDecorationModel` wraps every pad state's baked model (`modifyBlockModelAfterBake`). It draws
  vanilla's lily pad model seeded with the pad's own `getSeed`, so the pad keeps the rotation it had, coloured with
  `BlockColors.LILY_PAD_IN_WORLD` baked in (the tint would otherwise be looked up for the decoration and come out grey). Then it
  draws the decoration. Block-entity decorations (signs, banners, heads, pots) draw through their renderers as usual.
- **Flower pots:** planting, taking a plant out and a potted eyeblossom opening or closing swap in another `FlowerPotBlock` built
  from its default state. `mixin/FlowerPotBlockMixin` wraps those `setBlockAndUpdate` calls with `LilyPadDecorations.keepPad` so
  the pot stays on its pad.
- **Outline:** `client/mixin/LevelExtractorMixin` outlines only the aimed part (`partShape`: the bare pad or the decoration
  alone), the part a break would take. Aiming uses the same `partShape`, so the two always agree. The raycast still hits both.
- **Armour stand:** vanilla already lowers a stand clicked onto a pad's top onto the pad (`EntityType.create` with `tryMoveDown`).
  `mixin/ArmorStandItemMixin` only covers aiming into the pad's space from a neighbouring block, which vanilla would reject.
- **Item frames** are entities, so they aren't pad states; the pad stays a plain lily pad. `mixin/HangingEntityItemMixin`: a frame
  clicked onto a lily pad (any face) goes in the pad's space facing UP, as on a floor. `mixin/ItemFrameMixin`: `createBoundingBox` of
  an UP frame over a lily pad is moved up the pad's collision height (1.5 px, where vanilla stands an armour stand), so it's above the
  pad's outline and can be aimed at and hit; the renderer follows the box. `survives` counts the pad as solid support, so when the pad
  goes, vanilla's own 100-tick check pops the frame. `frameOnPad` only looks at loaded chunks (the box is also worked out while the
  entity is loaded or generated). A frame on a pad is a plain bare-pad case: a decoration placed into the pad later pops the frame.
- Known edges: boats don't break a decorated pad (vanilla only checks `LilyPadBlock`), walking sounds are the decoration's, and a
  middle-click on the pad edge picks the decoration.

## Stacked heads (two heads in one block)
Spec from Dylan (2026-09-24): a standing head (any mob head, the player head, and the **dragon head too**, his pick, overlap accepted)
goes on top of another, two heads in one block. The Builder Stick turns each head on its own, and only the head under the cursor is
outlined. Stacks stand on lily pads. **Breaking either head takes only that head** (2026-09-25: he first asked for "bottom breaks the
whole stack", then removed it). The top head then **stays where it was**, half a block up, and a head can be put back under it (his picks).

- **The bottom head stays the real vanilla head** (the lily-pad pattern, not a new block), so its skin, rotation, note-block sound,
  powered animation, `lily_pad` and Builder Stick work unchanged. `block/StackedHeads` holds the logic. `BlockMixin` adds two
  properties to every `SkullBlock` (standing heads only):
  - `TOP` (`top_head`: none / head / piglin / dragon) only says what *shape* the top head has (piglin 10 wide, dragon outline 8.5
    tall), so shapes come from the state. `NONE` is the first value, so it's the default without `BlockMixin`'s boolean fix.
  - `RAISED` (`raised`): a single head half a block up, left behind when the head under it broke. Never set with `TOP`. `BlockMixin`
    forces it false in the default state (a boolean's first value is true), as with `lily_pad`.
  - The top head itself (`Head`: block, rotation, and the three item components a `SkullBlockEntity` keeps: profile, note block
    sound, custom name) is extra data on the bottom head's vanilla block entity (`TopHeadHolder`, `mixin/SkullBlockEntityMixin`, saved
    as `bettervanillabuilding:top_head`; the update tag is the saved data, so clients get it). A stack with no data (`/setblock`) shows
    `Head.fallback`: a skeleton skull, piglin or dragon head by `TOP`. `Head` also carries a broken head on its way to being dropped.
- **Placing:** `BlockStateBaseMixin.canBeReplaced` → `takesHead`: a standing head with a free half, a head item in hand, not sneaking
  (as with candles, so a sneak click still puts a head in the block above), and either a click on the free half's face (a single
  head's top face at y ≥ 0.5, which rules out a lily pad's top; a raised head's underside at y ≤ 0.5) or into the head's space from a
  neighbour (vanilla's slab rule; e.g. the floor under a raised head). `BlockItemMixin` then uses the item's standing block's own
  `getStateForPlacement` (facing the player; `StandingAndWallBlockItem` would try a wall head first), checks the finished stack is
  unobstructed, and `placeBlock` becomes `StackedHeads.placeHead`:
  - Onto a single head: same block, so the bottom block entity stays; the new head becomes the stored top head; fires `PLACED_BLOCK`.
    The `placedState.is(...)` check after placing is made false, or vanilla would copy the new item's skin/name onto the **bottom**
    head's block entity.
  - Under a raised head: the raised head (with its block entity's components) becomes the stored top head and the new head is the real
    bottom head, so vanilla's own after-placing steps (the item's skin/name onto the block entity, `setPlacedBy`, `PLACED_BLOCK`) are right
    and are left alone. `lily_pad` is carried over.
  - Runs on the client too, so both heads show at once.
- **Shapes:** `mixin/SkullBlockMixin` → `StackedHeads.shape`: a stack adds the top head (vanilla's head shapes 8 px up); a raised head
  is only its own shape 8 px up. The lily-pad mixin adds the pad after that, so for `aimsAtPad` the whole stack is "the decoration".
- **Aiming** (`aimsAtTop`, a `player.pick` raycast like `aimsAtPad`; `hitsTop` for a hit already known): the hit point is inside the
  top head's outline. Outline (`client/mixin/LevelExtractorMixin`): the pad if aimed at, else the top or bottom head alone (`partShape`).
- **Breaking** (`breakHead`, used by `ServerPlayerGameModeMixin` and the client's prediction in `client/mixin/MultiPlayerGameModeMixin`;
  aiming at the pad works as before):
  - Top head: its data is cleared from the block entity and the state loses `TOP`.
  - Bottom head: the block becomes the top head's own standing state with `RAISED` (keeping `powered` and `lily_pad`). Its block entity
    (kept if it's the same kind of head, else a new one) gets the top head's components and no stored top head.
  - Either way `playerDestroy` is called for the broken head's own block and state with `tempEntity` (a throwaway `SkullBlockEntity`
    carrying its components), so vanilla's loot table drops it with its skin and name and the stat is that head's.
  - Anything else that breaks a stack (explosions, pistons, a lost lily pad) breaks both, through `getDrops`, where
    `BlockStateBaseMixin` adds `topDrops` (the same temp-entity trick).
- **Drawing:** `client/mixin/SkullBlockRendererMixin` (+ `SkullBlockRenderStateMixin`, `client/blockentity/TopHeadRenderState`) draws
  the top head with vanilla's model, skin (`playerSkinRenderCache`, as the private `resolveSkullRenderType`) and animation, translated
  0.5 up and turned by its own rotation. While the local player mines the top head, the crack overlay moves to it. A raised head's own
  transformation is moved 0.5 up. `mixin/AbstractSkullBlockMixin` gives the client the powered-animation ticker when the *top* head is
  a piglin or dragon.
- **Builder Stick:** aimed at the top head, the stick works on `top.state()` as a stand-in (so `STANDING_HEAD`, messages and sneak are
  the same) and `setTopRotation` writes the result back (`sendBlockUpdated` sends it). The bottom head, and a raised head, work as
  before; the `setBlock` keeps the block entity, so a top head survives.
- **Middle-click:** `ServerGamePacketListenerImplMixin` picks the aimed head. Ctrl+pick gives only that head's components, never the
  stack's block-entity data.
- Known edges: breaking one head shows break particles (and mining-crack particles) over the whole stack, because the client spreads
  them over the state's shape (all heads use the same particles and sound, so nothing else differs); a dragon head overlaps its
  neighbour; a wither skull on top of a stack doesn't count toward the wither summoning pattern (one at the bottom, or raised, does); multiplayer
  crack overlay follows the local player's aim.

## Aimed leaf litter and flower beds
Spec from Dylan (2026-09-25): leaf litter ("brown leaf pile"), pink petals and wildflowers ("2 flower piles") put each piece in the
**quarter of the block you aim at**, instead of vanilla's fixed pattern. Aiming at a filled quarter places nothing (as a full pile).

- Vanilla (`LeafLitterBlock`, `FlowerBedBlock`, the only `SegmentableBlock`s) stores `facing` + an amount 1-4: piece 1 is in the
  `facing` quarter (north = NW, east = NE, south = SE, west = SW), each next piece one quarter counter-clockwise. Every quarter has
  its own model (flowerbed_1..4 differ in height and stems; leaf litter uses combined models `_2` west half, `_3` SE, `_4` whole).
- `block/AimedSegments` adds `segment_order` (`BlockMixin`): the order the pieces fill in, `1234` (vanilla, default, first value) |
  `1342` | `1423`. The pieces present are the first `amount` of that order, so every set is covered: {1,3} is `1342` (diagonal),
  {1,4} `1423`, {1,3,4} `1342`, {1,2,4} `1423`. Piece 1 is always the first one placed, so no piece changes model when another is
  added. The amount stays the real count, so loot (drop count), bone meal (next piece in the order), worldgen, `/setblock` and
  structure rotation are vanilla's untouched.
- `mixin/SegmentedPileMixin` (`@Mixin({LeafLitterBlock, FlowerBedBlock})`, all `@ModifyReturnValue`):
  - `canBeReplaced`: the same item only goes into an empty quarter (`isFree`). Other items keep vanilla's answer (leaf litter is
    `replaceable()`). A filled quarter makes vanilla try the space above, where a pile can't survive, so nothing is placed.
  - `getStateForPlacement` → `place`: a new pile gets `facing` = the aimed quarter; an existing one gets the quarter added and the
    matching order (`1234` at 1 and 4).
  - `getShape` → `shape`: non-vanilla orders get one box around their real pieces (`singleEncompassing`, as vanilla), cached.
  - Aim (`aimedQuarter`): click point nudged 0.001 back into the clicked face, so the side of a piece is that piece and a
    neighbour's side face gives the nearest quarter. Client and server see the same hit, so prediction matches.
- Models: `assets/minecraft/blockstates/{pink_petals,wildflowers,leaf_litter}.json` override vanilla with multipart `"OR"`
  conditions per order. Leaf litter needed one extra quarter model, `bettervanillabuilding:block/leaf_litter_ne` (template
  `template_leaf_litter_ne`, uv = position like vanilla's, so the texture joins). No new textures.
- **Support** (Dylan, 2026-09-25: "on the top of any full block", then "all leave blocks"): `AimedSegments.supports` = a sturdy
  top face (vanilla leaf litter's rule) or `#leaves` (vanilla gives leaves an empty support shape, so they never count as sturdy).
  `mixin/LeafLitterBlockMixin` widens leaf litter's `canSurvive` with it.
  `mixin/VegetationBlockMixin` gives `FlowerBedBlock` the same rule by widening `VegetationBlock.mayPlaceOn` (`supports`,
  plus vanilla's `#supports_vegetation`), so placing, popping off when the block below goes, and drops stay vanilla.
- Known edges: a resource pack replacing those three blockstates draws non-`1234` piles in vanilla's order.

## Mushrooms and fungi in any light
Spec from Dylan (2026-09-25): brown/red mushrooms, shelf mushrooms and crimson/warped fungus go on grass, podzol, mycelium, dirt,
coarse dirt, rooted dirt, mud, moss, pale moss, stone, granite, diorite, andesite, deepslate, tuff, oak log, sand, red sand,
dripstone block and both nyliums **in any light**. Data only, no code: vanilla already reads these from block tags.

- `data/minecraft/tags/block/overrides_mushroom_light_requirement.json` (`replace: false`): `MushroomBlock.canSurvive` lets a
  mushroom stand on anything in this tag in any light (vanilla: mycelium, podzol, nyliums). Anywhere else vanilla's rule stays
  (a solid block with raw light < 13). Mushroom spreading also uses `canSurvive`, so mushrooms spread over these blocks in daylight,
  like they already do on mycelium.
- `data/minecraft/tags/block/supports_warped_fungus.json` (`replace: false`): fungus has no light rule, only this tag
  (`#supports_crimson_fungus` includes it). Adds the stones, oak log, sand, red sand and dripstone block; the rest were already in it. Bone meal
  still only grows a huge fungus on its own nylium (vanilla).
- Shelf mushrooms need nothing: vanilla puts them on the side of any block with a sturdy side face, in any light.

## Flower clumps (up to four of the same flower in one block)
Spec from Dylan (2026-09-25, from a picture): clicking a flower with the same flower adds another, like candles, up to 4, each full
size and one per quarter. The 18 in his picture: dandelion, golden dandelion, poppy, blue orchid, allium, azure bluet, the four tulips,
oxeye daisy, cornflower, lily of the valley, closed and open eyeblossom, wither rose, brown and red mushroom (not torchflower). His
picks: **same flower only** (like candle colours), a **fixed pattern, diagonal first** (2 = north-west + south-east, 3 adds
north-east, 4 adds south-west), only when you **click the flower's own hitbox**, and it **breaks as a whole clump**, dropping every
flower (like candles). The first version put each flower in the aimed quarter; he had that removed.

- `block/FlowerClumps`: a `flowers` count 1-4, exactly like the candle's `candles`, added by `BlockMixin` to every `FlowerBlock` and
  `MushroomBlock` (by class: properties are added before blocks have ids). 1 is the first value, so the default is vanilla's single
  flower, random offset and all. Only blocks in the tag `bettervanillabuilding:flower_clumps` (data, the 18 above) ever clump; the
  rest (torchflower, modded flowers) just carry an unused property.
- **Placing:** `BlockStateBaseMixin.canBeReplaced` → `takesFlower`: in the tag, same item, not sneaking (candle rule), fewer than 4,
  and the click was on the flower itself. Vanilla also asks `canBeReplaced` of the block *next to* the clicked one (clicking the
  ground beside a flower asks the flower). `BlockPlaceContext.replaceClicked` starts out true and is only set from the first ask's
  answer, so `replacingClickedOnBlock()` is true exactly on the first ask (the clicked block itself); the flower requires it.
  `BlockMixin`'s `getStateForPlacement` hook → `place`: the flower already there + 1. A full clump sends vanilla on to the space above, where a flower can't stand, so nothing is placed.
- **Look:** `BlockStateBaseMixin.getOffset` is zero for a clump (vanilla's random sideways shift would break the pattern).
  `client/model/OffsetCopiesModel` (via `modifyBlockModelAfterBake`, shared with corner torches) draws the flower's own vanilla
  model once per flower, moved
  ±4 px to its spot in `PATTERN` with a `QuadTransform`, so size, texture, cutout, the open eyeblossom's glow and resource packs are
  all vanilla's. `mixin/FlowerClumpMixin` (`FlowerBlock` + `MushroomBlock` `getShape`): vanilla's shape of one flower at each spot,
  one box around them (`singleEncompassing`, as candles), cached per state.
- **Drops:** data only. `data/minecraft/loot_table/blocks/<flower>.json` for the 18 are vanilla's `candle.json` pattern: `set_count`
  2/3/4 on a `match_block` `flowers` condition, then `explosion_decay` (for a single flower the same odds as vanilla's
  `survives_explosion`). Hand, water, pistons and explosions all use it.
- **Vanilla paths that would lose flowers:**
  - `mixin/EyeblossomBlockMixin`: opening/closing swaps in the other eyeblossom's *default* state; `keepFlowers` keeps the count.
    The wave to nearby eyeblossoms (`filterState(s -> s == state)`) is widened to any state of that block, so clumps join in.
  - `mixin/MushroomBlockMixin`: spreading copies the whole state; it now spreads one mushroom (`single`).
  - `mixin/EndermanTakeBlockGoalMixin`: an enderman carries `defaultBlockState()`; it now carries the clump, puts it back as one, and
    drops all of it on death (its death drop uses `getDrops`).
- Known edges: bone meal on a mushroom clump grows one huge mushroom and the clump is used up (as a single mushroom is); a datapack
  replacing those 18 loot tables drops one flower per clump.

## Corner torches (crouch to put a torch where the cursor is)
Spec from Dylan (2026-09-25): crouch-placing a torch, soul torch or copper torch on a full block puts it **where the cursor is**: the
aimed quarter of the top (up to 4 torches), or the aimed half of a side (only **two** wall torches, side by side). **Same torch type
only** in a group. Without crouching it's all vanilla. Redstone torches aren't included. After testing he changed two things:
- **Menu blocks** (crafting table, furnace, chest: you have to crouch to place anything on them) take corner torches too, and
  crouch-aiming at the **middle** of one gives vanilla's middle torch. On other blocks crouching always gives a corner torch (his pick).
- **A player breaks only the torch they aim at**; the rest of the group stays. Explosions, pistons and losing the block underneath
  still take the whole group.

- `block/CornerTorches`: vanilla's own torch blocks with extra properties (flower-clumps pattern), added by class in `BlockMixin`:
  - Standing torches (`TorchBlock` but not `WallTorchBlock`): four booleans `north_west`/`north_east`/`south_east`/`south_west`,
    forced false in `offByDefault`. All false = vanilla's centred torch. Quarters use `AimedSegments.aimedQuarter` (package-private,
    shared) and its naming (north = north-west, ...).
  - Wall torches (`WallTorchBlock`): `side` = `middle` (vanilla, first value, so the default) / `left` / `right` / `both`, as seen
    facing the wall (`right` = along `facing.getCounterClockWise()`).
  - Only blocks in the tag `bettervanillabuilding:corner_torches` (torch, soul and copper torch and their wall forms) ever spread;
    modded torches just carry unused properties. The redstone torch is a `BaseTorchBlock`, not a `TorchBlock`, so it has none.
- **Placing a new one:** `mixin/StandingAndWallBlockItemMixin` (`getPlacementState` return, the one place both forms come out of) →
  `place`: crouching, in the tag, and not `staysInMiddle`: the clicked block (`pos.relative(face.getOpposite())`) has a full face
  (`isFaceSturdy(..., SupportType.FULL)`), and if it has a menu (`getMenuProvider != null`) the click isn't `aimsAtMiddle` (the middle
  6×6 px of the top, or the middle 6 px strip of a side, left to right). A standing torch on face UP gets the aimed quarter; a wall
  torch whose `FACING` is the clicked face gets the aimed side.
- **Adding to a group:** crouch-click the same block face again. The group sits in the space in front of the clicked block, so vanilla
  asks the group `canBeReplaced` on its *second* ask (`!replacingClickedOnBlock()`); `BlockStateBaseMixin.canBeReplaced` →
  `takesTorch`: same torch item, crouching, the clicked face is the group's (UP / its `FACING`), and the aimed quarter/side is free.
  `place` then adds it to the group, whatever vanilla picked (vanilla might have picked a wall torch). A middle torch and a group never
  share a block.
- **Breaking one torch** (stacked-heads pattern): `torches(state)` is each torch alone, as a one-torch group state. `aimedTorch` (a
  `player.pick` raycast, like `aimsAtPad`; groups of 2+ only) is the one whose shape the hit is in, else the nearest. `breakTorch`
  turns it off (`both` → the other side). `ServerPlayerGameModeMixin` does that in place of `removeBlock` and hands the one-torch state
  to `playerDestroy` (`@Share("brokenTorch")`), so the loot table drops exactly one. `client/mixin/MultiPlayerGameModeMixin` makes the
  client's guess match, and `client/mixin/LevelExtractorMixin` outlines only the aimed torch. A lone corner torch breaks the vanilla way.
- **Look:** `client/model/OffsetCopiesModel` draws the torch's own vanilla model at each spot (±4 px). `mixin/CornerTorchShapeMixin`
  (`BaseTorchBlock` + `WallTorchBlock` `getShape`): vanilla's shape at each torch, **unioned** (not one box), so the top between
  torches can still be clicked. `mixin/TorchParticlesMixin` (`TorchBlock` + `WallTorchBlock` `animateTick`) wraps both
  `addParticle` calls, so each torch has its own smoke and flame. Light is vanilla's (one light source per block).
- **Drops:** data only. `data/minecraft/loot_table/blocks/{torch,soul_torch,copper_torch}.json` (each wall torch already uses its
  standing torch's table via `overrideLootTable`): `add` 1 per corner set, `add` -1 if any corner is set (`any_of`), `add` 1 for the
  wall torch with `side: both`, then `explosion_decay`.
- Known edges: structure mirror doesn't swap left/right or turn corners; a `/setblock` that swaps the block under corner torches for a
  non-full one leaves them standing (only vanilla's centre-support rule is checked afterwards); breaking one torch shows break particles
  over the whole group.

## Wall lanterns
Spec from Dylan (2026-09-25): lanterns go on walls, hanging off a small bracket in the **colours of the chain** (his pick: the iron
chain's own dark blue-grey for the lantern and soul lantern, and the matching copper chain stage for each copper lantern, so the bracket
oxidises with it). The bracket stops **exactly 1 px below the top** of its block (Dylan, 2026-09-26, was 2 px: "exactly 1 pixel below if a block is above it"), so it never touches a block above.
**Where you click picks the form**, even with a block above: a wall's side gives a wall lantern, a ceiling's underside a hanging
one, a floor's top a standing one. (Vanilla picks standing/hanging from where you *look*.)

- `block/WallLanterns`: a `wall` property (`none` = vanilla's lantern, the first value so the default; `north/east/south/west` = the
  side the wall is on), added by `BlockMixin` to every `LanternBlock` (copper lanterns are `WeatheringLanternBlock`s, a subclass).
  Only blocks in the tag `bettervanillabuilding:wall_lanterns` (the 10 vanilla lanterns, waxed included) are ever placed on a wall.
  Weathering, waxing and scraping keep it (`withPropertiesOf`). A wall lantern always has `hanging=false`.
- `mixin/LanternBlockMixin` (also merges `rotate`/`mirror` overrides into `LanternBlock`, which has none, so structures turn it):
  - `getStateForPlacement` HEAD → `WallLanterns.place`: by `getClickedFace()`: side = wall lantern, DOWN = hanging, UP = standing,
    used if it `canSurvive`, else vanilla's own look-direction loop. A click *into* a replaceable block (`replacingClickedOnBlock`:
    grass, a bare lily pad) is always vanilla, so lanterns on lily pads still stand.
  - `canSurvive`: a wall lantern needs a full sturdy face behind it (the wall torch's rule), so a fence or pane side falls back to
    vanilla's choice. `updateShape`: vanilla only re-checks above/below; the wall side going now drops it too (the `LadderBlockMixin`
    pattern).
  - `getShape`: vanilla's standing shape 1 px up plus the bracket's plate and arm, `Shapes.rotateHorizontal` per side.
- Look (wall on the north; blockstates rotate it y 90/180/270): `models/block/template_wall_lantern` = vanilla `template_lantern`'s
  elements 1 px up, where the hanging lantern's body is (body y 1–8, cap 8–10; Dylan, 2026-09-26, was 2 px up), hanging by a
  3-px chain (y 10–13, the handle and chain planes lengthened with the hanging lantern's own UV rows) from the tip of a flat 1-px bracket (x 7.5–8.5), drawn from
  Dylan's picture (2026-09-25, replacing a first plate + straight arm): a **3×6 base plate** on the wall (x 6.5–9.5, y 9–15,
  z 0–1; Dylan, 2026-09-26: was 1 wide, then one more layer on top of the plate only, so it pokes up 1 px above the arm); a top arm y 13–14, z 1–9; a brace stepping up from the plate's bottom to the arm (z 1–2
  y 10, z 2–4 y 11, z 4–6 y 12; Dylan had the step's last pixel, z 6–7 next to the handle, removed), leaving a triangle hole
  between them. Side view, wall on the left, y 14 at the top (the first column is the plate):
  ```
  ■
  ■■■■■■■■■
  ■···■■
  ■·■■
  ■■
  ■
  ```
  Children `wall_<lantern>` set `#lantern` and `#bracket` (waxed ones share, as in vanilla).
  `assets/minecraft/blockstates/<lantern>.json` (10 files): vanilla's two variants with `wall=none` added, plus `wall=<side>`.
- Textures `textures/block/lantern_bracket_{iron,copper,exposed_copper,weathered_copper,oxidized_copper}.png`: only each chain
  texture's own colours (mid with light and dark flecks, the brace's lower steps darker; exposed/weathered add their chain's
  weathering fleck). Layout: the arm and brace side view at x 1–8, y 0–3 (u = z out from the wall, v = 13 − y; the east faces flip
  u); 1-px edge strips along v: top faces x 12, undersides x 13, ends x 14; the plate: front x 0–2 y 5–10 (two rivets), sides x 3
  y 5–10, top x 0–2 y 11, bottom x 0–2 y 12. Made by a
  throwaway Java/ImageIO script (not kept); edit the PNGs to change them. The plate's sixth row (2026-09-26): its light top row
  moved up to y 5 and y 6 became a copy of its rivet-free row (y 8).
- Known edges: the Builder Stick doesn't turn lanterns (not asked for); a resource pack replacing the lantern blockstates draws wall
  lanterns with a missing model.

## More stairs and slabs
Spec from Dylan (2026-09-26): stairs and slabs for smooth stone (stairs only; vanilla has the slab), deepslate, moss, pale moss, snow
block, packed ice, blue ice, calcite, obsidian, block of amethyst and terracotta (plain + all 16 colours, his pick; not glazed). All go
in the stonecutter **except moss, pale moss and snow**. The Builder Stick works on them and every slab works with mixed slabs.
53 blocks: 27 stairs, 26 slabs.

- `block/ExtraStairsAndSlabs` registers them exactly as vanilla 26.3's `Blocks.registerStair` / `registerSlab` do: a plain
  `StairBlock` with `Properties.ofFullCopy(base)`, a plain `SlabBlock` with `ofLegacyCopy(base)`, both with a copy of vanilla's private
  `NEAR_PLANE_INTERSECTS_OUTLINE` view-blocking test. So hardness, blast resistance, sounds, map colour, ice friction, needs-a-tool and
  obsidian's `IMMOVEABLE` come from the base block. Each gets a `BlockItem` registered the way `Items.registerBlock` does
  (`useBlockDescriptionPrefix`, `registerBlocks(Item.BY_BLOCK, ...)`). Terracotta: `Blocks.TERRACOTTA` + `Blocks.DYED_TERRACOTTA.pick(color)`.
- **No code for the stick or mixed slabs**: both go by class (`BuilderStickItem.optionsFor`, `LockedBlocks`, `MixedSlabs`,
  `SlabBlockMixin`), so being real stairs/slabs is enough.
- Creative tabs: smooth stone stairs after smooth stone, deepslate after deepslate, amethyst after the amethyst block, the rest at the
  end of Building Blocks; terracotta in Colored Blocks after the last terracotta, every stair then every slab in vanilla's tab colour
  order (the concrete stairs/slab layout).
- **Drops (Dylan's pick: match vanilla):** packed/blue ice ones drop only with Silk Touch (nothing otherwise); snow ones need a shovel
  (the snow block's `requiresCorrectToolForDrops`) and drop themselves with Silk Touch, else snowballs: stairs 3, slab 2, double slab 4
  (the block is 4). Everything else drops itself (vanilla stair/slab loot).
- Data (all JSON, made from vanilla's concrete stairs/slab files by a throwaway Java script, not kept): blockstates (a slab's `double`
  is the vanilla block's own model), models over vanilla's `stairs`/`slab` parents and textures (deepslate: `deepslate_top` top and
  bottom; snow: `block/snow`), item definitions, loot tables, crafting (6 → 4 stairs, 3 → 6 slabs; terracotta in groups
  `terracotta_stairs`/`terracotta_slab`) and stonecutting (1 → 1 stairs, 1 → 2 slabs) recipes, a recipe-book advancement per recipe,
  lang. Tags (`data/minecraft/tags/`): `stairs`/`slabs` (block + item), `mineable/pickaxe|hoe|shovel`, `needs_diamond_tool` and
  `dragon_immune` (obsidian), `crystal_sound_blocks` and `vibration_resonators` (amethyst, so they chime when walked on).
- **Smooth stone stairs have their own models** (Dylan, 2026-09-26: "missing some 1x1 pixel borders"). Vanilla's `stairs` /
  `inner_stairs` / `outer_stairs` cut each face out of the texture by position (the step's side is uv `[0,0,8,8]`), so only edges
  that happen to be the texture's edges get smooth stone's dark 1-px border: the side's inner L and a corner stair's outer corner had
  none. His pick: **every face outlined all round**, like a block cut into the shape, so the crease where the step meets the lower
  step shows a border on both faces. `models/block/smooth_stone_stairs{,_inner,_outer}.json` are full models, made by a throwaway
  Java script (not kept): the shape in 4-px cubes; each exposed 4×4 tile takes its UV from the texture's edge on a side where the
  next tile in the same plane is missing (block edge, L, corner, crease), and from the interior otherwise; tiles whose UVs run on are
  merged. At an inside corner (both side neighbours there, the diagonal one missing) the two border lines leave one pixel out
  (Dylan, 2026-09-26: "missing 1 more pixel in the corners"), so that tile is split into the corner pixel (a pixel of the texture's
  left border column), the rest of its row and its other three rows. Each face is a flat element (vanilla `cullface` on the block's edges). Still `smooth_stone.png`, no new texture. The
  blockstate's `uvlock` rotations stay right because the border is the same on every side. To change them, edit the JSONs.
- **Vanilla's smooth stone slab, fixed to match** (Dylan, 2026-09-26: "this is a mojang's fault issue"): vanilla's
  `smooth_stone_slab_side.png` gives each half a light top and left edge instead of smooth stone's dark border. He asked for every
  edge to be dark. `assets/minecraft/models/block/smooth_stone_slab{,_top,_double}.json` override vanilla's models with plain
  `smooth_stone.png`: each 8-px half is two 4-px layers, the upper taking its sides from the texture's top rows (uv `[0,0,16,4]`),
  the lower from its bottom rows (`[0,12,16,16]`), so each half is outlined all round. The double slab keeps vanilla's look of two
  outlined halves. Slab faces where two halves meet still have no `cullface`, so `MixedSlabModel` drops them as before. A
  resource pack's own smooth stone slab models replace these.
- **Walls** (Dylan, 2026-09-29: "walls of all terracotta blocks, deepslate, packed and blue ice, calcite, smooth stone, and obsidian",
  then "moss, pale moss, snow, and amethyst block walls"): 27 walls, one for every material above (terracotta plain + 16 colours,
  deepslate, packed ice, blue ice, calcite, smooth stone, obsidian, moss, pale moss, snow, amethyst; vanilla 26.3 has none of them) in
  the same class, registered as `Blocks.registerWall` does: a plain `WallBlock` with `ofLegacyCopy(base).forceSolidOn()`. The Builder
  Stick and locking go by class (`WallBlock`), so nothing new there. Data made from vanilla's cobblestone wall files by throwaway Java
  scripts (not kept): multipart blockstate over `template_wall_post|side|side_tall` + `wall_inventory` models (one texture each, the
  base block's; deepslate uses its side texture, snow `block/snow`), item definitions, loot (ice: Silk Touch only, like the stairs;
  snow: itself with Silk Touch, else **4 snowballs**, Dylan's pick: the recipe is 1 block → 1 wall), crafting (6 → 6, terracotta in
  group `terracotta_wall`, no category like vanilla's) and stonecutting (1 → 1; not moss, pale moss or snow, like their stairs), recipe
  advancements under `recipes/decorations/` (where vanilla's walls are), lang, tags `walls` (block + item, new files), the same
  mining/sound tags as each material's stairs, and `needs_diamond_tool` + `dragon_immune` for obsidian. Tabs: each wall after its
  slab (smooth stone after vanilla's smooth stone slab), terracotta walls after the terracotta slabs in the tab colour order.
- **Smooth stone wall has its own models** (Dylan, 2026-09-29: vanilla's wall models left "missing a bunch of edges"): like the
  stairs, every face outlined all round. The border depends on the whole block (a straight run meets mid-block with no line; a side
  meeting a post is a crease with one), so its blockstate is `variants`, one model per `up` × 4 sides (162,
  `models/block/smooth_stone_wall/[post_]<n|l|t for N,E,S,W>.json`), plus `smooth_stone_wall_inventory.json` (parent
  `wall_inventory` for its display). A throwaway Java script (not kept) built each shape in 1-px cubes; a face pixel whose neighbour
  in the same plane is missing (air, a crease, the block's edge) takes the texture's border column/row, an inside corner takes a
  border pixel, and every other pixel vanilla's own (uvlock-style) pixel; runs are merged into flat elements (`cullface` on the
  block's edges). Still `smooth_stone.png`. To change them, edit the JSONs (or regenerate).
- Known edges (as with vanilla's own stairs/slabs): no base-block tricks, so moss doesn't spread with bone meal and amethyst doesn't
  chime when an arrow hits it.
