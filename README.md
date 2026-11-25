<div align="center">

# ⚔️ PaperArtifacts ⚔️

**45+ Magical Accessories, Animated Mimics & Exploration Content for CuriosPaper & Synthetix**

[![PaperArtifacts Version 2.0](https://img.shields.io/badge/version-2.0-blueviolet?style=for-the-badge)](https://discord.gg/r5YXqgrGya)
[![Paper / Folia 1.20.6 to 1.21+](https://img.shields.io/badge/Paper%20%7C%20Folia-1.20.6%20--%201.21+-green?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0id2hpdGUiIGQ9Ik0xMiAyQzYuNSAyIDIgNi41IDIgMTJzNC41IDEwIDEwLTQuNSAxMC0xMFMxNy41IDIgMTIgMnoiLz48L3N2Zz4=)](https://papermc.io/)
[![Java 21 LTS](https://img.shields.io/badge/java-21%20LTS-orange?style=for-the-badge)](https://www.oracle.com/java/)
[![Discord Community](https://img.shields.io/discord/1456137607569346739?label=Discord&style=for-the-badge&logo=discord&logoColor=white&color=5865F2)](https://discord.gg/r5YXqgrGya)
[![bStats Live Metrics](https://img.shields.io/badge/bStats-live-blue?style=for-the-badge)](https://bstats.org/plugin/bukkit/Artifacts/30728)
[![Support on Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Me-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/brothergaming52)

**Hats · Necklaces · Belts · Gloves · Boots · Animated Mimics · Underground Campsites**
<br>
Pure modern Paper & Folia API. Native multi-threaded regional ticking. Zero client mods required.

---

*A server-side port and spiritual continuation of the beloved **Artifacts** mod by ochotonida, built specifically for modern Paper and Folia 1.20.6+ servers. Powered by [CuriosPaper](https://discord.gg/r5YXqgrGya) for accessory slots and [Synthetix](https://discord.gg/r5YXqgrGya) for animated virtual entity mechanics and automated resource pack deployment.*

</div>

> [!IMPORTANT]
> **Ecosystem Notice:** PaperArtifacts 2.0 requires both **[CuriosPaper 3.0](https://discord.gg/r5YXqgrGya)** (for accessory slots and packet 3D models) and **[Synthetix](https://discord.gg/r5YXqgrGya)** (for the Tri-Layer animated Mimic entity and resource pack pipeline). Because Synthetix is currently pending approval on Modrinth, please join our **[Discord Community](https://discord.gg/r5YXqgrGya)** to download the latest releases!

---

## 🧩 What is PaperArtifacts?

PaperArtifacts brings rich RPG exploration, treasure hunting, and magical accessories to Paper, Purpur, and Folia servers without requiring any client-side mods.

Players discover over **45+ unique magical artifacts** with authentic custom textures and body-mounted 3D models across dungeon loot chests, themed mob drops, and subterranean **abandoned campsites**. Every campsite is a hidden time capsule buried in underground caves—packed with furniture, workbenches, and treasure chests. But beware: some chests are secretly **Mimics**, terrifying animated monsters waiting to ambush unsuspecting adventurers!

<div align="center">

![Full chest inventory showcasing all 45+ unique artifact items](https://cdn.modrinth.com/data/w0FXMugd/images/3fc404ab62e3e6ce238fddb17bb8127e5342c903_350.webp)

*Over 45+ discoverable magical artifacts, each with custom textures and dedicated abilities*

</div>

---

## ⚡ Features Overview

| | Feature | Description |
|:---:|---|---|
| ⚔️ | **45+ Magical Artifacts** | Authentic accessories across Head, Necklace, Belt, Hands, Feet, and Held slots with game-changing passive and active mechanics. |
| 🐉 | **Synthetix Mimic Engine** | Tri-Layer virtual entity rig featuring physical Slime driver physics, zero-ticking ItemDisplay bone hierarchies, GPU SLERP interpolation, and 2.0x mouth weakpoint hitboxes. |
| 🎭 | **Packet 3D Wearables** | Body-mounted 3D accessory models rendered via CuriosPaper 3.0 clientbound packets—zero persistent server armor stand entities or chunk bloat. |
| ⚡ | **Folia Multi-Threading** | Native support for Folia multi-threaded regional ticking via universal `PlatformScheduler`. Entity, campsite generation, and ability tasks run authoritatively on their owning regional threads. |
| 🏕️ | **Underground Campsites** | Naturally generating subterranean campsites with functional furniture, cooking stations, trapped TNT chests, and valuable loot. |
| 🗺️ | **World Loot & Mob Drops** | Automatic injection into vanilla dungeon chests, jungle temples, bastions, and end cities, plus themed mob drop tables (e.g. Drowned dropping Flippers). |
| 📦 | **Zero-Port Resource Pack** | Bundled textures, models, and audio automatically hook into Synthetix's core resource pack pipeline and downstream bridges (Nexo, ItemsAdder, Oraxen, CraftEngine). |
| 📱 | **Geyser & Bedrock Support** | Automatic deployment of `Artifacts_Geyser.zip` and custom item mappings into GeyserMC, ensuring Bedrock players see authentic textures and GUI slot icons. |
| ⚙️ | **Deep Configuration** | Fully customize every item's display name, material, CustomModelData, item-model key, lore, potion effects, and attribute modifiers in `artifacts.yml`. |

---

## 🎒 The Artifacts Showcase

Artifacts are categorized into six dedicated equipment groups, each granting unique gameplay abilities. Wearable items attach seamlessly to CuriosPaper accessory slots, including the dynamically registered **Feet slot**:

<div align="center">

![Player wearing the Crystal Heart accessory 3D model](https://cdn.modrinth.com/data/w0FXMugd/images/c9f2e58d0b925616091e6898a239810b0248c231_350.webp)
&nbsp;&nbsp;&nbsp;&nbsp;
![Player wearing the Helium Flamingo accessory 3D model](https://cdn.modrinth.com/data/w0FXMugd/images/35ace486ab2a0d43b5ac6914f19cc3c133a29333_350.webp)

*In-game packet 3D body models: Crystal Heart (left) and Helium Flamingo (right)*

</div>

### 🎩 Head Slot

| Item | Key Mechanics |
|---|---|
| **Snorkel** | Provides sustained underwater breathing and prevents drowning damage |
| **Villager Hat** | Drastically improves villager trading prices and reputation |
| **Superstitious Hat** | Grants +1 Looting tier to all mob kills |
| **Cowboy Hat** | Enhances mount movement speed and riding control |
| **Plastic Drinking Hat** | Speeds up the consumption of food, potions, and drinks |
| **Novelty Drinking Hat** | Greatly accelerates food and drink consumption speed |
| **Night Vision Goggles** | Grants permanent, clear vision in dark caverns and oceans |
| **Angler's Hat** | Boosts fishing speed, luck of the sea, and rare catches |

### 📿 Necklace Slot

| Item | Key Mechanics |
|---|---|
| **Lucky Scarf** | Grants +1 Fortune level when mining blocks |
| **Cross Necklace** | Extends the post-damage invulnerability grace period |
| **Panic Necklace** | Grants a temporary burst of Speed when taking damage |
| **Shock Pendant** | Chance to strike attackers with retaliatory lightning bolts |
| **Flame Pendant** | Chance to ignite attacking enemies in combat |
| **Scarf of Invisibility** | Cloaks the player in complete invisibility while worn |
| **Thorn Pendant** | Reflects a portion of received damage directly back at attackers |
| **Charm of Sinking** | Allows free, unimpeded walking along underwater ocean floors |
| **Charm of Shrinking** | Shrinks player scale for traversing sub-block passages *(1.20.5+)* |

### 🔋 Belt Slot

| Item | Key Mechanics |
|---|---|
| **Cloud in a Bottle** | Grants an aerial mid-air double jump with authentic cloud burst particles |
| **Obsidian Skull** | Temporarily grants Fire Resistance whenever fire or lava damage is taken |
| **Antidote Vessel** | Drastically reduces the duration of negative status ailments and poisons |
| **Universal Attractor** | Magnetic field pulls nearby dropped items directly into your inventory |
| **Crystal Heart** | Permanently increases maximum health by +5 extra hearts |
| **Helium Flamingo** | Allows swimming freely through the air with flight stamina mechanics |
| **Chorus Totem** | Teleports you to a safe location upon taking lethal damage *(single use)* |
| **Warp Drive** | Consumes hunger points instead of Ender Pearls when pearl teleporting |

### 🧤 Hands Slot

| Item | Key Mechanics |
|---|---|
| **Digging Claws** | Mine stone-tier blocks barehanded with built-in Haste |
| **Feral Claws** | Significantly increases melee attack speed |
| **Power Glove** | Boosts raw melee attack damage |
| **Fire Gauntlet** | Ignites melee targets upon physical contact |
| **Pocket Piston** | Increases physical knockback dealt to enemies |
| **Vampiric Glove** | Drains life from enemies to heal the player on hit |
| **Golden Hook** | Multiplies experience orbs earned from slain mobs |
| **Onion Ring** | Grants a temporary burst of Haste after eating food |
| **Pickaxe Heater** | Automatically smelts mined iron, gold, and ancient debris ores |
| **Withered Bracelet** | Inflicts lethal Wither on enemies struck in combat |

### 👟 Feet Slot

| Item | Key Mechanics |
|---|---|
| **Aqua-Dashers** | Sprint directly across the surface of water and oceans |
| **Bunny Hoppers** | Grants Jump Boost and provides complete fall damage immunity |
| **Kitty Slippers** | Creepers fear you and flee in terror |
| **Running Shoes** | Increases sprinting speed and step height over full blocks |
| **Snowshoes** | Walk across Powder Snow without sinking; negates ice slipperiness |
| **Steadfast Spikes** | Grants total immunity to incoming knockback effects |
| **Flippers** | Grants Dolphin's Grace swimming speed in aquatic environments |
| **Rooted Boots** | Slowly replenishes hunger and saturation while standing on natural grass |
| **Strider Shoes** | Walk across molten lava oceans while sneaking |

### 🍖 Held Items

| Item | Key Mechanics |
|---|---|
| **Umbrella** | Slows downward fall while held and doubles as an emergency shield |
| **Everlasting Beef** | Infinite raw beef supply with a short reuse cooldown |
| **Eternal Steak** | Infinite cooked steak supply *(obtained by cooking Everlasting Beef)* |

---

## 🏕️ Campsites & The Mimic

### Underground Campsites
While exploring deep cave systems, players will stumble upon **abandoned campsites** generated directly into the world terrain. Campsites feature survival shelters complete with colored tents, crafting benches, lit campfires, brewing stands, and storage barrels containing rare supplies. Some campsites also conceal trapped chests with explosive TNT surprises!

<div align="center">

![Naturally generated underground campsite with tents, furnaces, and loot chests](https://cdn.modrinth.com/data/w0FXMugd/images/9c730693f9f96aa605a03521213d4bfa74860b80_350.webp)

*A subterranean campsite naturally spawned within an underground cave chamber*

</div>

### The Mimic: Synthetix Virtual Entity
Not every treasure chest is safe. Certain campsite chests are **Mimics** in disguise. The moment a player right-clicks or breaks a mimic chest, it wakes up and transforms into an aggressive predator powered by **Synthetix's Tri-Layer Virtual Entity Architecture**:

- **Physical Driver Layer:** Driven by a living mob proxy with authoritative server-side pathfinding, 60 HP (30 hearts), and customized leap attack velocity.
- **Visual Skeletal Rig:** Dual-bone hierarchy (`root` lower chest and `lid` pivoting on the back hinge) rendered with virtual `ItemDisplay` entities using client-side 60 FPS GPU SLERP matrix hardware interpolation.
- **Sub-Voxel Hitbox Matrix:** Features anatomical damage multipliers—strike the body for 1.0x damage, or time your hit as the lid snaps open to strike the mouth weakpoint for **2.0x critical damage**!
- **Animation Tracks:** 5 procedural timeline tracks including idle breathing, hopping locomotion, snapping leap attacks, hurt flinches, and death collapses.
- **Guaranteed Rewards:** Slaying a mimic guarantees an authentic artifact drop from your configured `mimic.yml` drop pool.

<div align="center">

![Aggressive animated mimic chest attacking the player](https://cdn.modrinth.com/data/w0FXMugd/images/d21c8f4b61bfe7c6699e021d8b1bd4afc0de5e79_350.webp)

*A awakened mimic snapping its hinged lid and leaping at the adventurer*

</div>

---

## 🗺️ How to Discover Artifacts

Artifacts cannot be crafted in crafting tables—they must be earned through brave exploration:

| Method | Where to Look |
|---|---|
| 🗺️ **Dungeon Loot Chests** | Naturally generated in vanilla dungeons, mineshafts, jungle temples, desert pyramids, nether fortresses, bastions, and end cities. |
| 🧟 **Themed Mob Drops** | Defeat specific mobs with configured chances (e.g. Drowned drop Flippers, Wither Skeletons drop Withered Bracelets, Elder Guardians drop Charms). |
| 🏕️ **Campsite Exploration** | Scour underground cave networks for abandoned campsites containing loot chests. |
| 👹 **Mimic Encounters** | Locate and defeat hostile chest mimics in campsites for guaranteed artifact rewards. |
| 📋 **Admin Commands** | Administrators can spawn specific items via `/curios give <player> <item>` or test mimics with `/artifacts mimic`. |

---

## ⚡ What's New in v2.0

- 🧩 **CuriosPaper 3.0 Native Integration** — Fully migrated to CuriosPaper 3.0's dynamic slot architecture, including runtime registration of the dedicated **Feet slot** (`curiospaper.slot.feet`) and clientbound packet 3D body models.
- 🐉 **Synthetix Tri-Layer Mimic Rigging** — Complete overhaul of the Mimic entity into a modular Synthetix rig with dual-bone forward kinematics, GPU SLERP interpolation, procedural animation tracks, and 2.0x mouth weakpoint hitboxes.
- ⚡ **Full Folia Multi-Threading** — Integrated universal `PlatformScheduler` routing campsite world generation, mimic entity AI, and ability loops to their authoritative regional threads with zero cross-region desyncs.
- 📦 **Zero-Port Resource Pack Pipeline** — Bundled assets automatically extract and register with Synthetix's core resource pack host, bridging seamlessly to Nexo, ItemsAdder, Oraxen, and CraftEngine.
- 📱 **Automated Geyser & Bedrock Setup** — Automatically extracts and synchronizes `Artifacts_Geyser.zip` and `Artifacts_mappings.json` directly into Geyser folders on startup.
- 🛠️ **Modern Data Components (1.20.6 / 1.21+)** — Native support for `item_model` identifiers and modern Paper component attributes.
- ⚙️ **Custom Abilities & Per-Item Tuning** — Configure potion effects, attribute modifiers, triggers (`WHILE_EQUIPPED`, `EQUIP`, `DE_EQUIP`), and drop tables via `artifacts.yml` and `config.yml`.

---

## 🔧 Requirements & Compatibility

| | Requirements |
|---|---|
| **Minecraft** | 1.20.6 — 1.21+ |
| **Server Engine** | Paper, Purpur, Folia *(Experimental Support)* |
| **Java** | 21 LTS+ |
| **Required Dependencies** | [CuriosPaper 3.0](https://discord.gg/r5YXqgrGya) & [Synthetix](https://discord.gg/r5YXqgrGya) |
| **External Pack Bridges** | Nexo, ItemsAdder, Oraxen, CraftEngine, or built-in host |
| **Bedrock Support** | GeyserMC & Floodgate (with auto-deployed mappings & packs) |

> [!WARNING]
> **Experimental Folia Support:** Folia regional multi-threading support is experimental—many errors, desyncs, and unexpected bugs can happen when running on Folia servers.

---

## 📊 Live Statistics

<p align="center">
  <a href="https://bstats.org/plugin/bukkit/Artifacts/30728">
    <img src="https://bstats.org/signatures/bukkit/Paper%20Artifacts.svg" alt="PaperArtifacts bStats Live Metrics Graph" width="500">
  </a>
</p>

---

## 💬 Community & Support

<div align="center">

[![Join the Discord Community](https://img.shields.io/discord/1456137607569346739?label=Join%20the%20Discord&style=for-the-badge&logo=discord&logoColor=white&color=5865F2)](https://discord.gg/r5YXqgrGya)

**[discord.gg/r5YXqgrGya](https://discord.gg/r5YXqgrGya)**

Get builds · Report bugs · Suggest features · Community addons

<br>

[![Buy Me a Coffee on Ko-fi](https://img.shields.io/badge/Ko--fi-Buy%20me%20a%20coffee-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/brothergaming52)

**[ko-fi.com/brothergaming52](https://ko-fi.com/brothergaming52)**

</div>

---

<details>
<summary>📋 <b>Click to Expand: Commands & Permissions Reference</b></summary>

### Commands

| Command | Permission | Description |
|---|---|---|
| `/artifacts list` | `artifacts.admin` | Displays a list of all registered artifact items and internal IDs |
| `/artifacts mimic` | `artifacts.admin` | Spawns a live animated mimic chest at your current location |
| `/artifacts reload` | `artifacts.admin` | Hot reloads configs, `artifacts.yml`, campsites, and drop tables |
| `/surprise-me` | `artifacts.surprise` | Spawns a mysterious surprise mimic encounter |
| `/curios give <player> <item>` | `curiospaper.admin` | Gives a registered artifact directly to a player's inventory |

### Permissions

| Permission | Default | Description |
|---|---|---|
| `artifacts.admin` | `op` | Full access to administrative artifact commands, mimic spawns, and reload |
| `artifacts.surprise` | `op` | Allows invoking the `/surprise-me` mimic encounter command |
| `curiospaper.slot.feet` | `true` | Allows equipping accessories into the custom Feet slot |

</details>

<details>
<summary>⚙️ <b>Click to Expand: Configuration Showcase (`artifacts.yml`, `config.yml`, `mimic.yml`)</b></summary>

### 1. Item Definition & Custom Abilities (`/plugins/Artifacts/artifacts.yml`)

```yaml
# Customize display name, material, CMD, item-model key, curio slot, and abilities
crystal_heart:
  name: "<red><bold>Crystal Heart</bold></red>"
  material: "NETHERITE_UPGRADE_SMITHING_TEMPLATE"
  custom-model-data: 201033
  item-model: "crystal_heart"
  slot-type: "belt"
  lore:
    - "<gray>A crystalline relic radiating vibrant vitality.</gray>"
    - "<dark_gray>+5 Max Hearts while worn on your belt.</dark_gray>"
  has-3d-model: true
  abilities:
    bonus_health:
      trigger: WHILE_EQUIPPED
      effect-type: PLAYER_MODIFIER
      effect-name: GENERIC_MAX_HEALTH
      amplifier: 1000 # +10.0 health (+5 hearts)
      operation: ADD_NUMBER

cloud_in_a_bottle:
  name: "<aqua><bold>Cloud in a Bottle</bold></aqua>"
  material: "GLASS_BOTTLE"
  custom-model-data: 201029
  item-model: "cloud_in_a_bottle"
  slot-type: "belt"
  lore:
    - "<gray>A bottled cloud that propels you into the air.</gray>"
    - "<dark_gray>Grants double-jump ability.</dark_gray>"
  has-3d-model: true
```

### 2. General Engine Config (`/plugins/Artifacts/config.yml`)

```yaml
version: "2.0"

updates:
  check-for-updates: true

effects:
  enable-particles: true
  enable-sounds: true

items:
  umbrella:
    enabled: true
    slow-falling-amplifier: 0
  everlasting_beef:
    enabled: true
    cooldown-seconds: 15
  snorkel:
    enabled: true
    oxygen-duration-seconds: 15
```

### 3. Mimic Drop Pool (`/plugins/Artifacts/mimic.yml`)

```yaml
enabled-drops:
  - umbrella
  - everlasting_beef
  - eternal_steak
  - crystal_heart
  - cloud_in_a_bottle
  - helium_flamingo
  - power_glove
  - aqua_dashers
  - bunny_hoppers
```

</details>

<details>
<summary>🙏 <b>Click to Expand: Credits & Acknowledgments</b></summary>

PaperArtifacts is inspired by and based on the original [**Artifacts**](https://modrinth.com/mod/artifacts) mod created by [**ochotonida**](https://github.com/ochotonida/artifacts) for Forge and Fabric. This plugin is a standalone, server-side Paper/Folia implementation utilizing the [CuriosPaper](https://discord.gg/r5YXqgrGya) accessory inventory and [Synthetix](https://discord.gg/r5YXqgrGya) core engines.

All original designs, textures, and concepts belong to their respective creators.

</details>

---

<div align="center">

**Made with ❤️ by Brothergaming52**

[CuriosPaper](https://discord.gg/r5YXqgrGya) · [Synthetix](https://discord.gg/r5YXqgrGya) · [Discord](https://discord.gg/r5YXqgrGya) · [Ko-fi](https://ko-fi.com/brothergaming52) · [bStats](https://bstats.org/plugin/bukkit/Artifacts/30728)

</div>
