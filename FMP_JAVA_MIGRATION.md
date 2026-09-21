# ForgeMultipart Java API migration

This migration targets the local `algent/java` development artifact
`com.github.GTNewHorizons:ForgeMultipart:1.8.1:dev`, not an upstream 1.8.1 release.
Both Gradle compile and runtime configurations resolve
`C:/Users/Algent/.m2/repository/com/github/GTNewHorizons/ForgeMultipart/1.8.1/ForgeMultipart-1.8.1-dev.jar`.
The repository filter makes that coordinate local-only. The FML dependency is also pinned
to exactly 1.8.1; replace both pins when a compatible release is available.

Audited FMP reference: `965f1e1e204c302f13789c860462c2f3dceccaf6` on `algent/java`.
Dev jar SHA-256: `cea9003937d369d373567ea87a9fde442c6b46a2b70dcfa13cb4fe6e03845138`.
This republished development jar includes `ButtonPart.metaForSide`; the earlier artifact
at the same version did not. Runtime installations must use this updated artifact too.
The relevant published sources match the current reference sources. API signatures and
dispatch were also inspected in the actual dev jar. FMP's existing working-tree changes
were left alone. ProjectRed started clean at `e173952e96a4c7ee1743253c8cce634b3f5d7f8a`.

## Source inventory

Paths below are relative to `src/main/scala/mrtjp/projectred`. This includes direct FMP
imports; their ProjectRed subclasses and mixins were traced for inherited behavior.
The commented-out FMP import in `compatibility/thermalexpansion/LinkStateTesseract.java`
has no compiled dependency. There were no direct FMP reflection calls, raw client-tile
calls, traversal callbacks using `operate`, or uses of `TRandomUpdateTick`,
`TScheduledPacketPart` or `ScratchBitSet`.

| Area | Files | FMP behavior |
| --- | --- | --- |
| Core | `CoreRecipes.java`, `connectableparts.scala`, `connectabletiles.scala`, `libmc/PRLib.scala`, `libmc/recipe/inputs.scala`, `parttraits.scala`, `powerparts.scala` | Saw recipes, shape IDs/material items, tile/slot lookup, connectivity, lifecycle and packet traits |
| Expansion | `TileBlockBreaker.scala`, `TileBlockPlacer.scala`, `TileDiamondBlockBreaker.scala`, `TileFireStarter.scala`, `TileFrameMotor.scala`, `TileItemImporter.scala`, `TileSolarPanel.scala`, `partabstracts.scala`, `proxies.scala` | Redstone connectors, solar part placement/registration, cuboid geometry and effects |
| Exploration | `items.scala`, `proxies.scala` | Saw capability and block-material registration |
| Fabrication | `gatepartseq.scala`, `proxies.scala` | Loading-world timer context and IC gate registration; `fmpgatepart.scala` inherits integration gate behavior |
| Illumination | `LightMicroblock.java`, `blocks.scala`, `buttonpart.scala`, `items.scala`, `lightmicroblocks.scala`, `lightpart.scala`, `proxies.scala` | Lamp redstone, light/button factories, collision, occlusion, generated material traits and halos |
| Integration | `gatepart.scala`, `gatepartarray.scala`, `gatepartrs.scala`, `gatepartseq.scala`, `items.scala`, `packethandlers.scala`, `proxies.scala` | Gates, crossing exception, redstone, timer load context, placement, indexed packets and factories |
| Transmission | `APIImpl_Transmission.scala`, `RenderFramedWire.scala`, `bundledwires.scala`, `items.scala`, `powerwires.scala`, `propagation.scala`, `proxies.scala`, `redwires.scala`, `rsparts.scala`, `wireabstracts.scala` | Wire factories/placement, open redstone masks, indexed highlights, occlusion and propagation notifications |
| Transportation | `RoutedInterfacePipePart.scala`, `items.scala`, `pipeabstracts.scala`, `pipetraits.scala`, `pressurepathfinders.scala`, `proxies.scala`, `renders.scala` | Pipe factories/placement, slot lookup, neighbor callbacks, materials, occlusion and indexed highlights |

## Migration and behavior

- Redwire opening queries now use `IRedstoneTile.openConnections`, retaining all masks and rotations.
- All FMP part-list reads use `jPartList`. Packet indices and highlight indices retain stored order.
  Light aggregation still includes every stored matching sibling, without filtering detached parts,
  and captures the matching collection before size callbacks.
- All six module registrations use `registerPartFactory`, with the same IDs and order. Existing
  NBT/packet factories remain unchanged. The three former Boolean factories retain their side
  dispatch and the old adapter's client-preview behavior. Placement uses `loadPart(name, null)`,
  which has the same construction-only behavior as the old `createPart(name, false)`.
