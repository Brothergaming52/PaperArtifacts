package org.bg52.artifacts.manager;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.entity.MimicAI;
import org.bg52.artifacts.entity.MimicAnimator;
import org.bg52.artifacts.entity.MimicEntity;
import org.bukkit.Bukkit;
import org.bg52.curiospaper.storage.CuriosStorageAPI;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all active mimic entities and tracks mimic chest locations.
 * Mimic chest locations are persisted to disk so they survive server restarts.
 */
public class MimicManager {

    private final Artifacts plugin;

    // Active mimics indexed by slime UUID
    private final Map<UUID, MimicAI> activeMimics = new ConcurrentHashMap<UUID, MimicAI>();

    // Zombie UUID → slime UUID mapping for reverse lookup
    private final Map<UUID, UUID> zombieToSlime = new ConcurrentHashMap<UUID, UUID>();

    // Locations of placed mimic chests (not yet opened)
    // Using block-snapped locations as keys
    private final Set<Location> mimicChestLocations = Collections.synchronizedSet(new HashSet<Location>());

    // Locations of recently died mimics (for preventing slime split)
    private final Set<Location> recentDeathLocations = Collections.synchronizedSet(new HashSet<Location>());

    // Persistence file for mimic chest locations
    private final File dataFile;
    private final CuriosStorageAPI storageAPI;
    private static final UUID GLOBAL_DATA_UUID = new UUID(0L, 0L);
    private static final String ADDON_ID = "mimic-chests";

