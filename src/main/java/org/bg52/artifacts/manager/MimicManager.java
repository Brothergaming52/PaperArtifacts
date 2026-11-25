package org.bg52.artifacts.manager;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.entity.MimicEntityDefinition;
import org.bg52.synthetix.api.SynthetixAPI;
import org.bg52.synthetix.api.event.SynthetixEntityDeathEvent;
import org.bg52.synthetix.api.event.SynthetixEntitySpawnEvent;
import org.bg52.synthetix.entity.runtime.CustomEntityInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.bg52.artifacts.scheduler.PlatformScheduler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Super-modular manager for Mimic entities and mimic chest locations.
 *
 * Integrates with Synthetix CustomEntityEngine:
 * - Spawns Mimic as a CustomEntityDefinition with virtual skeletal rig and sub-voxel hitboxes.
 * - Manages chest conversion, reversion timers, and world persistence.
 */
public class MimicManager implements Listener {

    private final Artifacts plugin;

    // Active mimics indexed by driver mob UUID
    private final Map<UUID, CustomEntityInstance> activeMimics = new ConcurrentHashMap<>();

    // Map tracking idle ticks for active mimics (reverts to chest if no player nearby for 5 seconds)
    private final Map<UUID, Integer> noTargetTicks = new ConcurrentHashMap<>();

    // Locations of placed mimic chests (not yet opened)
    private final Set<Location> mimicChestLocations = Collections.synchronizedSet(new HashSet<>());

    // Persistence file for mimic chest locations
    private final File dataFile;
    private PlatformScheduler.TaskHandle monitorTask;

    public MimicManager(Artifacts plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "mimic-chests.yml");

        // Register the Mimic custom entity definition into Synthetix
        try {
            if (SynthetixAPI.getOptional().isPresent()) {
                SynthetixAPI.get().getCustomEntityRegistry().register(MimicEntityDefinition.getDefinition());
                plugin.getLogger().info("[Mimic] Registered Mimic CustomEntityDefinition with Synthetix!");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Mimic] Could not register Mimic definition with Synthetix: " + t.getMessage());
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadMimicChests();
        startMonitorTask();
    }

    @EventHandler
    public void onCustomEntitySpawn(SynthetixEntitySpawnEvent event) {
        CustomEntityInstance instance = event.getEntityInstance();
        if (instance != null && "mimic".equalsIgnoreCase(instance.getDefinition().getId())) {
            if (instance.getDriverMob() != null) {
                UUID driverId = instance.getDriverMob().getUniqueId();
                activeMimics.put(driverId, instance);
                noTargetTicks.put(driverId, 0);
            }
        }
    }

    @EventHandler
    public void onCustomEntityDeath(SynthetixEntityDeathEvent event) {
        CustomEntityInstance instance = event.getEntityInstance();
        if (instance != null && "mimic".equalsIgnoreCase(instance.getDefinition().getId())) {
            if (instance.getDriverMob() != null) {
                UUID driverId = instance.getDriverMob().getUniqueId();
                activeMimics.remove(driverId);
                noTargetTicks.remove(driverId);
            }
        }
    }

