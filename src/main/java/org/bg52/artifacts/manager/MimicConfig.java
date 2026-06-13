package org.bg52.artifacts.manager;

import org.bg52.artifacts.Artifacts;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages the mimic.yml configuration, controlling which items
 * are allowed to drop from mimics.
 */
public class MimicConfig {

    private final Artifacts plugin;
    private final File configFile;
    private final Set<String> enabledDrops = new HashSet<>();

    public MimicConfig(Artifacts plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "mimic.yml");
        load();
    }

    /**
     * Loads or reloads the mimic configuration from disk.
     */
    public void load() {
        enabledDrops.clear();

        // Save default from resources if it doesn't exist
        if (!configFile.exists()) {
            plugin.saveResource("mimic.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<String> dropList = config.getStringList("enabled-drops");

        if (dropList != null) {
            for (String itemId : dropList) {
                if (itemId != null && !itemId.isEmpty()) {
                    enabledDrops.add(itemId.toLowerCase().trim());
                }
            }
        }

        int count = enabledDrops.size();
        plugin.getLogger().info("[Artifacts] Loaded " + count + " enabled mimic drops.");
    }

    /**
     * Checks if a specific artifact item is enabled for mimic drops.
     * 
     * @param itemId The ID of the artifact item
     * @return true if enabled, false otherwise
     */
    public boolean isDropEnabled(String itemId) {
        if (itemId == null) return false;
        return enabledDrops.contains(itemId.toLowerCase().trim());
    }
}
