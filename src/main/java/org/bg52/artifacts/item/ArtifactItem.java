package org.bg52.artifacts.item;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * All artifact items with their base materials, visual model data,
 * display names, curio slot types, and lore.
 *
 * Items 1-3 use their own base materials.
 * Items 4-17 use CARROT_ON_A_STICK as their base.
 *
 * CMD values are in the 201xxx range to avoid collisions with mimic models.
 */
public enum ArtifactItem {

        // ── Non-curio items (own base materials) ──

        UMBRELLA(
                        "umbrella",
                        Material.SHIELD,
                        201010, "umbrella",
                        "\u00a7bUmbrella",
                        null, // Not a curio
                        Arrays.asList("\u00a77Slows your fall when held", "\u00a77Can be used as a shield")),

        EVERLASTING_BEEF(
                        "everlasting_beef",
                        Material.valueOf("BEEF"), // Raw Beef
                        201011, "everlasting_beef",
                        "\u00a7bEverlasting Beef",
                        null,
                        Arrays.asList("\u00a77Not consumed when eaten")),

        ETERNAL_STEAK(
                        "eternal_steak",
                        Material.COOKED_BEEF,
                        201012, "eternal_steak",
                        "\u00a7bEternal Steak",
                        null,
                        Arrays.asList("\u00a77Not consumed when eaten")),

        // ── Curio items (CARROT_ON_A_STICK base) ──

        SNORKEL(
                        "snorkel",
                        Material.CARROT_ON_A_STICK,
                        201013, "snorkel",
                        "\u00a7bSnorkel",
                        "head",
                        Arrays.asList("\u00a77Allows the wearer to breathe underwater for a limited amount of time")),

        VILLAGER_HAT(
                        "villager_hat",
                        Material.CARROT_ON_A_STICK,
                        201014, "villager_hat",
                        "\u00a7bVillager Hat",
                        "head",
                        Arrays.asList("\u00a77Decreases the trading prices of villagers")),

        SUPERSTITIOUS_HAT(
                        "superstitious_hat",
                        Material.CARROT_ON_A_STICK,
                        201015, "superstitious_hat",
                        "\u00a7bSuperstitious Hat",
                        "head",
                        Arrays.asList("\u00a77Applies an extra level of looting to killed entities")),

        COWBOY_HAT(
                        "cowboy_hat",
                        Material.CARROT_ON_A_STICK,
                        201016, "cowboy_hat",
                        "\u00a7bCowboy Hat",
                        "head",
                        Arrays.asList("\u00a77Increases the speed of ridden mounts")),

        LUCKY_SCARF(
                        "lucky_scarf",
                        Material.CARROT_ON_A_STICK,
                        201017, "lucky_scarf",
                        "\u00a7bLucky Scarf",
                        "necklace",
                        Arrays.asList("\u00a77Applies an extra level of fortune to mined blocks"), true, null, 15f),

        CROSS_NECKLACE(
                        "cross_necklace",
                        Material.CARROT_ON_A_STICK,
                        201018, "cross_necklace",
                        "\u00a7bCross Necklace",
                        "necklace",
                        Arrays.asList("\u00a77Increases the length of invincibility after taking damage"), true, null,
                        30f),

        PANIC_NECKLACE(
                        "panic_necklace",
                        Material.CARROT_ON_A_STICK,
                        201019, "panic_necklace",
                        "\u00a7bPanic Necklace",
                        "necklace",
                        Arrays.asList("\u00a77Increases the wearer's movement speed after taking damage"), true, null,
                        30f),

        SHOCK_PENDANT(
                        "shock_pendant",
                        Material.CARROT_ON_A_STICK,
                        201020, "shock_pendant",
                        "\u00a7bShock Pendant",
                        "necklace",
                        Arrays.asList("\u00a77Has a chance to strike attackers with lightning",
                                        "\u00a77Grants protection against lightning strikes"),
                        true),

        FLAME_PENDANT(
                        "flame_pendant",
                        Material.CARROT_ON_A_STICK,
                        201021, "flame_pendant",
                        "\u00a7bFlame Pendant",
                        "necklace",
                        Arrays.asList("\u00a77Has a chance to light attackers on fire",
                                        "\u00a77Grants fire resistance after lighting an attacker on fire"),
                        true),

        PLASTIC_DRINKING_HAT(
                        "plastic_drinking_hat",
                        Material.CARROT_ON_A_STICK,
                        201022, "plastic_drinking_hat",
                        "\u00a7bPlastic Drinking Hat",
                        "head",
                        Arrays.asList("\u00a77Decreases the time it takes to drink items",
                                        "\u00a77Decreases the time it takes to eat items")),

