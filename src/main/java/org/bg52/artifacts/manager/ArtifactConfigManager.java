package org.bg52.artifacts.manager;

import org.bg52.artifacts.Artifacts;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages the Artifacts config.yml, providing typed access to all
 * configurable ability parameters. Each item's abilities can be
 * toggled, and strength/duration values can be adjusted.
 *
 * Changes to config.yml take effect on reload — no code edits needed.
 */
public class ArtifactConfigManager {

    private final Artifacts plugin;
    private FileConfiguration config;

    public ArtifactConfigManager(Artifacts plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // --- Config Versioning & Migration ---
        String currentVersion = plugin.getDescription().getVersion();
        String configVersion = config.getString("version", "0.0.0");

        if (isOlder(configVersion, currentVersion)) {
            plugin.getLogger().info("Config version " + configVersion + " is older than plugin version " + currentVersion + ". Migrating config...");
            migrateConfig(currentVersion);
            plugin.reloadConfig();
            this.config = plugin.getConfig();
        }

        // --- Update Checker ---
        if (config.getBoolean("updates.check-for-updates", true)) {
            plugin.getLogger().info("Triggering update checker...");
            new org.bg52.artifacts.util.UpdateChecker(plugin).check();
        }
    }

    /**
     * Reload config from disk.
     */
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ──────────────────────────────────────────────
    // Global item enable/disable
    // ──────────────────────────────────────────────

    public boolean isItemEnabled(String itemId) {
        return config.getBoolean("items." + itemId + ".enabled", true);
    }

    // ──────────────────────────────────────────────
    // Umbrella
    // ──────────────────────────────────────────────

    public int getUmbrellaSlowFallingAmplifier() {
        return config.getInt("items.umbrella.slow-falling-amplifier", 0);
    }

    // ──────────────────────────────────────────────
    // Everlasting Beef / Eternal Steak
    // ──────────────────────────────────────────────

    public int getEverlastingBeefCooldown() {
        return config.getInt("items.everlasting_beef.cooldown-seconds", 15);
    }

    public int getEternalSteakCooldown() {
        return config.getInt("items.eternal_steak.cooldown-seconds", 15);
    }

    public int getEternalSteakFurnaceTime() {
        return config.getInt("items.eternal_steak.furnace-time-seconds", 10);
    }

    public int getEternalSteakSmokerTime() {
        return config.getInt("items.eternal_steak.smoker-time-seconds", 5);
    }

    public int getEternalSteakCampfireTime() {
        return config.getInt("items.eternal_steak.campfire-time-seconds", 10);
    }

    // ──────────────────────────────────────────────
    // Snorkel
    // ──────────────────────────────────────────────

    public int getSnorkelOxygenDuration() {
        return config.getInt("items.snorkel.oxygen-duration-seconds", 15);
    }

    // ──────────────────────────────────────────────
    // Villager Hat
    // ──────────────────────────────────────────────


    public int getVillagerHatHeroOfVillageLevel() {
        return config.getInt("items.villager_hat.hero-of-village-level", 2);
    }

    // ──────────────────────────────────────────────
    // Superstitious Hat
    // ──────────────────────────────────────────────

    public int getSuperstitiousHatLootingBonus() {
        return config.getInt("items.superstitious_hat.looting-bonus", 1);
    }

    // ──────────────────────────────────────────────
    // Cowboy Hat
    // ──────────────────────────────────────────────

    public int getCowboyHatSpeedAmplifier() {
        return config.getInt("items.cowboy_hat.mount-speed-amplifier", 0);
    }


    // ──────────────────────────────────────────────
    // Lucky Scarf
    // ──────────────────────────────────────────────

    public int getLuckyScarfFortuneBonus() {
        return config.getInt("items.lucky_scarf.fortune-bonus", 1);
    }

    // ──────────────────────────────────────────────
    // Cross Necklace
    // ──────────────────────────────────────────────

    public int getCrossNecklaceInvulnerabilityTicks() {
        return config.getInt("items.cross_necklace.invulnerability-ticks", 30);
    }

    // ──────────────────────────────────────────────
    // Panic Necklace
    // ──────────────────────────────────────────────

