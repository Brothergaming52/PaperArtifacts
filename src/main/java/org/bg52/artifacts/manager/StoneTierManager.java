package org.bg52.artifacts.manager;

import org.bg52.artifacts.Artifacts;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages the list of blocks considered "stone tier".
 * Loads from StoneTierBlocks.yml and filters out materials not available
 * in the current Minecraft version.
 */
public class StoneTierManager {

    private final Artifacts plugin;
    private final Set<Material> stoneTierBlocks = new HashSet<>();
    private final File configFile;

    public StoneTierManager(Artifacts plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "StoneTierBlocks.yml");
        load();
    }

    /**
     * Loads or reloads the stone tier blocks from disk.
     */
    public void load() {
        stoneTierBlocks.clear();

        // Save default from resources if it doesn't exist
        if (!configFile.exists()) {
            plugin.saveResource("StoneTierBlocks.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<String> blockList = config.getStringList("stone-tier-blocks");

        for (String materialName : blockList) {
            if (materialName == null || materialName.isEmpty()) continue;

            // Safely parse material name. Material.getMaterial() returns null 
            // if the material is unknown (e.g., a 1.17 block on a 1.14 server).
            Material material = Material.getMaterial(materialName.toUpperCase().trim());
            
            if (material != null) {
                stoneTierBlocks.add(material);
            }
        }

        int count = stoneTierBlocks.size();
        plugin.getLogger().info("[Artifacts] Loaded " + count + " valid stone tier blocks.");
    }

    /**
     * Returns true if the given material is in the stone tier list.
     */
    public boolean isStoneTierBlock(Material material) {
        if (material == null) return false;
        return stoneTierBlocks.contains(material);
    }
}