    public MimicManager(Artifacts plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "mimic-chests.yml");
        this.storageAPI = plugin.getCuriosPaperAPI().getStorageAPI();
        loadMimicChests();
    }

    /**
     * Spawn a new mimic at the given location.
     */
    public MimicAI spawnMimic(Location location) {
        MimicEntity entity = new MimicEntity(plugin);
        entity.spawn(location);

        MimicAnimator animator = new MimicAnimator(entity);
        MimicAI ai = new MimicAI(plugin, entity, animator, this);

        UUID slimeUUID = entity.getSlime().getUniqueId();

        activeMimics.put(slimeUUID, ai);

        // Track the zombie for cleanup and damage protection
        if (entity.getZombie() != null) {
            zombieToSlime.put(entity.getZombie().getUniqueId(), slimeUUID);
        }

        // Start the AI tick loop (every tick)
        ai.runTaskTimer(plugin, 1L, 1L);

        return ai;
    }

    /**
     * Remove a mimic by slime UUID.
     */
    public void removeMimic(UUID slimeUUID) {
        MimicAI ai = activeMimics.remove(slimeUUID);
        if (ai != null) {
            MimicEntity entity = ai.getMimicEntity();
            if (entity.getZombie() != null) {
                zombieToSlime.remove(entity.getZombie().getUniqueId());
            }
            try {
                ai.cancel();
            } catch (IllegalStateException ignored) {
                // Already cancelled
            }
            entity.remove();
        }
    }

    /**
     * Revert a mimic back to a chest block and re-register the location as a mimic
     * chest.
     * Called when the player leaves detection range or logs out.
     */
    public void revertMimic(UUID slimeUUID) {
        MimicAI ai = activeMimics.remove(slimeUUID);
        if (ai != null) {
            MimicEntity entity = ai.getMimicEntity();

            if (entity.getZombie() != null) {
                zombieToSlime.remove(entity.getZombie().getUniqueId());
            }
            try {
                ai.cancel();
            } catch (IllegalStateException ignored) {
            }

            // Revert to chest block — uses safe placement now
            Location chestLoc = entity.revertToChest();

            // Re-register the location as a mimic chest so it triggers again
            if (chestLoc != null) {
                chestLoc.getWorld().playSound(chestLoc, "artifacts:entity.mimic.close", 1.0f, 1.0f);
                addMimicChest(chestLoc);
            }
        }
    }

    /**
     * Get a MimicAI by slime UUID.
     */
    public MimicAI getMimic(UUID slimeUUID) {
        return activeMimics.get(slimeUUID);
    }

    /**
     * Get a MimicAI by zombie UUID.
     */
    public MimicAI getMimicByZombie(UUID zombieUUID) {
        UUID slimeUUID = zombieToSlime.get(zombieUUID);
        if (slimeUUID != null) {
            return activeMimics.get(slimeUUID);
        }
        return null;
    }

    /**
     * Check if an entity UUID belongs to a mimic (slime or zombie).
     */
    public boolean isMimicEntity(UUID entityUUID) {
        return activeMimics.containsKey(entityUUID) || zombieToSlime.containsKey(entityUUID);
    }

    // ─── Mimic Chest Location Tracking ───

    public void addMimicChest(Location location) {
        Location snapped = toBlockLocation(location);
        if (mimicChestLocations.add(snapped)) {
            upsertLocationToTable(snapped);
        }
    }

    public boolean isMimicChest(Location location) {
        return mimicChestLocations.contains(toBlockLocation(location));
    }

    public void removeMimicChest(Location location) {
        Location snapped = toBlockLocation(location);
        if (mimicChestLocations.remove(snapped)) {
            deleteLocationFromTable(snapped);
        }
    }

    // ─── Death Location Tracking (for slime split prevention) ───

    public void addDeathLocation(Location location) {
        final Location loc = toBlockLocation(location);
        recentDeathLocations.add(loc);
        // Remove after 5 ticks
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                recentDeathLocations.remove(loc);
            }
        }, 5L);
    }

    public boolean isRecentDeathLocation(Location location) {
        Location block = toBlockLocation(location);
        for (Location deathLoc : recentDeathLocations) {
            if (deathLoc.getWorld() != null && deathLoc.getWorld().equals(block.getWorld())
                    && deathLoc.distanceSquared(block) < 9.0) { // Within 3 blocks
                return true;
            }
        }
        return false;
    }

    /**
     * Normalize a location to block coordinates for consistent set lookups.
     */
    private Location toBlockLocation(Location location) {
        return new Location(location.getWorld(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    // ─── Persistence & Database Sync ───

    /**
     * Load mimic chest locations. Uses custom database table,
     * handling migration from legacy data if present.
     */
    private void loadMimicChests() {
        if (storageAPI == null) {
            loadFromYamlFile(dataFile);
            return;
        }

        // 1. Register the custom table
        Map<String, String> sqliteCols = new java.util.HashMap<>();
        sqliteCols.put("world", "VARCHAR(64)");
        sqliteCols.put("x", "INTEGER");
        sqliteCols.put("y", "INTEGER");
        sqliteCols.put("z", "INTEGER");

        Map<String, String> mysqlCols = new java.util.HashMap<>();
        mysqlCols.put("world", "VARCHAR(64)");
        mysqlCols.put("x", "INT");
        mysqlCols.put("y", "INT");
        mysqlCols.put("z", "INT");

        List<String> pks = java.util.Arrays.asList("world", "x", "y", "z");

        try {
            storageAPI.registerTable("artifact_mimic_chests", sqliteCols, mysqlCols, pks).join();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register custom table artifact_mimic_chests", e);
        }

        // 2. Check for legacy yaml file migration
        boolean migratedYaml = false;
        if (dataFile.exists()) {
            plugin.getLogger().info("Found legacy mimic-chests.yml file. Migrating to custom table...");
            loadFromYamlFile(dataFile);
            migratedYaml = true;
        }

        // 3. Check for legacy database migration (if database mode is active)
        boolean migratedDb = false;
        if (storageAPI.isDatabaseMode()) {
            try {
                String legacyData = storageAPI.loadData(GLOBAL_DATA_UUID, ADDON_ID).join();
                if (legacyData != null && !legacyData.trim().isEmpty()) {
                    plugin.getLogger().info("Found legacy database mimic-chests data. Migrating to custom table...");
                    loadFromLegacyString(legacyData);
                    migratedDb = true;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to read legacy mimic chest data from database", e);
            }
        }

        // 4. If any legacy data was found, save it all to the new custom table and delete legacy data
        if (migratedYaml || migratedDb) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            synchronized (mimicChestLocations) {
                for (Location loc : mimicChestLocations) {
                    futures.add(upsertLocationToTable(loc));
                }
            }

            // Wait for all migrations to be written to table
            if (!futures.isEmpty()) {
                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    plugin.getLogger().info("Migrated " + futures.size() + " location(s) to custom table.");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to complete migration to custom table", e);
                }
            }

            // Cleanup legacy YAML file
            if (dataFile.exists()) {
                File migrated = new File(dataFile.getParentFile(), dataFile.getName() + ".migrated");
                if (dataFile.renameTo(migrated)) {
                    plugin.getLogger().info("Successfully renamed legacy file to " + migrated.getName());
                } else {
                    plugin.getLogger().warning("Could not rename legacy file " + dataFile.getName());
                }
            }

            // Cleanup legacy database data
            if (migratedDb) {
                try {
                    storageAPI.deleteData(GLOBAL_DATA_UUID, ADDON_ID).join();
                    plugin.getLogger().info("Successfully deleted legacy database data for mimic-chests.");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to delete legacy mimic-chests data from database", e);
                }
            }
        } else {
            // 5. No migration needed, just load from the custom table
            loadFromCustomTable();
        }
    }

    private void loadFromYamlFile(File file) {
        if (!file.exists())
            return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<?> locations = config.getList("locations");
        if (locations == null)
            return;

        int loaded = 0;
        for (Object obj : locations) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                String worldName = (String) map.get("world");
                if (worldName == null)
                    continue;
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Mimic chest in unknown world '" + worldName
                            + "', skipping (will load if world loads later)");
                    continue;
                }
                int x = ((Number) map.get("x")).intValue();
                int y = ((Number) map.get("y")).intValue();
                int z = ((Number) map.get("z")).intValue();
                mimicChestLocations.add(new Location(world, x, y, z));
                loaded++;
            }
        }

        if (loaded > 0) {
            plugin.getLogger().info("Loaded " + loaded + " mimic chest location(s) from legacy YAML.");
        }
    }

    private void loadFromLegacyString(String serialized) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(serialized);
            List<?> locations = config.getList("locations");
            if (locations == null)
                return;

            int loaded = 0;
            for (Object obj : locations) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    String worldName = (String) map.get("world");
                    if (worldName == null)
                        continue;
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("Mimic chest in unknown world '" + worldName
                                + "', skipping");
                        continue;
                    }
                    int x = ((Number) map.get("x")).intValue();
                    int y = ((Number) map.get("y")).intValue();
                    int z = ((Number) map.get("z")).intValue();
                    mimicChestLocations.add(new Location(world, x, y, z));
                    loaded++;
                }
            }
            if (loaded > 0) {
                plugin.getLogger().info("Loaded " + loaded + " mimic chest location(s) from legacy database data.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse legacy mimic locations string", e);
        }
    }

    private void loadFromCustomTable() {
        try {
            List<Map<String, Object>> rows = storageAPI.selectRows("artifact_mimic_chests", new java.util.HashMap<>()).join();
            int loaded = 0;
            for (Map<String, Object> row : rows) {
                String worldName = (String) row.get("world");
                if (worldName == null) continue;
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Mimic chest in unknown world '" + worldName
                            + "', skipping (will load if world loads later)");
                    continue;
                }
                int x = ((Number) row.get("x")).intValue();
                int y = ((Number) row.get("y")).intValue();
                int z = ((Number) row.get("z")).intValue();
                mimicChestLocations.add(new Location(world, x, y, z));
                loaded++;
            }
            if (loaded > 0) {
                plugin.getLogger().info("Loaded " + loaded + " mimic chest location(s) from custom table.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load mimic chest locations from custom table", e);
        }
    }

    private CompletableFuture<Void> upsertLocationToTable(Location loc) {
        if (loc.getWorld() == null || storageAPI == null) {
            return CompletableFuture.completedFuture(null);
        }
        Map<String, Object> rowData = new java.util.HashMap<>();
        rowData.put("world", loc.getWorld().getName());
        rowData.put("x", loc.getBlockX());
        rowData.put("y", loc.getBlockY());
        rowData.put("z", loc.getBlockZ());

        List<String> pks = java.util.Arrays.asList("world", "x", "y", "z");

        return storageAPI.upsertRow("artifact_mimic_chests", rowData, pks);
    }

    private CompletableFuture<Void> deleteLocationFromTable(Location loc) {
        if (loc.getWorld() == null || storageAPI == null) {
            return CompletableFuture.completedFuture(null);
        }
        Map<String, Object> conditions = new java.util.HashMap<>();
        conditions.put("world", loc.getWorld().getName());
        conditions.put("x", loc.getBlockX());
        conditions.put("y", loc.getBlockY());
        conditions.put("z", loc.getBlockZ());

        return storageAPI.deleteRows("artifact_mimic_chests", conditions);
    }

    /**
     * Remove all active mimics. Called on plugin disable.
     * Reverts all active mimics back to chests first, then saves locations.
     */
    public void shutdown() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Revert all active mimics to chests so they survive the restart
        List<UUID> toRevert = new ArrayList<UUID>(activeMimics.keySet());
        for (UUID slimeUUID : toRevert) {
            MimicAI ai = activeMimics.remove(slimeUUID);
            if (ai != null) {
                MimicEntity entity = ai.getMimicEntity();
                if (entity.getZombie() != null) {
                    zombieToSlime.remove(entity.getZombie().getUniqueId());
                }
                try {
                    ai.cancel();
                } catch (IllegalStateException ignored) {
                }
                // Revert to chest and track the new location
                Location chestLoc = entity.revertToChest();
                if (chestLoc != null) {
                    Location snapped = toBlockLocation(chestLoc);
                    if (mimicChestLocations.add(snapped)) {
                        futures.add(upsertLocationToTable(snapped));
                    }
                }
            }
        }

        // Wait for all database/file writes to finish during shutdown
        if (!futures.isEmpty()) {
            try {
                plugin.getLogger().info("Saving " + futures.size() + " reverted mimic chest location(s) to storage...");
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save mimic chest locations during shutdown", e);
            }
        }

        activeMimics.clear();
        zombieToSlime.clear();
        recentDeathLocations.clear();
    }

    public int getActiveMimicCount() {
        return activeMimics.size();
    }

    /**
     * Handle a player disconnecting — immediately revert any mimics targeting them.
     */
    public void onPlayerQuit(Player player) {
        List<UUID> toRevert = new ArrayList<UUID>();
        for (Map.Entry<UUID, MimicAI> entry : activeMimics.entrySet()) {
            if (player.equals(entry.getValue().getTarget())) {
                toRevert.add(entry.getKey());
            }
        }
        for (UUID uuid : toRevert) {
            revertMimic(uuid);
        }
    }
}
