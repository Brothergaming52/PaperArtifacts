package org.bg52.artifacts.item;

import org.bg52.artifacts.Artifacts;
import org.bg52.curiospaper.data.LootTableData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines loot tables for artifact items to be registered via the CuriosPaper
 * API. Reads configuration directly from loottables.yml.
 */
public class ArtifactLootTables {

    /**
     * Get a map of item IDs to their corresponding loot table data.
     * 
     * @param plugin The Artifacts plugin instance to access the data folder.
     * @return Map of artifact ID to list of LootTableData
     */
    public static Map<String, List<LootTableData>> getLootTables(Artifacts plugin) {
        Map<String, List<LootTableData>> lootTables = new HashMap<>();

        File configFile = new File(plugin.getDataFolder(), "loottables.yml");
        if (!configFile.exists()) {
            plugin.saveResource("loottables.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.getBoolean("loot-tables.enabled", true)) {
            return lootTables;
        }

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return lootTables;

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null || !itemSection.getBoolean("enabled", true)) {
                continue;
            }

            List<LootTableData> tables = new ArrayList<>();
            List<Map<?, ?>> tablesList = itemSection.getMapList("tables");
            for (Map<?, ?> tableMap : tablesList) {
                Boolean enabled = true;
                if (tableMap.get("enabled") instanceof Boolean) {
                    enabled = (Boolean) tableMap.get("enabled");
                } else if (tableMap.get("enabled") instanceof String) {
                    enabled = Boolean.parseBoolean((String) tableMap.get("enabled"));
                }
                
                if (!enabled) {
                    continue;
                }

                String tableId = (String) tableMap.get("table");
                Number chance = (Number) tableMap.get("chance");
                Number min = (Number) tableMap.get("min");
                Number max = (Number) tableMap.get("max");

                if (tableId != null && chance != null && min != null && max != null) {
                    tables.add(new LootTableData(tableId, chance.doubleValue(), min.intValue(), max.intValue()));
                }
            }

            if (!tables.isEmpty()) {
                lootTables.put(itemId, tables);
            }
        }

        return lootTables;
    }
}