        NOVELTY_DRINKING_HAT(
                        "novelty_drinking_hat",
                        Material.CARROT_ON_A_STICK,
                        201023, "novelty_drinking_hat",
                        "\u00a7dNovelty Drinking Hat",
                        "head",
                        Arrays.asList("\u00a77\u00a7o'Hey! I'm #1, and I let gravity do my drinking!'",
                                        "\u00a77Decreases the time it takes to drink items",
                                        "\u00a77Decreases the time it takes to eat items")),

        NIGHT_VISION_GOGGLES(
                        "night_vision_goggles",
                        Material.CARROT_ON_A_STICK,
                        201024, "night_vision_goggles",
                        "\u00a7bNight Vision Goggles",
                        "head",
                        Arrays.asList("\u00a77Allows the wearer to see in the dark")),

        ANGLERS_HAT(
                        "anglers_hat",
                        Material.CARROT_ON_A_STICK,
                        201025, "anglers_hat",
                        "\u00a7bAngler's Hat",
                        "head",
                        Arrays.asList("\u00a77Applies an extra level of Luck of the Sea when fishing",
                                        "\u00a77Applies an extra level of Lure when fishing")),

        SCARF_OF_INVISIBILITY(
                        "scarf_of_invisibility",
                        Material.CARROT_ON_A_STICK,
                        201026, "scarf_of_invisibility",
                        "\u00a7bScarf of Invisibility",
                        "necklace",
                        Arrays.asList("\u00a77Turns the wearer invisible"), true, null, 15f),

        THORN_PENDANT(
                        "thorn_pendant",
                        Material.CARROT_ON_A_STICK,
                        201027, "thorn_pendant",
                        "\u00a7bThorn Pendant",
                        "necklace",
                        Arrays.asList("\u00a77Has a chance to damage attackers"), true, null, 30f),

        CHARM_OF_SINKING(
                        "charm_of_sinking",
                        Material.CARROT_ON_A_STICK,
                        201028, "charm_of_sinking",
                        "\u00a7bCharm of Sinking",
                        "necklace",
                        Arrays.asList("\u00a77Allows the wearer to move freely in water"), true, null, 30f),

        CLOUD_IN_A_BOTTLE(
                        "cloud_in_a_bottle",
                        Material.CARROT_ON_A_STICK,
                        201029,
                        "cloud_in_a_bottle",
                        "\u00a7bCloud in a Bottle",
                        "belt",
                        Arrays.asList("\u00a77Allows the wearer to double jump"), true, null, 50f),

        OBSIDIAN_SKULL(
                        "obsidian_skull",
                        Material.CARROT_ON_A_STICK,
                        201030,
                        "obsidian_skull",
                        "\u00a7bObsidian Skull",
                        "belt",
                        Arrays.asList("\u00a77The wearer becomes temporarily immune to fire damage when hurt by fire"),
                        true, null, 50f),

        ANTIDOTE_VESSEL(
                        "antidote_vessel",
                        Material.CARROT_ON_A_STICK,
                        201031,
                        "antidote_vessel",
                        "\u00a7bAntidote Vessel",
                        "belt",
                        Arrays.asList("\u00a77Greatly reduces the duration of negative effects"), true, null, 50f),

        UNIVERSAL_ATTRACTOR(
                        "universal_attractor",
                        Material.CARROT_ON_A_STICK,
                        201032,
                        "universal_attractor",
                        "\u00a7bUniversal Attractor",
                        "belt",
                        Arrays.asList("\u00a77Attracts nearby items"), true, null, 50f),

        CRYSTAL_HEART(
                        "crystal_heart",
                        Material.CARROT_ON_A_STICK,
                        201033,
                        "crystal_heart",
                        "\u00a7bCrystal Heart",
                        "belt",
                        Arrays.asList("\u00a77Increases the wearer's maximum health"), true, null, 50f),

        HELIUM_FLAMINGO(
                        "helium_flamingo",
                        Material.CARROT_ON_A_STICK,
                        201034,
                        "helium_flamingo",
                        "\u00a7bHelium Flamingo",
                        "belt",
                        Arrays.asList("\u00a77Allows the wearer to swim in the air for a limited period of time",
                                        "\u00a77Press Left Control while in the air to start swimming"),
                        true, null, 30f),

        CHORUS_TOTEM(
                        "chorus_totem",
                        Material.TOTEM_OF_UNDYING,
                        201035,
                        "chorus_totem",
                        "\u00a7bChorus Totem",
                        "belt",
                        Arrays.asList("\u00a77A fatal hit teleports you somewhere else instead"), true, null, 50f),