    private void startMonitorTask() {
        this.monitorTask = PlatformScheduler.get().runGlobalTimer(() -> {
            if (activeMimics.isEmpty()) return;

            for (Map.Entry<UUID, CustomEntityInstance> entry : new ArrayList<>(activeMimics.entrySet())) {
                UUID id = entry.getKey();
                CustomEntityInstance instance = entry.getValue();

                if (instance == null || instance.isDespawned() || instance.getDriverMob() == null || !instance.getDriverMob().isValid()) {
                    activeMimics.remove(id);
                    noTargetTicks.remove(id);
                    continue;
                }

                LivingEntity mob = instance.getDriverMob();
                Location loc = mob.getLocation();

                // Check for nearby survival/adventure players within 16 blocks (or creative player targeting this mimic)
                boolean playerNear = false;
                if (loc.getWorld() != null) {
                    for (Player player : loc.getWorld().getPlayers()) {
                        if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                            if (player.getLocation().distanceSquared(loc) <= 256.0) { // 16 blocks
                                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                                    if (mob instanceof org.bukkit.entity.Mob m && m.getTarget() != null && m.getTarget().equals(player)) {
                                        playerNear = true;
                                        break;
                                    }
                                } else {
                                    playerNear = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!playerNear) {
                    int ticks = noTargetTicks.getOrDefault(id, 0) + 20;
                    noTargetTicks.put(id, ticks);

                    // 100 ticks = 5 seconds with no players nearby -> revert to chest!
                    if (ticks >= 100) {
                        revertMimic(id);
                    }
                } else {
                    noTargetTicks.put(id, 0);
                }
            }
        }, 20L, 20L);
    }

    /**
     * Spawn a new mimic at the given location.
     */
    public CustomEntityInstance spawnMimic(Location location) {
        return spawnMimic(location, null);
    }

    /**
     * Spawn a new mimic at the given location targeting an initial player.
     */
    public CustomEntityInstance spawnMimic(Location location, Player initialTarget) {
        if (!SynthetixAPI.getOptional().isPresent()) {
            plugin.getLogger().severe("[Mimic] Cannot spawn Mimic: Synthetix API not available!");
            return null;
        }

        CustomEntityInstance instance = SynthetixAPI.get().getCustomEntityManager().spawnEntity(
                MimicEntityDefinition.getDefinition(), location
        );

        if (instance == null) {
            plugin.getLogger().warning("[Mimic] Failed to spawn CustomEntityInstance for Mimic!");
            return null;
        }

        UUID driverId = instance.getDriverMob().getUniqueId();
        activeMimics.put(driverId, instance);
        noTargetTicks.put(driverId, 0);

        if (initialTarget != null && instance.getDriverMob() instanceof org.bukkit.entity.Mob mob) {
            mob.setTarget(initialTarget);
        }

        return instance;
    }

    public static final NamespacedKey MIMIC_CHEST_PDC = new NamespacedKey("artifacts", "is_mimic");

    public void tagMimicChestPdc(Block block) {
        if (block != null && block.getState() instanceof org.bukkit.block.TileState tileState) {
            tileState.getPersistentDataContainer().set(MIMIC_CHEST_PDC, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            tileState.update(true, false);
        }
    }

    public void clearMimicChestPdc(Block block) {
        if (block != null && block.getState() instanceof org.bukkit.block.TileState tileState) {
            tileState.getPersistentDataContainer().remove(MIMIC_CHEST_PDC);
            tileState.update(true, false);
        }
    }

    public boolean isPdcMimicChest(Block block) {
        if (block != null && block.getState() instanceof org.bukkit.block.TileState tileState) {
            return tileState.getPersistentDataContainer().has(MIMIC_CHEST_PDC, org.bukkit.persistence.PersistentDataType.BYTE);
        }
        return false;
    }

    /**
     * Revert an active mimic back to a chest block and re-register the location as a mimic chest.
     */
    public void revertMimic(UUID driverUuid) {
        CustomEntityInstance instance = activeMimics.remove(driverUuid);
        noTargetTicks.remove(driverUuid);

        if (instance != null && instance.getDriverMob() != null && instance.getDriverMob().isValid()) {
            Location loc = instance.getDriverMob().getLocation();

            // Clean up any passengers (Bedrock proxy ArmorStands) before despawning driver mob
            for (Entity pass : new ArrayList<>(instance.getDriverMob().getPassengers())) {
                if (pass instanceof org.bukkit.entity.ArmorStand) {
                    pass.remove();
                }
            }

            instance.despawn();

            // Revert ground block to chest with safe placement on region thread
            PlatformScheduler.get().runLocation(loc, () -> {
                Block placed = findSafePlacement(loc);
                if (placed != null) {
                    placed.setType(Material.CHEST);
                    tagMimicChestPdc(placed);
                    placed.getWorld().playSound(placed.getLocation(), "artifacts:entity.mimic.close", 1.0f, 1.0f);
                    addMimicChest(placed.getLocation());
                } else {
                    Block origin = loc.getBlock();
                    origin.setType(Material.CHEST);
                    tagMimicChestPdc(origin);
                    origin.getWorld().playSound(origin.getLocation(), "artifacts:entity.mimic.close", 1.0f, 1.0f);
                    addMimicChest(origin.getLocation());
                }
            });
        }
    }

    private Block findSafePlacement(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        Block origin = loc.getBlock();
        int originY = origin.getY();
        int maxY = loc.getWorld().getMaxHeight() - 1;
        int minY = loc.getWorld().getMinHeight();

        // 1. Check origin block first
        if (isBlockSafeForChest(origin)) {
            return origin;
        }

        // 2. Check 1 block down
        if (originY > minY) {
            Block down = origin.getRelative(BlockFace.DOWN);
            if (isBlockSafeForChest(down)) {
                return down;
            }
        }

        // 3. Check 1 block up
        if (originY < maxY) {
            Block up = origin.getRelative(BlockFace.UP);
            if (isBlockSafeForChest(up)) {
                return up;
            }
        }

        return origin;
    }

    private boolean isBlockSafeForChest(Block block) {
        if (!block.getType().isAir() && block.getType() != Material.SHORT_GRASS && block.getType() != Material.TALL_GRASS) {
            return false;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        return below.getType().isSolid();
    }

    /**
     * Remove an active mimic by driver UUID.
     */
    public void removeMimic(UUID driverUuid) {
        CustomEntityInstance instance = activeMimics.remove(driverUuid);
        noTargetTicks.remove(driverUuid);
        if (instance != null) {
            instance.despawn();
        }
    }

    public CustomEntityInstance getMimic(UUID driverUuid) {
        return activeMimics.get(driverUuid);
    }

    public boolean isMimicEntity(Entity entity) {
        if (entity == null) return false;
        if (activeMimics.containsKey(entity.getUniqueId())) return true;
        if (SynthetixAPI.getOptional().isPresent()) {
            CustomEntityInstance instance = SynthetixAPI.get().getCustomEntity(entity);
            if (instance != null && "mimic".equalsIgnoreCase(instance.getDefinition().getId())) {
                return true;
            }
        }
        return false;
    }

    // ─── Mimic Chest Location Tracking ───

    public void addMimicChest(Location location) {
        Location snapped = toBlockLocation(location);
        if (snapped.getBlock() != null && snapped.getBlock().getType() == Material.CHEST) {
            tagMimicChestPdc(snapped.getBlock());
        }
        if (mimicChestLocations.add(snapped)) {
            saveMimicChests();
        }
    }

    public boolean isMimicChest(Location location) {
        if (location == null) return false;
        Location snapped = toBlockLocation(location);
        if (mimicChestLocations.contains(snapped)) {
            return true;
        }
        if (snapped.getBlock() != null && snapped.getBlock().getType() == Material.CHEST && isPdcMimicChest(snapped.getBlock())) {
            mimicChestLocations.add(snapped);
            return true;
        }
        return false;
    }

    public void removeMimicChest(Location location) {
        Location snapped = toBlockLocation(location);
        if (snapped.getBlock() != null && snapped.getBlock().getType() == Material.CHEST) {
            clearMimicChestPdc(snapped.getBlock());
        }
        if (mimicChestLocations.remove(snapped)) {
            saveMimicChests();
        }
    }

    private Location toBlockLocation(Location location) {
        return new Location(location.getWorld(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    // ─── Persistence ───

    private void loadMimicChests() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        List<?> locations = config.getList("locations");
        if (locations == null) return;

        int loaded = 0;
        for (Object obj : locations) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                String worldName = (String) map.get("world");
                if (worldName == null) continue;
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                int x = ((Number) map.get("x")).intValue();
                int y = ((Number) map.get("y")).intValue();
                int z = ((Number) map.get("z")).intValue();
                mimicChestLocations.add(new Location(world, x, y, z));
                loaded++;
            }
        }

        if (loaded > 0) {
            plugin.getLogger().info("Loaded " + loaded + " mimic chest location(s) from mimic-chests.yml.");
        }
    }

    public synchronized void saveMimicChests() {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();

        synchronized (mimicChestLocations) {
            for (Location loc : mimicChestLocations) {
                if (loc.getWorld() == null) continue;
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("world", loc.getWorld().getName());
                map.put("x", loc.getBlockX());
                map.put("y", loc.getBlockY());
                map.put("z", loc.getBlockZ());
                list.add(map);
            }
        }

        config.set("locations", list);
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save mimic-chests.yml", e);
        }
    }

    public void shutdown() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }

        // Revert all active mimics to chests so they survive the restart
        List<UUID> toRevert = new ArrayList<>(activeMimics.keySet());
        for (UUID id : toRevert) {
            revertMimic(id);
        }

        saveMimicChests();
        activeMimics.clear();
        noTargetTicks.clear();
    }

    public int getActiveMimicCount() {
        return activeMimics.size();
    }

    public void onPlayerQuit(Player player) {
        List<UUID> toRevert = new ArrayList<>();
        for (Map.Entry<UUID, CustomEntityInstance> entry : activeMimics.entrySet()) {
            if (entry.getValue().getDriverMob() instanceof org.bukkit.entity.Mob mob) {
                if (player.equals(mob.getTarget())) {
                    toRevert.add(entry.getKey());
                }
            }
        }
        for (UUID uuid : toRevert) {
            revertMimic(uuid);
        }
    }
}
