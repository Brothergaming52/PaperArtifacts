# Changelogs

## v2.0

**Release Date:** 2026-11-25

### 🧩 CuriosPaper 3.0 & Synthetix Integration

PaperArtifacts 2.0 migrates its item, recipe, entity, and resource pack pipelines onto the **Synthetix** core engine and **CuriosPaper 3.0**:

- **CuriosPaper 3.0 Slot Registration:** Updated `Artifacts` to register the dedicated **Feet** slot (`curiospaper:feet_slot`) via CuriosPaper 3.0's `SlotDefinition` API with per-slot permission `curiospaper.slot.feet`.
- **Synthetix Registry Migration (`ItemRegistry`):** Migrated artifact item definitions, cooking/smelting recipes (`SynthetixRecipe`), mob drops (`MobDropRule`), and chest loot tables (`LootTableRule`) to Synthetix.
- **Zero-Port Resource Pack Pipeline (`ResourcePackExtractor`):** Bundled textures, models, and Mimic sounds are extracted from the plugin JAR and registered directly with `SynthetixAPI` (supporting Nexo, ItemsAdder, Oraxen, CraftEngine, or Synthetix's built-in host).
- **Drop Filtering (`ArtifactDropFilterListener`):** Added an `EntityDeathEvent` filter preventing disabled artifacts from dropping when toggled off in `config.yml`.

### 🐉 Synthetix Tri-Layer Mimic Overhaul

- **Synthetix Virtual Entity Rig (`MimicEntityDefinition`):** Replaced legacy entity/model/AI classes (`MimicEntity`, `MimicAI`, `MimicAnimator`, `MimicModel`) with a modular Synthetix `CustomEntityDefinition` using a Slime driver base, virtual `ItemDisplay` skeletal bones with SLERP interpolation, sub-voxel interaction hitboxes, and procedural animation tracks.
- **Lifecycle & Audio (`MimicManager`):** Rebuilt mimic spawning, cleanup, and loot handling around `SynthetixEntitySpawnEvent` and `SynthetixEntityDeathEvent`, and bundled custom Mimic sound effects (`hurt1–3.ogg`, `death1–2.ogg`).

### ⚡ Folia Multi-Threading, Effects & Configuration

- **Universal Scheduler (`PlatformScheduler`):** Added `folia-supported: true` in `plugin.yml` and routed campsite world generation, mimic tasks, and ability listeners through `PlatformScheduler`. *(Note: Folia support is experimental—many errors and bugs can happen when running on Folia servers.)*
- **Modern Platform Baseline:** Updated project target to Paper API `1.20.6+` and Java `21 LTS`.
- **Configuration Updates (`config.yml`):** Bumped config version to `2.0`, added `effects.enable-particles` and `effects.enable-sounds`, added `cloud_in_a_bottle.fall-damage-immune`, added `chorus_totem.teleport-diameter` and `save-from-void`, and re-enabled `chorus_totem`, `warp_drive`, and `strider_shoes`.
- **Shutdown Cleanup:** Ensured `rootedBootsListener`, `striderShoesListener`, `CampsitePopulator`, `MimicManager`, and `PlatformScheduler` tasks shut down cleanly on disable.

---

## v1.3

**Release Date:** 2026-08-18

### ⚙️ Per-Artifact Configuration (`artifacts.yml`)

- **Configurable Artifacts (`ArtifactItemsConfig`):** Added `artifacts.yml` so server owners can customize display names, base materials, `CustomModelData`, `item_model`, slot assignments, lore, 3D model toggles, and extra abilities for every artifact.
- **Command & Config Improvements:** Expanded `/artifacts` management (`ArtifactsCommand`), updated `CampsiteConfig` and `MimicConfig`, and refined passive artifact listeners (`PassiveArtifactsListener`, `PassiveEffectListener`, `RunningShoesListener`).
- **Bedrock Asset Update:** Updated `Artifacts_Geyser.zip` and `Artifacts_mappings.json` for expanded Bedrock model/item mapping support.

---

## v1.2

**Release Date:** 2026-07-01

### 📱 Bedrock Integration & Gameplay Refinements

- **GeyserMC & Floodgate Support:** Bundled `Artifacts_Geyser.zip` and `Artifacts_mappings.json` with automatic extraction and copying into server Geyser folders.
- **Mechanics & Mimic Updates:** Improved `DoubleJumpListener`, `LuckyScarfListener`, and `MimicManager` spawning/behavior.

