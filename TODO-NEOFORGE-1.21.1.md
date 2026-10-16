# NeoForge 1.21.1 backport TODO

Repository review: 2026-10-16, starting at commit `2e5fb7a`.

Implementation is in progress on `t3/neoforge-port-roadmap`, the dedicated checkout for this backport; `main` retains the original revision. The agreed baseline is **Minecraft 1.21.1 / NeoForge 21.1.259 / Java 21**. CurseForge/Modrinth publishing remains postponed; GitHub release downloads are now authorized.

## Execution status

- Chapters 1–4: the NeoForge-only ModDevGradle build, metadata, and behavior backport are implemented. A clean source copy with an empty Gradle cache produces a byte-identical JAR. No Allay/Trove dependencies remain; the original mod ID and MIT notice are retained.
- Chapter 5: all **14 required native NeoForge GameTests pass**. A deliberate assertion failure was confirmed to make the task fail; it was restored and the passing suite rerun. Test code/resources are isolated from the production JAR.
- Chapter 6: build/test/artifact-upload GitHub Actions and Dependabot configuration are implemented. `actionlint` passes. The commands pass locally; a hosted GitHub workflow run and branch protection have not been performed.
- Chapter 7: a separately installed packaged server boots successfully with the JAR. Two-client protocol checks were inconclusive because both tested protocol libraries failed to parse data; a no-mod NeoForge control reproduced the error. Actual modpack testing remains pending because no pack files/mod list were supplied. Client compatibility/rendering remains unverified.
- Chapters 8–9: CurseForge/Modrinth publishing remains postponed. A separate GitHub release workflow now supports public JAR downloads after successful builds/tests. No third-party hosting credentials or platform projects were created.

Artifact: `neoforge/build/libs/invisibleframes-2.0.1+1.21.1-neoforge.jar`.
Commands and current installation instructions are in [README.md](README.md). Detailed evidence and remaining checks are in [docs/VALIDATION.md](docs/VALIDATION.md).

## Original repository snapshot (before the backport)

- **Core behavior:** `common/src/main/java/me/roundaround/invisibleframes/mixin/ItemFrameMixin.java` toggles frame invisibility when a crouching player interacts at the intercepted sound call. It cancels the remaining interaction and emits `GameEvent.BLOCK_CHANGE`. The intended use, also reflected in the client test, is a frame already holding an item.
- **Visibility reset:** a second injection makes the frame visible when its held item becomes empty. This depends on a specific call inside `onSyncedDataUpdated`.
- **Loader structure:** shared code in `common/` plus Fabric, NeoForge, and Forge subprojects. The NeoForge `@Mod("invisibleframes")` entrypoint already exists; it bootstraps Trove and has no other registrations.
- **Current target:** Minecraft `26.3`, mod version `2.0.1`, Gradle `9.2.0`. Loader versions use `latest`; Trove is pinned to `1.7.2+26.3`. This is a Minecraft version backport as well as build work, rather than adding NeoForge support from scratch.
- **Custom build/dependencies:** all projects use Allay `0.1.0-SNAPSHOT`; settings prefer `mavenLocal()`, and `allay_plugin_maven_local=true` is enabled. Trove core is bundled, with separate Trove game-test dependencies. No checked-in `neoforge.mods.toml` or mixin JSON exists at the current revision; the build relies on plugin behavior that must be verified or replaced.
- **Tests:** one client usage test checks a filled frame becoming invisible in single-player creative; one server smoke test checks startup through Trove. They are shared test sources, but the README only documents Fabric test commands. Their existence does not establish that NeoForge 1.21.1 is tested.
- **CI:** no tracked `.github/workflows/` files exist.
- **Publishing:** the root build already contains release settings, but targets Minecraft `26.3` and the original author's Modrinth project `invisible-frames-mod` and CurseForge project `1295518`.
- **Scope:** no custom items, blocks, recipes, commands, configuration screen, key bindings, or custom network protocol are implemented.

The local history contains an earlier Minecraft 1.21 implementation at `408d0c7` (`ItemFrameEntityMixin.java`, Fabric/Yarn names), plus a later reset-behavior change at `2d426fd`. Use these as references, not as a ready-made NeoForge port: the older implementation resets visibility through damage handling rather than the current item-change hook.

## Initial review findings and their resolution