    public int getPanicNecklaceSpeedAmplifier() {
        return config.getInt("items.panic_necklace.speed-amplifier", 0);
    }

    public int getPanicNecklaceSpeedDuration() {
        return config.getInt("items.panic_necklace.speed-duration-seconds", 7);
    }

    // ──────────────────────────────────────────────
    // Shock Pendant
    // ──────────────────────────────────────────────

    public double getShockPendantChance() {
        return config.getDouble("items.shock_pendant.lightning-chance", 0.15);
    }

    public boolean getShockPendantLightningImmunity() {
        return config.getBoolean("items.shock_pendant.lightning-immunity", true);
    }

    // ──────────────────────────────────────────────
    // Flame Pendant
    // ──────────────────────────────────────────────

    public double getFlamePendantChance() {
        return config.getDouble("items.flame_pendant.fire-chance", 0.30);
    }

    public double getFlamePendantDamage() {
        return config.getDouble("items.flame_pendant.instant-damage", 2.0);
    }

    public int getFlamePendantFireDuration() {
        return config.getInt("items.flame_pendant.fire-duration-seconds", 4);
    }

    public int getFlamePendantFireResistanceDuration() {
        return config.getInt("items.flame_pendant.fire-resistance-duration-seconds", 10);
    }

    // ──────────────────────────────────────────────
    // Plastic Drinking Hat
    // ──────────────────────────────────────────────

    public int getPlasticDrinkingHatSpeed() {
        return config.getInt("items.plastic_drinking_hat.eat-speed-ticks", 24);
    }

    // ──────────────────────────────────────────────
    // Novelty Drinking Hat
    // ──────────────────────────────────────────────

    public int getNoveltyDrinkingHatSpeed() {
        return config.getInt("items.novelty_drinking_hat.eat-speed-ticks", 16);
    }

    // ──────────────────────────────────────────────
    // Night Vision Goggles
    // ──────────────────────────────────────────────


    // ──────────────────────────────────────────────
    // Angler's Hat
    // ──────────────────────────────────────────────

    public int getAnglersHatLuckBonus() {
        return config.getInt("items.anglers_hat.luck-of-the-sea-bonus", 1);
    }

    public int getAnglersHatLureBonus() {
        return config.getInt("items.anglers_hat.lure-bonus", 1);
    }

    // ──────────────────────────────────────────────
    // Scarf of Invisibility
    // ──────────────────────────────────────────────


    // ──────────────────────────────────────────────
    // Thorn Pendant
    // ──────────────────────────────────────────────

    public double getThornPendantChance() {
        return config.getDouble("items.thorn_pendant.thorn-chance", 0.30);
    }

    public double getThornPendantDamageMin() {
        return config.getDouble("items.thorn_pendant.thorn-damage-min", 1.0);
    }

    public double getThornPendantDamageMax() {
        return config.getDouble("items.thorn_pendant.thorn-damage-max", 4.0);
    }

    // ──────────────────────────────────────────────
    // Obsidian Skull
    // ──────────────────────────────────────────────

    public int getObsidianSkullFireResistanceDuration() {
        return config.getInt("items.obsidian_skull.fire-resistance-duration-seconds", 10);
    }

    public int getObsidianSkullCooldown() {
        return config.getInt("items.obsidian_skull.cooldown-seconds", 60);
    }

    // ──────────────────────────────────────────────
    // Antidote Vessel
    // ──────────────────────────────────────────────

    public int getAntidoteVesselCapDurationSeconds() {
        return config.getInt("items.antidote_vessel.cap-duration-seconds", 6);
    }

    // ──────────────────────────────────────────────
    // Universal Attractor
    // ──────────────────────────────────────────────

    public int getUniversalAttractorRadius() {
        return config.getInt("items.universal_attractor.radius", 5);
    }

    public int getUniversalAttractorCooldown() {
        return config.getInt("items.universal_attractor.cooldown-seconds", 10);
    }

    // ──────────────────────────────────────────────
    // Crystal Heart
    // ──────────────────────────────────────────────

    public int getCrystalHeartBonusHealth() {
        return config.getInt("items.crystal_heart.bonus-health", 10);
    }