        DIGGING_CLAWS(
                        "digging_claws",
                        Material.CARROT_ON_A_STICK,
                        201036,
                        "digging_claws",
                        "\u00a7bDigging Claws",
                        "hands",
                        Arrays.asList("\u00a77Increases the wearer's base mining level to stone",
                                        "\u00a77Increases the wearer's mining Speed")),

        FERAL_CLAWS(
                        "feral_claws",
                        Material.CARROT_ON_A_STICK,
                        201037,
                        "feral_claws",
                        "\u00a7bFeral Claws",
                        "hands",
                        Arrays.asList("\u00a77Increases the wearer's attack speed")),

        POWER_GLOVE(
                        "power_glove",
                        Material.CARROT_ON_A_STICK,
                        201038,
                        "power_glove",
                        "\u00a7bPower Glove",
                        "hands",
                        Arrays.asList("\u00a77Increases damage dealth by the wearer")),

        FIRE_GAUNTLET(
                        "fire_gauntlet",
                        Material.CARROT_ON_A_STICK,
                        201039,
                        "fire_gauntlet",
                        "\u00a7bFire Gauntlet",
                        "hands",
                        Arrays.asList("\u00a77Causes the wearer's melee attacks to deal fire damage")),

        POCKET_PISTON(
                        "pocket_piston",
                        Material.CARROT_ON_A_STICK,
                        201040,
                        "pocket_piston",
                        "\u00a7bPocket Piston",
                        "hands",
                        Arrays.asList("\u00a77Increases knockback dealth by the wearer")),

        VAMPIRIC_GLOVE(
                        "vampiric_glove",
                        Material.CARROT_ON_A_STICK,
                        201041,
                        "vampiric_glove",
                        "\u00a7bVampiric Glove",
                        "hands",
                        Arrays.asList("\u00a77Causes the wearer's melee attacks to absorb health")),

        GOLDEN_HOOK(
                        "golden_hook",
                        Material.CARROT_ON_A_STICK,
                        201042,
                        "golden_hook",
                        "\u00a7bGolden Hook",
                        "hands",
                        Arrays.asList("\u00a77Increases the experience dropped by creatures")),

        ONION_RING(
                        "onion_ring",
                        Material.CARROT_ON_A_STICK,
                        201043,
                        "onion_ring",
                        "\u00a7bOnion Ring",
                        "hands",
                        Arrays.asList("\u00a77Grants a temporary boost to mining speed after eating food")),

        PICKAXE_HEATER(
                        "pickaxe_heater",
                        Material.CARROT_ON_A_STICK,
                        201044,
                        "pickaxe_heater",
                        "\u00a7bPickaxe Heater",
                        "hands",
                        Arrays.asList("\u00a77Automatically smelts mined ores")),

        AQUA_DASHERS(
                        "aqua_dashers",
                        Material.CARROT_ON_A_STICK,
                        201045,
                        "aqua_dashers",
                        "\u00a7bAqua-Dashers",
                        "feet",
                        Arrays.asList("\u00a77Allows the wearer to walk on fluids while sprinting")),

        BUNNY_HOPPERS(
                        "bunny_hoppers",
                        Material.CARROT_ON_A_STICK,
                        201046,
                        "bunny_hoppers",
                        "\u00a7bBunny Hoppers",
                        "feet",
                        Arrays.asList("\u00a77Increases the wearer's jump height",
                                        "\u00a77Grants immunity to fall damage")),

        KITTY_SLIPPERS(
                        "kitty_slippers",
                        Material.CARROT_ON_A_STICK,
                        201047,
                        "kitty_slippers",
                        "\u00a7bKitty Slippers",
                        "feet",
                        Arrays.asList("\u00a77Creepers avoid the wearer")),

        RUNNING_SHOES(
                        "running_shoes",
                        Material.CARROT_ON_A_STICK,
                        201048,
                        "running_shoes",
                        "\u00a7bRunning Shoes",
                        "feet",
                        Arrays.asList("\u00a77Increases the wearer's movement speed while sprinting",
                                        "\u00a77Increases the wearer's step height while sprinting")),

        SNOWSHOES(
                        "snowshoes",
                        Material.CARROT_ON_A_STICK,
                        201049,
                        "snowshoes",
                        "\u00a7bSnowshoes",
                        "feet",
                        Arrays.asList("\u00a77Allows the wearer to walk on Powder Snow",
                                        "\u00a77Makes ice less slippery to walk on")),

        STEADFAST_SPIKES(
                        "steadfast_spikes",
                        Material.CARROT_ON_A_STICK,
                        201050,
                        "steadfast_spikes",
                        "\u00a7bSteadfast Spikes",
                        "feet",
                        Arrays.asList("\u00a77Grants immunity to knockback")),