- Java was absent from the original shell. The pinned Nix environment now runs OpenJDK 21.0.12.1 and Gradle 9.2.0.
- The original Allay snapshot could not resolve from Maven Local/Gradle Plugin Portal, and Allay/Trove GitHub URLs returned 404. The backport replaces that infrastructure with ModDevGradle 2.0.148 and vanilla/NeoForge APIs.
- The original project's [Modrinth version API](https://api.modrinth.com/v2/project/invisible-frames-mod/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D) returned no NeoForge 1.21.1 release during the review; [CurseForge's file listing](https://www.curseforge.com/minecraft/mc-mods/rounds-invisible-frames/files/all) showed its existing 1.21/1.21.1 release as Fabric/Quilt.
- The exact pack/mod list remains unavailable; the user approved 21.1.259 as the initial baseline.

## Chosen approach

Use the dedicated backport checkout with a standard ModDevGradle build based on the [NeoForge 1.21.1 MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle). This removes Allay/Trove from this branch. The original Fabric/Forge build and test harness are preserved in Git history; this branch supports NeoForge only.

Complete steps 1–7 for a usable modpack build. Publishing in steps 8–9 is postponed and is not part of the current implementation scope.

## Step-by-step implementation checklist

### 1. Establish the target and branch

- [x] Record the user-approved initial baseline: NeoForge `21.1.259` and Java 21. Actual pack files and relevant interaction-mod list remain a chapter 7 prerequisite.
- [x] Implement in the dedicated `t3/neoforge-port-roadmap` branch/checkout; the newer Minecraft line remains available on `main`.
- [x] Start with NeoForge only on this branch. Adjust `settings.gradle.kts` so unrelated Fabric/Forge projects do not require their plugins or dependencies during configuration. Building just `:neoforge:build` does not automatically avoid configuring other included projects.
- [x] Keep `invisibleframes` as the mod ID unless there is a concrete reason to change it. Document that the fork and original must not be installed together.

Done when: the branch's target and supported installation are explicit.

### 2. Make the build reproducible on a clean machine

- [x] Provide a pinned 64-bit JDK 21 development environment through `flake.nix` and `flake.lock`. Nix is already installed locally; enter with `nix develop`.
- [x] Set the Gradle Java toolchain to 21 when replacing/backporting the build. This is the documented [NeoForge 1.21.1 prerequisite](https://docs.neoforged.net/docs/1.21.1/gettingstarted/).
- [x] Resolve the build choice before porting the mixin: either prove compatible Allay/Trove artifacts resolve without a developer's Maven cache, or replace their configuration with the standard MDK. Do not simply change `minecraft_version` and keep the `+26.3` Trove binaries.
- [x] For the recommended MDK route, configure `net.neoforged.moddev`, explicitly include `common/src/main/java` and `common/src/main/resources` in the NeoForge project, and replace `libBundle` and Allay-specific DSL/task usage.
- [x] Remove Trove bootstrap/imports and bundled dependencies if taking the MDK route. The current feature uses vanilla entity state; identify any actual remaining dependency before retaining it. Rewrite its test integrations in step 5.
- [x] Pin Minecraft `1.21.1`, our chosen NeoForge `21.1.x`, and the build plugin. Remove `latest`, local-only resolution requirements, and the snapshot plugin dependency from the supported build path.
- [x] Select a Gradle wrapper supported by the chosen plugin/MDK, rather than assuming the current wrapper must be downgraded. Reassess the current 8 GB Gradle heap and parallelism for a CI runner.
- [x] Run `./gradlew :neoforge:tasks --all` and `./gradlew :neoforge:build --no-daemon` from a fresh checkout with an empty task-specific `GRADLE_USER_HOME`; record the resulting commands and artifact path.

Done when: a clean checkout builds without separately publishing Allay/Trove to `mavenLocal()`.

### 3. Define metadata and register the mixin

- [x] Add explicit `META-INF/neoforge.mods.toml` metadata, or verify equivalent generated output if retaining Allay. Match the mod ID to the existing `@Mod` entrypoint.
- [x] Declare Minecraft compatibility as `[1.21.1]` initially, the supported NeoForge range, Java 21, MIT license, authors/credits, and the fork's source and issue URLs. Do not claim compatibility with other Minecraft versions before testing.
- [x] Add an `invisibleframes.mixins.json` with the common mixin, Java 21 compatibility, and required injection matching. Register it through `[[mixins]]` in the TOML; check packaging/remapping requirements for the chosen tooling. See [NeoForge mod-file documentation](https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/).
- [x] Package `LICENSE` and the referenced logo. Inspect the final JAR for metadata, mixin configuration, compiled classes, and resources, and ensure test classes are excluded.

Status: metadata/packaging and dedicated-server mixin loading verified. Interactive client loading/rendering remains part of chapter 7.

### 4. Backport the frame behavior

- [x] Inspect the generated 1.21.1 Minecraft sources using the chosen mappings. Compare them with both the current mixin and historical `408d0c7`/`2d426fd` implementations.
- [x] Adapt the `interact` injector to the actual 1.21.1 signature. The current handler includes `Vec3 location`; it must not be copied unchanged without checking the target descriptor.
- [x] Verify the `playSound` call owner, descriptor, and which interaction branch reaches it. Keep normal insertion/rotation behavior while allowing crouch-interact to toggle a filled frame once without rotating it or consuming an item.
- [x] Check the sneaking predicate, return value, and logical client/server behavior against 1.21.1. Make the server authoritative and verify that prediction or an offhand interaction does not toggle twice.
- [x] Rework the reset injection against the actual 1.21.1 item-change path. Do not assume the newer `onItemChanged(ItemStack)` call exists. Confirm that removing the displayed item restores visibility.
- [x] Check the inherited `HangingEntity` constructor if retaining the current mixin superclass; use a simpler target cast if that avoids unnecessary version coupling.
- [x] Preserve vanilla invisibility state and synchronization where possible. Verify glow item frames through their inherited behavior instead of assuming they are covered.

Done when: both frame types toggle correctly and empty frames become visible, with no failed injection warnings.

### 5. Add meaningful NeoForge tests

- [x] Replace the Allay/Trove test annotations and harness with native NeoForge GameTests if removing those libraries. Configure test sources, registration, structures, and a `gameTestServer` run for the `invisibleframes` namespace.
- [x] Add server GameTests exercising the real interaction path: visible → invisible → visible, no rotation during toggling, normal rotation without sneaking, normal insertion into an empty frame, and visibility reset after removing the item. Cover regular and glow frames.
- [x] Check main-hand/offhand behavior and held-item counts so tests catch duplicate interactions or accidental consumption. Ensure tests actually load the mod and its mixin rather than merely calling a helper that flips a boolean.
- [x] Preserve a dedicated-server startup check with no client-only class loading. Ensure CI discovers a nonzero number of required tests and fails when a required test fails.
- [x] Replace the obsolete Allay/Trove tests with native tests. Real-client rendering remains an explicit manual check; protocol/server tests do not replace visual verification.
- [ ] Verify save/reload persistence and synchronization to a second connected client, including one joining after the toggle. Test server-only installation with an unmodified client before advertising it; upstream metadata alone is insufficient evidence for the port.

The standard automated task is `runGameTestServer` (likely `:neoforge:runGameTestServer` with this layout). Confirm the generated name in step 2. [NeoForge documents the runner and failure exit behavior](https://docs.neoforged.net/docs/1.21.1/misc/gametest/). The existing Fabric test commands are not substitutes.

Done when: automated behavior tests pass and multiplayer/persistence checks are recorded.

### 6. Add GitHub Actions build verification

Yes, this is practical once step 2 works on a clean machine. Implement `.github/workflows/build.yml` using GitHub's [Gradle workflow guidance](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle) and [Gradle's maintained actions](https://github.com/gradle/actions).

- [x] Trigger builds on `push`, `pull_request`, and `workflow_dispatch` for the maintained branch.
- [x] Use an Ubuntu runner, source checkout, Temurin JDK 21, and `gradle/actions/setup-gradle` for wrapper validation and caching. Pin selected action revisions and enable dependency updates.
- [x] Run `./gradlew :neoforge:build --no-daemon --stacktrace`, then the verified NeoForge GameTest task as a separate step or job. `build` alone does not establish that these game tests ran.
- [x] Accept the Minecraft EULA explicitly for disposable game-test worlds, document that setting, set a job timeout, and upload logs/test reports on failure.
- [x] Upload the distributable NeoForge JAR as a workflow artifact. Select the production artifact explicitly so source/test/dev JARs cannot be mistaken for the mod.
- [x] Keep build CI at `contents: read` with no publishing tokens, suitable for fork pull requests. Hosted execution remains unchecked below.
- [x] Keep build and server-test CI independent of graphical client tests. A graphical test job is deferred; see the remaining visual checks in chapter 7.
- [ ] Verify one successful pull-request run and one intentionally failing required GameTest before making CI a branch-protection requirement.

Done when: pull requests produce a downloadable JAR and report compilation or behavior failures automatically.

### 7. Validate in our actual modpack

- [ ] Install the packaged JAR in a copy of the pack, using the exact NeoForge version from step 1, and start both the client and dedicated server.
- [ ] Repeat frame toggling, rotation, insertion, removal, save/reload, and two-player checks in survival and creative. Include wall, floor, and ceiling frames, maps, and glow frames.
- [ ] Check interaction/protection mods and any mods changing item frames for conflicts. Confirm protected interactions remain protected.
- [x] Record installation sides and real dependencies. Update README build/run/test instructions and add a changelog for the 1.21.1 backport.

Done when: the actual pack works with the packaged artifact, not just development runs.

### 8. Prepare optional CurseForge/Modrinth hosting (postponed)

Both platforms can host NeoForge JARs. Decide whether this goes upstream or becomes a separately maintained fork before configuring uploads.

- [ ] If contributing upstream, arrange for the original project's maintainers to publish, or obtain project-team upload access. A GitHub fork does not grant rights to upload into their hosting accounts.
- [ ] If publishing our fork, create new project IDs/slugs and replace the existing publishing destinations. Credit Roundaround, link the original project, and explain the added NeoForge 1.21.1 support. Update fork support/download links and badges.
- [ ] Retain the repository's MIT copyright/license notice in source and JAR. The original CurseForge listing displayed **All Rights Reserved**, while this checkout and Modrinth identify MIT; document the source revision/license used for the port and clarify any hosting-license mismatch before submitting. Review branding/assets separately from code attribution.
- [ ] Prepare a distinct project name/icon, a clear description, source/issues links, installation-side information from step 5, and an in-game screenshot. Do not reuse `DESCRIPTION.md` unchanged: it currently describes and links the original author's project.
- [ ] On CurseForge, create a Minecraft Mods project, choose the appropriate license/category, and submit the tested JAR tagged `1.21.1` and `NeoForge`. Expect project/file moderation. See [submission instructions](https://support.curseforge.com/support/solutions/articles/9000197241-creating-and-submitting-a-project) and [fork/modpack policies](https://support.curseforge.com/support/solutions/articles/9000197279).
- [ ] On Modrinth, create the project and upload the same tested JAR with `game_versions: ["1.21.1"]` and `loaders: ["neoforge"]`, correct sides, changelog, and actual dependency relations. Its [content rules](https://modrinth.com/legal/rules) require attribution and define eligible forks as substantially diverged; whether this backport qualifies is a moderation decision, so acceptance is not guaranteed.
- [ ] Add the approved hosted version to the pack using that platform's file/version reference. For CurseForge packs, a GitHub-hosted JAR alone is insufficient unless it qualifies through their approved non-CurseForge mod process. For Modrinth, check the [modpack permissions guidance](https://support.modrinth.com/en/articles/8797527-obtaining-modpack-permissions) if using externally hosted files.

Done when: a hosted version can be installed by the pack launcher, or we have chosen the fallback below.

### 9. Automate releases only after the first working release (postponed)

- [ ] Add a separate release workflow triggered by a deliberate version tag or manual dispatch. Run build/tests first and publish the exact verified artifact, with a version matching its metadata.
- [ ] Use either verified Allay publish tasks if retained, or a maintained publishing integration/the official [Modrinth version API](https://docs.modrinth.com/api/operations/createversion/) and [CurseForge Upload API](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api). Confirm destinations and task names rather than assuming the current DSL is usable.
- [ ] Store dedicated Modrinth and CurseForge upload credentials in GitHub secrets for the release environment. Give GitHub release jobs `contents: write` only if creating a GitHub Release.
- [ ] Publish only the intended production JAR, changelog, exact Minecraft/loader tags, and real dependency metadata. Do not carry the Fabric API requirement onto the NeoForge release.
- [ ] Support rerunning a partial release without duplicating versions already uploaded successfully. Record both platform version/file IDs and resulting download URLs.

Fallback: publish a versioned JAR and checksums on GitHub Releases, keep CI artifacts for testing, and install it manually in the private pack/server. Public pack distribution still needs to follow the destination platform's external-file rules; platform hosting is not required to finish the port itself.

## Remaining external validation

- Supply the actual modpack/mod list to validate integration/protection mods and pack-specific behavior.
- Verify rendering in real Minecraft clients, including a late join and chunk/world reload.
- Run the committed workflow on GitHub before relying on a branch-protection requirement.
- Revisit public publishing only when requested.
