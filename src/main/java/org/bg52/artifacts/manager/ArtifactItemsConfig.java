package org.bg52.artifacts.manager;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.curiospaper.data.AbilityData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages loading and reloading of artifacts.yml, allowing players to customize
 * display names, base materials, custom model data, item models, slot types,
 * lores, 3D model settings, and extra abilities for all artifact items.
 */
public class ArtifactItemsConfig {

    private final Artifacts plugin;
    private final File configFile;
    private FileConfiguration config;

    public ArtifactItemsConfig(Artifacts plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "artifacts.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("artifacts.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            ArtifactItem artifact = ArtifactItem.fromId(key);
            if (artifact == null) {
                plugin.getLogger().warning("Unknown artifact item key in artifacts.yml: " + key);
                continue;
            }

            // Name
            if (section.contains("name")) {
                String rawName = section.getString("name");
                if (rawName != null) {
                    artifact.setDisplayName(ChatColor.translateAlternateColorCodes('&', rawName));
                }
            }

            // Base Material
            if (section.contains("material")) {
                String matStr = section.getString("material");
                if (matStr != null) {
                    try {
                        Material mat = Material.valueOf(matStr.toUpperCase());
                        artifact.setBaseMaterial(mat);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material '" + matStr + "' for artifact " + key);
                    }
                }
            }

            // CustomModelData
            if (section.contains("custom-model-data")) {
                artifact.setCustomModelData(section.getInt("custom-model-data"));
            }

            // Item Model Key
            if (section.contains("item-model")) {
                String modelKey = section.getString("item-model");
                if (modelKey != null) {
                    artifact.setItemModelKey(modelKey);
                }
            }

            // Slot Type
            if (section.contains("slot-type")) {
                artifact.setSlotType(section.getString("slot-type"));
            }

            // Lore
            if (section.contains("lore")) {
                List<String> rawLore = section.getStringList("lore");
                List<String> coloredLore = new ArrayList<>();
                for (String line : rawLore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                artifact.setLore(coloredLore);
            }

            // 3D Model Settings
            if (section.contains("has-3d-model")) {
                artifact.setHas3DModel(section.getBoolean("has-3d-model"));
            }

            if (section.contains("pitch-up-limit")) {
                if (section.get("pitch-up-limit") == null) {
                    artifact.setPitchUpLimit(null);
                } else {
                    artifact.setPitchUpLimit((float) section.getDouble("pitch-up-limit"));
                }
            }

            if (section.contains("pitch-down-limit")) {
                if (section.get("pitch-down-limit") == null) {
                    artifact.setPitchDownLimit(null);
                } else {
                    artifact.setPitchDownLimit((float) section.getDouble("pitch-down-limit"));
                }
            }

            // Abilities
            List<AbilityData> abilitiesList = new ArrayList<>();
            ConfigurationSection abilitiesSection = section.getConfigurationSection("abilities");
            if (abilitiesSection != null) {
                for (String abilityKey : abilitiesSection.getKeys(false)) {
                    ConfigurationSection abilitySec = abilitiesSection.getConfigurationSection(abilityKey);
                    if (abilitySec != null) {
                        AbilityData ability = AbilityData.loadFromConfig(abilitySec);
                        if (ability != null && ability.isValid()) {
                            abilitiesList.add(ability);
                        } else {
                            plugin.getLogger().warning("Invalid ability '" + abilityKey + "' defined for artifact " + key);
                        }
                    }
                }
            }
            artifact.setAbilities(abilitiesList);
        }
    }

    public void reload() {
        load();
    }
}
