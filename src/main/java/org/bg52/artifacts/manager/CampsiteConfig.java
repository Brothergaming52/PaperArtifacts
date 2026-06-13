package org.bg52.artifacts.manager;

import org.bg52.artifacts.Artifacts;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CampsiteConfig {

    private final Artifacts plugin;
    private final File configFile;
    private FileConfiguration config;

    private int count;
    private int minY;
    private int maxY;
    private double mimicChance;
    private double trappedChestChance;
    private double chestLootChance;
    private double barrelLootChance;
    private boolean allowLightSources;
    private boolean minimalistCampsites;
    private int maxCeilingHeight;
    private final Map<String, Boolean> enabledWorlds = new HashMap<>();

    public CampsiteConfig(Artifacts plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "Campsite.yml");
        reload();
    }

    public void reload() {
        if (!configFile.exists()) {
            plugin.saveResource("Campsite.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        count = config.getInt("campsite.count", 10);
        minY = config.getInt("campsite.min-y", -60);
        maxY = config.getInt("campsite.max-y", 40);
        mimicChance = config.getDouble("campsite.mimic-chance", 0.3);
        trappedChestChance = config.getDouble("campsite.trapped-chest-chance", 0.125);
        chestLootChance = config.getDouble("campsite.chest-loot-chance", 1.0);
        barrelLootChance = config.getDouble("campsite.barrel-loot-chance", 0.5);
        allowLightSources = config.getBoolean("campsite.allow-light-sources", true);
        minimalistCampsites = config.getBoolean("campsite.minimalist-campsites", false);
        maxCeilingHeight = config.getInt("campsite.max-ceiling-height", 6);

        // Load enabled worlds
        enabledWorlds.clear();
        if (config.contains("campsite.worlds")) {
            for (String worldName : config.getConfigurationSection("campsite.worlds").getKeys(false)) {
                enabledWorlds.put(worldName, config.getBoolean("campsite.worlds." + worldName));
            }
        }

        // Dynamic world discovery
        updateWorldList();
    }

    private void updateWorldList() {
        boolean changed = false;
        java.util.List<World> worlds = Bukkit.getWorlds();
        
        for (int i = 0; i < worlds.size(); i++) {
            World world = worlds.get(i);
            String name = world.getName();
            
            if (!enabledWorlds.containsKey(name)) {
                // If this is the first world (usually overworld) and we have no worlds yet, enable it
                // Otherwise, check if it's the overworld by environment if it's the first NORMAL world
                boolean shouldEnable = false;
                if (enabledWorlds.isEmpty() && i == 0) {
                    shouldEnable = true;
                } else if (world.getEnvironment() == World.Environment.NORMAL && !hasEnabledWorld()) {
                    shouldEnable = true;
                }
                
                enabledWorlds.put(name, shouldEnable);
                config.set("campsite.worlds." + name, shouldEnable);
                changed = true;
            }
        }

        if (changed) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save Campsite.yml: " + e.getMessage());
            }
        }
    }

    private boolean hasEnabledWorld() {
        for (boolean enabled : enabledWorlds.values()) {
            if (enabled) return true;
        }
        return false;
    }

    public boolean isWorldEnabled(String worldName) {
        return enabledWorlds.getOrDefault(worldName, false);
    }

    public int getCount() {
        return count;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public double getMimicChance() {
        return mimicChance;
    }

    public double getTrappedChestChance() {
        return trappedChestChance;
    }

    public double getChestLootChance() {
        return chestLootChance;
    }

    public double getBarrelLootChance() {
        return barrelLootChance;
    }

    public boolean isAllowLightSources() {
        return allowLightSources;
    }

    public boolean isMinimalistCampsites() {
        return minimalistCampsites;
    }

    public int getMaxCeilingHeight() {
        return maxCeilingHeight;
    }
}