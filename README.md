# Invisible Frames — NeoForge 1.21.1

Sneak and right-click a **filled** item frame to toggle the frame's visibility.
The displayed item stays visible and does not rotate during the toggle. Removing
that item makes the frame visible again. Regular and glow item frames are supported,
including wall, floor, and ceiling placement. Normal insertion and rotation remain
unchanged, and fixed frames and canceled NeoForge interaction events are respected.

This branch backports [Roundaround's Invisible Frames](https://github.com/Roundaround/mc-invisible-frames)
to **Minecraft 1.21.1**, using **NeoForge 21.1.257** and **Java 21**. It is a
NeoForge-only branch; newer Minecraft and other loader implementations remain in
upstream history. Publishing to CurseForge/Modrinth is postponed. Tested builds are available from
[GitHub Releases](https://github.com/JulianGrabitzky/mc-invisible-frames/releases).

## Build

Install [Nix](https://nixos.org/download/) with `nix-command` and `flakes` enabled,
then run:

```sh
nix develop
./gradlew build
```

Or run a single command with `nix develop --command ./gradlew build`.
Without Nix, install a 64-bit JDK 21 and run the same Gradle wrapper command.

The production artifact is:

```text
neoforge/build/libs/invisibleframes-2.0.2+1.21.1-neoforge.jar
```

Install the JAR into the NeoForge server's `mods/` directory (or your local
NeoForge installation for single-player). No Fabric API, Allay, Trove, or other
library mod is required. Do not install this fork alongside the original mod:
they share the `invisibleframes` mod ID. The declared NeoForge minimum is 21.1.257;
older 21.1 builds have not been tested.

The supported minimum is pinned separately as `neo_min_version`; changing
`neo_version` for development/testing does not silently raise the JAR's requirement.

`flake.nix` and `flake.lock` pin the development tools, including JDK 21.
Use the checked-in Gradle wrapper, whose distribution has a SHA-256 check.
Commit both flake files together; update the tool pin deliberately with
`nix flake update nixpkgs`. For Nix installations without flakes enabled, add
`--extra-experimental-features 'nix-command flakes'` to Nix commands.
The development environment is pinned; this is not an offline or hermetic Nix
build of Minecraft. The first Gradle run downloads Minecraft, NeoForge, and tools.

## Test and develop

```sh
# Compilation and production JAR
nix develop --command ./gradlew build

# Native NeoForge GameTests; exits unsuccessfully if a required test fails
nix develop --command ./gradlew :neoforge:runGameTestServer

# Interactive development runs
nix develop --command ./gradlew :neoforge:runClient
nix develop --command ./gradlew :neoforge:runServer
```

The GameTest task accepts the [Minecraft EULA](https://www.minecraft.net/eula)
for its disposable world in `neoforge/run-gametest/`. Interactive server runs
use a separate directory and require their own EULA acceptance.
The Nix shell covers builds and headless server tests; running the graphical
client also requires a working host display and graphics drivers.

Tests live in `neoforge/src/gametest/`, run with a separate test mod, and are
excluded from the production JAR. They exercise actual player/entity interactions,
protection-event cancellation, frame types and orientations, item consumption,
rotation, removal, NBT save/load, and vanilla entity-data updates. They do not
replace checks in the actual modpack or visual checks with real game clients.

GitHub Actions builds on pushes and pull requests, runs the GameTests, and uploads
the production JAR under the run's **Artifacts** section (GitHub sign-in required).
Failure logs are uploaded separately. Dependabot tracks pinned action revisions.

For a public direct JAR download, open [GitHub Releases](https://github.com/JulianGrabitzky/mc-invisible-frames/releases)
and download the `.jar` under **Assets**. The **Downloadable JAR release** workflow
builds and tests before attaching the JAR and SHA-256 checksum to a prerelease.
Trigger it by pushing a tag such as `neoforge-1.21.1-2.0.1`, or use **Run workflow**
with a new tag after the workflow reaches the default branch. The built-in GitHub
token is used only for GitHub release creation; no CurseForge/Modrinth credentials
are configured. Existing releases are not overwritten by reruns.

The feature mixin is in `common/src/main/`; NeoForge build configuration,
entrypoint, metadata, and tests are in `neoforge/`.
See [the TODO and validation status](TODO-NEOFORGE-1.21.1.md) for completed work and
remaining modpack checks.

## Credits and license

Original mod by Evan Steinkerchner (Roundaround). NeoForge 1.21.1 backport maintained
in [this repository](https://github.com/JulianGrabitzky/mc-invisible-frames).
Licensed under [MIT](LICENSE); the original copyright notice is retained in the
source and packaged JAR.