- Lights and gates extend `JCuboidPart`; the electrical Scala trait forwards its cuboid callbacks.
  Normal occlusion explicitly combines the helper with the existing super chain for lights,
  gates, electrical parts, wires and pipes. The array-gate crossing exception still precedes
  the normal check. Gate and electrical hit/destroy effects explicitly use `IconHitEffects`.
- `TItemMultiPartPlacement` forwards to `JItemMultiPart` before the existing glass-sound mixin.
  Compiled item dispatch remains sound wrapper → placement → sound on success.
- Only the generated `LightMicroblock` trait was converted to Java. Registration uses its name
  before class loading. Thin overrides delegate through `Object` to ordinary Scala helpers, which
  cast to `Microblock`. There are no member calls through the transformed Java input type.
- Lamp materials retain metadata 16–31, names, lamp identity, cutter strength and registration
  order. The existing configuration is read on each light query. Size-based dimming, saturation,
  pass-zero halos, tile coordinates, colour, copied/expanded ordinary boxes and four rotated hollow
  strips are preserved. Rendering stays client-only; common light queries do not invoke rendering.
- Existing NBT keys, packet fields/order, notification ordering, world callbacks and tick behavior
  are unchanged. ProjectRed's `TSwitchPacket` still handles packet dispatch. Button scheduling and
  geometry continue through FMP's ordinary `ButtonPart` hierarchy. No FMP changes or reflection
  workarounds were introduced.

## Button lookup and retained facade dependencies

The inverse button-mapping gap is closed. `illumination/items.scala` now calls the supported
`ButtonPart.metaForSide(side ^ 1)` accessor. Its source and published bytecode read the same live
inverse mapping, including legacy remapping/replacement and unmapped results. Vertical-face
rejection, support validation, attachment-face conversion, custom construction and `onPlaced`
retain their existing order and behavior. No direct button orientation-array access remains.

The timer-loading code still uses the Java facade `MultipartSaveLoad.loadingWorld()`: parts are not
yet bound during NBT loading, and no replacement loading-context accessor exists. Changing to the
part's world or delaying restoration would change saved elapsed-time behavior. Other retained
facades (`MultipartProxy.block`, `MicroblockProxy` item/saw getters and shape/material statics)
already provide Java-typed access; their companion dispatch is internal to FMP. ProjectRed emits
no direct FMP `MODULE$` or `$class` calls. The one-argument destruction callback is retained for
the `TIconHitEffects` contract and FMP's two-argument callback delegation.

No remaining ProjectRed source dependency on FMP's Scala-shaped APIs was found in this audit.
FMP must preserve the Java facade signatures and callback contracts while replacing their
internal implementation. ProjectRed still uses Scala for its own implementation; Gradle resolves
`org.scala-lang:scala-library:2.11.5` directly on `compileClasspath`, independently of FMP.
Client/server validation, released-consumer adoption and replacing the temporary dependency pins
remain release gates rather than known source-migration blockers.

## Verification and remaining game checks

Passed `gradlew.bat check --offline --console=plain`: compilation, Spotless, Checkstyle,
reobfuscation and 19 JUnit tests (14 existing, five dispatch regression tests).
The tests inspect actual compiled redstone, placement/sound, button lookup, geometry/effects, array-gate
occlusion, packet and generated-light helper dispatch without initializing game classes.

An additional constant-pool audit covered 2,248 compiled ProjectRed classes and 104 distinct
direct FMP member references: no raw `scalatraits`/`TileMultipartClient` member calls, no direct
FMP companions or trait helpers, no button orientation-array references, and no `partList`, `operate`, `registerParts` or Boolean
`createPart` calls remain. `javap` confirms the light helper's runtime `Microblock` casts and
the client annotation on the Java render override. This verifies consumer bytecode, not a
Forge-transformed/generated class or a running physical client.

Still required before release/pack adoption:

- Launch a dedicated server and physical client with this exact FMP artifact and the rebuilt mod.
- Load existing saves; place, update, remove, reload chunks and move representative gates, wires,
  pipes, solar panels, lights and buttons. Check NBT/timers, packet indices and callback effects.
- Exercise redstone openings/obstructions, crossed array gates, collision/occlusion, placement
  sounds, breaking/hit particles, solar ticking and scheduled button release.
- Exercise all 16 illuminated materials and face/hollow/corner/edge/post shapes on both sides;
  test dimming changes, aggregate light, connector-dependent hollow halos on all six faces,
  pass filtering, previews and rendering in the target pack.
- Publish compatible consumer/FMP releases and
  validate target-pack adoption before retiring compatibility surfaces.
