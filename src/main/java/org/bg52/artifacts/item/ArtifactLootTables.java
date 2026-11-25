package org.bg52.artifacts.item;

import org.bg52.artifacts.Artifacts;
import org.bg52.synthetix.distribution.LootTableRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Defines loot table injection rules for artifact items, loaded from loottables.yml
 * and registered directly into Synthetix's DistributionManager.
 */
public class ArtifactLootTables {

    public static List<LootTableRule> getLootTables(Artifacts plugin) {
        List<LootTableRule> rules = new ArrayList<>();

        File configFile = new File(plugin.getDataFolder(), "loottables.yml");
        if (!configFile.exists()) {
            plugin.saveResource("loottables.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.getBoolean("loot-tables.enabled", true)) {
            return rules;
        }

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return rules;

        for (String itemId : itemsSection.getKeys(false)) {
            if (!plugin.getArtifactConfig().isItemEnabled(itemId)) {
                continue;
            }
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null || !itemSection.getBoolean("enabled", true)) {
                continue;
            }

            List<Map<?, ?>> tablesList = itemSection.getMapList("tables");
            int idx = 0;
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
                    String ruleId = "artifacts_" + itemId.toLowerCase(Locale.ROOT) + "_" + (++idx);
                    rules.add(new LootTableRule(
                            ruleId,
                            "synth:" + itemId.toLowerCase(Locale.ROOT),
                            Collections.singletonList(tableId),
                            chance.doubleValue(),
                            min.intValue(),
                            max.intValue(),
                            1
                    ));
                }
            }
        }

        return rules;
    }
}
