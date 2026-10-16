# NeoForge 1.21.1 validation

## 2.0.2 compatibility correction

The user screenshot showed the actual pack running **NeoForge 21.1.257**, while
2.0.1 incorrectly required 21.1.259. Version **2.0.2+1.21.1** builds/tests against
21.1.257 and declares `[21.1.257,21.2)` in its packaged metadata. `neo_min_version`
is separate from `neo_version`, so overriding the development loader does not
raise the supported minimum.

`nix develop --command ./gradlew clean build :neoforge:runGameTestServer --no-daemon`
passed all **14 required GameTests on NeoForge 21.1.257**. The mod loaded
successfully on that exact loader version; the production JAR was independently
inspected for its version and dependency range. No feature-code changes were
needed for the compatibility fix. The same 14 tests also pass with
`-Pneo_version=21.1.259`; the JAR remains byte-identical and still declares the
21.1.257 minimum after that override.

Artifact: `neoforge/build/libs/invisibleframes-2.0.2+1.21.1-neoforge.jar`.
SHA-256: `8139bc9c3b9904391fcc9170cdefbbe5944a16d4ac8b0dd6b0349f94b72fe47f`.

The remaining graphical-client and actual-modpack checks below still apply.

## Historical 2.0.1 validation

Validated on 2026-10-16 using the pinned Nix environment on x86_64 Linux/WSL2:
OpenJDK 21.0.12.1, Gradle 9.2.0, ModDevGradle 2.0.148,
Minecraft 1.21.1, NeoForge 21.1.259.

## Build and packaging

- `nix develop --command ./gradlew build`: passed.
- Built a second clean copy of the source with a new, empty `GRADLE_USER_HOME`:
  passed without Maven Local, Allay, or Trove artifacts.
- The normal and clean-cache production JARs were byte-identical.
- Inspected the production archive: the entrypoint, mixin, mixin JSON, expanded
  NeoForge metadata, icon, and MIT license are present. GameTest classes, test-mod
  metadata, Allay, and Trove are absent.
- Wrapper downloads verify the pinned Gradle distribution SHA-256.
- `nix flake check --no-build --all-systems`: passed for declared Linux and
  Apple Silicon macOS environments during environment setup. Runtime tests were
  executed on x86_64 Linux only.

Production JAR:
`neoforge/build/libs/invisibleframes-2.0.1+1.21.1-neoforge.jar`

SHA-256:
`9c248be197c16a2e1c75d3ce8d064bb085ca5d685ca2b310e08b120ea12b9a9e`

## Native server GameTests

`nix develop --command ./gradlew :neoforge:runGameTestServer`: passed all
**14 required tests**, with the actual mod and its mixin loaded:

1. Regular frame toggles twice without rotation or held-item consumption.
2. Glow frame toggles twice without rotation or held-item consumption.
3. Explicit offhand interactions toggle correctly.
4. Ordinary interactions still rotate both frame types.
5. Survival insertion consumes one item and keeps an empty frame visible.
6. Creative insertion does not consume the held item.
7. Attacking a filled frame removes the displayed item and reveals the frame.
8. Direct item clearing also reveals the frame.
9. Entity NBT save/load preserves invisibility, item, and rotation.
10. Vanilla entity-data updates carry the visibility flag and its reset.
11. A consumed main-hand interaction prevents a second-hand retry.
12. Fixed frames remain unchanged.
13. Canceled NeoForge protection/interaction events prevent toggling.
14. Both frame types toggle on all six mounting faces.

Most interactions go through `Player.interactOn`, including its NeoForge event
hook, and then the actual `ItemFrame.interact` implementation. These are not
network-packet or graphical client tests. The synchronization test checks vanilla
entity-data values, not visible rendering in a real client.

A temporary deliberately false assertion caused three required tests sharing
that assertion to fail. The server exited with status 3 and Gradle with status 1.
The mutation was restored and the full suite/build passed again. This verifies
that the test runner reports behavior failures to CI.

The GameTest task explicitly accepts the Minecraft EULA for its disposable world;
it does not change the EULA setting of an interactive or production server.

## CI

- `.github/workflows/build.yml`: passed `actionlint` 1.7.12.
- Source checkout, Java setup, Gradle setup, and artifact upload actions are pinned
  to commit SHAs. Dependabot checks GitHub Actions updates weekly.
- Runs on pushes, pull requests, and manual dispatch; builds and runs the native
  GameTests, uploads the production JAR, and uploads logs on failure.
- No CurseForge/Modrinth credentials or uploads are configured. The separate GitHub release workflow builds/tests and attaches a JAR plus checksum to a public prerelease, using the built-in GitHub token.
- A hosted GitHub run and branch protection are not verified by local linting.

## Packaged-server and modpack checks

The production JAR was copied into a separate server installed with the official
NeoForge 21.1.259 installer. The server discovered the mod and reached its running
state on localhost. No development/test classes or Trove libraries were needed.

Two-client protocol smoke tests were attempted with Mineflayer 4.39.0 /
minecraft-protocol 1.68.0 and Mineflayer 4.23.0 / minecraft-protocol 1.53.0.
Both encountered a Protodef `PartialReadError` while decoding incoming component
data (`ArmorTrimMaterial` / `ItemBookPage`). A control run on the same NeoForge
installation without Invisible Frames reproduced the parser failure. This check
was inconclusive; it does not demonstrate a mod defect or prove vanilla-client
compatibility. Temporary scripts, locked npm dependencies, and server logs were
retained under `/tmp/invisibleframes-packaged-smoke/client/` and the adjacent
server directories. Both test servers were stopped cleanly.

Actual modpack integration and graphical client checks remain separate from the
passing automated tests. Before adding the JAR to the maintained pack, verify:

- Two real game clients see both hide/reveal updates; a late join sees the current
  frame state. Confirm required/optional client installation for this port.
- A full server restart/chunk reload preserves the displayed item, rotation, and
  visibility. NBT unit coverage alone does not establish this end to end.
- Survival and creative interactions work with maps and glow frames mounted on
  walls, floors, and ceilings.
- The pack's actual protection and entity-interaction mods remain compatible.
- The packaged JAR runs in the single-player client, and the rendered item remains
  visible when its frame is hidden.

No modpack files or mod list were supplied. NeoForge 21.1.259 was the original
baseline; the later screenshot confirmed the pack actually uses 21.1.257, which
is the corrected 2.0.2 baseline above.
