package org.bg52.artifacts.manager;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.entity.MimicAI;
import org.bg52.artifacts.entity.MimicAnimator;
import org.bg52.artifacts.entity.MimicEntity;
import org.bukkit.Bukkit;
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

    public MimicManager(Artifacts plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "mimic-chests.yml");
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
        mimicChestLocations.add(toBlockLocation(location));
        saveMimicChests();
    }

    public boolean isMimicChest(Location location) {
        return mimicChestLocations.contains(toBlockLocation(location));
    }

    public void removeMimicChest(Location location) {
        mimicChestLocations.remove(toBlockLocation(location));
        saveMimicChests();
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

    // ─── Persistence ───

    /**
     * Load mimic chest locations from disk.
     */
    private void loadMimicChests() {
        if (!dataFile.exists())
            return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
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
            plugin.getLogger().info("Loaded " + loaded + " mimic chest location(s) from disk.");
        }
    }

    /**
     * Save mimic chest locations to disk.
     */
    private void saveMimicChests() {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> locationList = new ArrayList<Map<String, Object>>();

        synchronized (mimicChestLocations) {
            for (Location loc : mimicChestLocations) {
                if (loc.getWorld() == null)
                    continue;
                Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
                map.put("world", loc.getWorld().getName());
                map.put("x", loc.getBlockX());
                map.put("y", loc.getBlockY());
                map.put("z", loc.getBlockZ());
                locationList.add(map);
            }
        }

        config.set("locations", locationList);

        try {
            plugin.getDataFolder().mkdirs();
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save mimic chest locations", e);
        }
    }

    /**
     * Remove all active mimics. Called on plugin disable.
     * Reverts all active mimics back to chests first, then saves locations.
     */
    public void shutdown() {
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
                    mimicChestLocations.add(toBlockLocation(chestLoc));
                }
            }
        }

        // Save all chest locations (including freshly reverted ones)
        saveMimicChests();

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