        FLIPPERS(
                        "flippers",
                        Material.CARROT_ON_A_STICK,
                        201051,
                        "flippers",
                        "\u00a7bFlippers",
                        "feet",
                        Arrays.asList("\u00a77Improves agility in water")),

        ROOTED_BOOTS(
                        "rooted_boots",
                        Material.CARROT_ON_A_STICK,
                        201052,
                        "rooted_boots",
                        "\u00a7bRooted Boots",
                        "feet",
                        Arrays.asList("\u00a77Slowly replenishes hunger while walking on grass")),

        STRIDER_SHOES(
                        "strider_shoes",
                        Material.CARROT_ON_A_STICK,
                        201054,
                        "strider_shoes",
                        "\u00a7bStrider Shoes",
                        "feet",
                        Arrays.asList("\u00a77Allows the wearer to stand on lava while sneaking",
                                        "\u00a77Grants protection against hot floor damage")),

        WARP_DRIVE(
                        "warp_drive",
                        Material.CARROT_ON_A_STICK,
                        201055,
                        "warp_drive",
                        "\u00a7bWarp Drive",
                        "belt",
                        Arrays.asList("\u00a77Ender Pearls are not consumed, but cost hunger instead",
                                        "\u00a77Ender Pearls deal no damage"),
                        true),

        CHARM_OF_SHRINKING(
                        "charm_of_shrinking",
                        Material.CARROT_ON_A_STICK,
                        201056,
                        "charm_of_shrinking",
                        "\u00a7bCharm of Shrinking",
                        "necklace",
                        Arrays.asList("\u00a77Shrinks the wearer"), true, null, 30f),

        WITHERED_BRACELET(
                        "withered_bracelet",
                        Material.CARROT_ON_A_STICK,
                        201057,
                        "withered_bracelet",
                        "\u00a7bWithered Bracelet",
                        "hands",
                        Arrays.asList("\u00a77Melee attacks have a chance to inflict a wither effect"));

        private final String id;
        private final Material baseMaterial;
        private final int customModelData;
        private final String itemModelKey;
        private final String displayName;
        private final String slotType; // null = not a curio
        private final List<String> lore;

        private final boolean has3DModel;
        private final Float pitchUpLimit;
        private final Float pitchDownLimit;

        ArtifactItem(String id, Material baseMaterial, int customModelData,
                        String itemModelKey, String displayName, String slotType,
                        List<String> lore) {
                this(id, baseMaterial, customModelData, itemModelKey, displayName, slotType, lore, false, null, null);
        }

        ArtifactItem(String id, Material baseMaterial, int customModelData,
                        String itemModelKey, String displayName, String slotType,
                        List<String> lore, boolean has3DModel) {
                this(id, baseMaterial, customModelData, itemModelKey, displayName, slotType, lore, has3DModel, null,
                                null);
        }

        ArtifactItem(String id, Material baseMaterial, int customModelData,
                        String itemModelKey, String displayName, String slotType,
                        List<String> lore, boolean has3DModel, Float pitchUpLimit, Float pitchDownLimit) {
                this.id = id;
                this.baseMaterial = baseMaterial;
                this.customModelData = customModelData;
                this.itemModelKey = itemModelKey;
                this.displayName = displayName;
                this.slotType = slotType;
                this.lore = lore;
                this.has3DModel = has3DModel;
                this.pitchUpLimit = pitchUpLimit;
                this.pitchDownLimit = pitchDownLimit;
        }

        public String getId() {
                return id;
        }

        public Material getBaseMaterial() {
                return baseMaterial;
        }

        public int getCustomModelData() {
                return customModelData;
        }

        /**
         * The key portion of the item model NamespacedKey (namespace is always
         * "artifacts").
         */
        public String getItemModelKey() {
                return itemModelKey;
        }

        public boolean has3DModel() {
                return has3DModel;
        }

        public int get3DCustomModelData() {
                return customModelData + 1000;
        }

        public String get3DItemModelKey() {
                return itemModelKey + "_3d";
        }

        public Float getPitchUpLimit() {
                return pitchUpLimit;
        }

        public Float getPitchDownLimit() {
                return pitchDownLimit;
        }

        public String getDisplayName() {
                return displayName;
        }

        /**
         * Returns the curio slot type, or null if this is not a curio item.
         */
        public String getSlotType() {
                return slotType;
        }

        public List<String> getLore() {
                return lore;
        }

        public boolean isCurio() {
                return slotType != null;
        }

        /**
         * Find an ArtifactItem by its string ID.
         */
        public static ArtifactItem fromId(String id) {
                for (ArtifactItem item : values()) {
                        if (item.id.equals(id)) {
                                return item;
                        }
                }
                return null;
        }
}