    // ──────────────────────────────────────────────
    // Helium Flamingo
    // ──────────────────────────────────────────────

    public int getHeliumFlamingoCooldown() {
        return config.getInt("items.helium_flamingo.cooldown-seconds", 15);
    }

    public int getHeliumFlamingoUseSeconds() {
        return config.getInt("items.helium_flamingo.use-seconds", 15);
    }

    // ──────────────────────────────────────────────
    // Digging Claws
    // ──────────────────────────────────────────────

    public int getDiggingClawsSpeedAmplifier() {
        return config.getInt("items.digging_claws.speed-amplifier", 3);
    }

    // ──────────────────────────────────────────────
    // Feral Claws
    // ──────────────────────────────────────────────

    public double getFeralClawsAttackSpeed() {
        return config.getDouble("items.feral_claws.attack-speed-amplifier", 0.35);
    }

    // ──────────────────────────────────────────────
    // Power Glove
    // ──────────────────────────────────────────────

    public double getPowerGloveAttackDamage() {
        return config.getDouble("items.power_glove.attack-damage-multiplier", 4.0);
    }

    // ──────────────────────────────────────────────
    // Fire Gauntlet
    // ──────────────────────────────────────────────

    public int getFireGauntletFireDuration() {
        return config.getInt("items.fire_gauntlet.fire-duration-seconds", 4);
    }

    // ──────────────────────────────────────────────
    // Pocket Piston
    // ──────────────────────────────────────────────

    public double getPocketPistonKnockbackAmplifier() {
        return config.getDouble("items.pocket_piston.knockback-amplifier", 1.5);
    }

    // ──────────────────────────────────────────────
    // Vampiric Glove
    // ──────────────────────────────────────────────

    public double getVampiricGloveHealthAbsorbed() {
        return config.getDouble("items.vampiric_glove.health-absorbed", 0.2);
    }

    // ──────────────────────────────────────────────
    // Golden Hook
    // ──────────────────────────────────────────────

    public double getGoldenHookExpBonus() {
        return config.getDouble("items.golden_hook.exp-bonus", 0.1);
    }

    // ──────────────────────────────────────────────
    // Onion Ring
    // ──────────────────────────────────────────────

    public int getOnionRingSpeedAmplifier() {
        return config.getInt("items.onion_ring.speed-amplifier", 2);
    }

    public int getOnionRingDurationSeconds() {
        return config.getInt("items.onion_ring.duration-seconds", 15);
    }

    // ──────────────────────────────────────────────
    // Bunny Hoppers
    // ──────────────────────────────────────────────

    public int getBunnyHoppersJumpHeightAmplifier() {
        return config.getInt("items.bunny_hoppers.jump-height-amplifier", 1);
    }

    public boolean isBunnyHoppersFallDamageImmune() {
        return config.getBoolean("items.bunny_hoppers.fall-damage-immunity", true);
    }

    // ──────────────────────────────────────────────
    // Running Shoes
    // ──────────────────────────────────────────────

    public double getRunningShoesSpeedAmplifier() {
        return config.getDouble("items.running_shoes.speed-amplifier", 0.4);
    }

    public double getRunningShoesStepHeightAmplifier() {
        return config.getDouble("items.running_shoes.step-height-amplifier", 1.0);
    }

    // ──────────────────────────────────────────────
    // Snowshoes
    // ──────────────────────────────────────────────

    public boolean isSnowshoesPowderSnowImmune() {
        return config.getBoolean("items.snowshoes.powder-snow-immunity", true);
    }

    public double getSnowshoesIceSlipperinessReduction() {
        return config.getDouble("items.snowshoes.ice-slipperiness-reduction", 0.5);
    }

    // ──────────────────────────────────────────────
    // Steadfast Spikes
    // ──────────────────────────────────────────────

    public double getSteadfastSpikesKnockbackAmplifier() {
        return config.getDouble("items.steadfast_spikes.knockback-amplifier", 1.0);
    }

    // ──────────────────────────────────────────────
    // Flippers
    // ──────────────────────────────────────────────

    public int getFlippersDolphinsGraceAmplifier() {
        return config.getInt("items.flippers.dolphins-grace-amplifier", 0);
    }

