package org.bg52.artifacts.item;

import org.bg52.artifacts.Artifacts;
import org.bg52.synthetix.distribution.MobDropRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Defines mob drop rules for artifact items, loaded from mobdrops.yml
 * and registered directly into Synthetix's DistributionManager.
 */
public class ArtifactMobDrops {

    public static List<MobDropRule> getMobDrops(Artifacts plugin) {
        List<MobDropRule> rules = new ArrayList<>();

        File configFile = new File(plugin.getDataFolder(), "mobdrops.yml");
        if (!configFile.exists()) {
            plugin.saveResource("mobdrops.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.getBoolean("mob-drops.enabled", true)) {
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

            List<Map<?, ?>> tablesList = itemSection.getMapList("mobs");
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

                String mobId = (String) tableMap.get("mob");
                Number chance = (Number) tableMap.get("chance");
                Number min = (Number) tableMap.get("min");
                Number max = (Number) tableMap.get("max");

                if (mobId != null && chance != null && min != null && max != null) {
                    String ruleId = "artifacts_" + itemId.toLowerCase(Locale.ROOT) + "_" + (++idx);
                    rules.add(new MobDropRule(
                            ruleId,
                            "synth:" + itemId.toLowerCase(Locale.ROOT),
                            mobId.toUpperCase(Locale.ROOT),
                            chance.doubleValue(),
                            0.0,
                            min.intValue(),
                            max.intValue(),
                            true,
                            Collections.emptyList(),
                            Collections.emptyList()
                    ));
                }
            }
        }

        return rules;
    }
}