    // ──────────────────────────────────────────────
    // Rooted Boots
    // ──────────────────────────────────────────────

    public double getRootedBootsHungerReplenishSpeed() {
        return config.getDouble("items.rooted_boots.hunger-replenish-speed", 0.5);
    }

    public int getRootedBootsHungerReplenishInterval() {
        return config.getInt("items.rooted_boots.hunger-replenish-interval", 40);
    }

    // ──────────────────────────────────────────────
    // Whoopee Cushion
    // ──────────────────────────────────────────────


    // ──────────────────────────────────────────────
    // Withered Bracelet
    // ──────────────────────────────────────────────

    public int getWitheredBraceletWitherDuration() {
        return config.getInt("items.withered_bracelet.wither-duration-seconds", 10);
    }

    public double getWitheredBraceletWitherAmplifier() {
        return config.getDouble("items.withered_bracelet.wither-amplifier", 0);
    }

    public double getWitheredBraceletWitherChance() {
        return config.getDouble("items.withered_bracelet.wither-chance", 0.4);
    }

    // ──────────────────────────────────────────────
    // Warp Drive
    // ──────────────────────────────────────────────

    public int getWarpDriveHungerCost() {
        return config.getInt("items.warp_drive.hunger-cost", 2);
    }

    private boolean isOlder(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i].replaceAll("[^0-9]", "")) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i].replaceAll("[^0-9]", "")) : 0;
            if (p1 < p2) return true;
            if (p1 > p2) return false;
        }
        return false;
    }

    private void migrateConfig(String newVersion) {
        java.io.File configFile = new java.io.File(plugin.getDataFolder(), "config.yml");
        java.io.File backupFile = new java.io.File(plugin.getDataFolder(), "config.old.yml");

        if (configFile.exists()) {
            configFile.renameTo(backupFile);
        }

        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration newConfig = plugin.getConfig();
        FileConfiguration oldConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(backupFile);

        for (String key : oldConfig.getKeys(true)) {
            if (newConfig.contains(key) && !oldConfig.isConfigurationSection(key)) {
                if (!key.equals("version")) {
                    newConfig.set(key, oldConfig.get(key));
                }
            }
        }

        newConfig.set("version", newVersion);

        try {
            newConfig.save(configFile);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not save migrated config: " + e.getMessage());
        }

        // --- Migrate additional files ---
        String[] filesToMigrate = {
            "Campsite.yml", "FoodInfo.yml", "loottables.yml", "mimic.yml",
            "mobdrops.yml", "SmeltingRecipes.yml", "StoneTierBlocks.yml"
        };
        for (String fileName : filesToMigrate) {
            migrateYamlFile(fileName, null);
        }

        // --- Delete resources folder ---
        java.io.File resourcesFolder = new java.io.File(plugin.getDataFolder(), "resources");
        if (resourcesFolder.exists()) {
            deleteDirectory(resourcesFolder);
            plugin.getLogger().info("Deleted old resources folder.");
        }

        // --- Cleanup .old.yml files ---
        backupFile.delete();
        for (String fileName : filesToMigrate) {
            new java.io.File(plugin.getDataFolder(), fileName.replace(".yml", ".old.yml")).delete();
        }
    }

    private void migrateYamlFile(String fileName, String versionKey) {
        java.io.File file = new java.io.File(plugin.getDataFolder(), fileName);
        java.io.File backupFile = new java.io.File(plugin.getDataFolder(), fileName.replace(".yml", ".old.yml"));

        if (file.exists()) {
            file.renameTo(backupFile);
        }

        plugin.saveResource(fileName, true);
        FileConfiguration newConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        FileConfiguration oldConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(backupFile);

        for (String key : oldConfig.getKeys(true)) {
            if (newConfig.contains(key) && !oldConfig.isConfigurationSection(key)) {
                if (versionKey == null || !key.equals(versionKey)) {
                    newConfig.set(key, oldConfig.get(key));
                }
            }
        }

        try {
            newConfig.save(file);
            plugin.getLogger().info("Migrated " + fileName);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not save migrated " + fileName + ": " + e.getMessage());
        }
    }

    private void deleteDirectory(java.io.File directory) {
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
