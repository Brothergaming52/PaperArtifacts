package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class StriderShoesListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager configManager;
    private final Map<UUID, Set<Location>> ghostBlocks = new HashMap<>();
    private final BlockData barrierData = Material.BARRIER.createBlockData();

    public StriderShoesListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager configManager) {
        this.plugin = plugin;
        this.api = api;
        this.configManager = configManager;
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(ghostBlocks.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                restoreGhostBlocks(player);
            }
        }
        ghostBlocks.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ghostBlocks.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Only run if the item is enabled
        if (!configManager.isItemEnabled("strider_shoes")) {
            if (ghostBlocks.containsKey(uuid)) {
                restoreGhostBlocks(player);
            }
            return;
        }

        // Check conditions for Strider Shoes - Shifting (Sneaking)
        boolean canWalk = player.isSneaking() && ArtifactUtil.hasEquipped(player, ArtifactItem.STRIDER_SHOES, api);

        if (!canWalk) {
            if (ghostBlocks.containsKey(uuid)) {
                restoreGhostBlocks(player);
            }
            return;
        }

        // Handle Ghost Blocks
        Location center = player.getLocation().clone().subtract(0, 0.5, 0); // Check slightly below feet
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        Set<Location> targetFakes = new HashSet<>();

        // Logic: If there is lava at the player's legs (cy + 1), stop sending fake
        // blocks
        boolean submerged = isLava(center.getWorld().getBlockAt(cx, cy + 1, cz));

        if (!submerged) {
            // Check a 3x1x3 area below the player's feet
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Block b = center.getWorld().getBlockAt(cx + x, cy, cz + z);
                    if (isLava(b)) {
                        targetFakes.add(b.getLocation());
                    }
                }
            }
        }

        Set<Location> currentFakes = ghostBlocks.computeIfAbsent(uuid, k -> new HashSet<>());

        // Restore blocks that are no longer targeted
        Iterator<Location> it = currentFakes.iterator();
        while (it.hasNext()) {
            Location loc = it.next();
            if (!targetFakes.contains(loc)) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
                it.remove();
            }
        }

        // Send new ghost blocks
        for (Location loc : targetFakes) {
            if (!currentFakes.contains(loc)) {
                player.sendBlockChange(loc, barrierData);
                currentFakes.add(loc);
            }
        }

        if (currentFakes.isEmpty()) {
            ghostBlocks.remove(uuid);
        } else {
            // If we are on lava barriers, ensure we don't fall and reset fall distance
            Vector v = player.getVelocity();
            if (v.getY() < 0) {
                player.setFallDistance(0);
            }
        }
    }

    private void restoreGhostBlocks(Player player) {
        Set<Location> locations = ghostBlocks.remove(player.getUniqueId());
        if (locations != null) {
            for (Location loc : locations) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }
    }

    private boolean isLava(Block block) {
        if (block == null)
            return false;
        Material type = block.getType();
        return type == Material.LAVA;
    }

    private boolean isNearLava(Player player) {
        Location center = player.getLocation().clone().subtract(0, 0.5, 0);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // Check legs first (submerged check)
        if (isLava(center.getWorld().getBlockAt(cx, cy + 1, cz)))
            return true;

        // Check 3x3 below
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (isLava(center.getWorld().getBlockAt(cx + x, cy, cz + z))) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleStriderDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();

        EntityDamageEvent.DamageCause cause = event.getCause();

        // Only run if the item is enabled and equipped
        if (!configManager.isItemEnabled("strider_shoes") ||
                !ArtifactUtil.hasEquipped(player, ArtifactItem.STRIDER_SHOES, api)) {
            return;
        }

        // HOT_FLOOR (Magma) is always blocked if equipped
        if (cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setCancelled(true);
            return;
        }

        // Other fire/lava damage is blocked only when sneaking near lava
        if (cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.LAVA) {

            if (player.isSneaking() && isNearLava(player)) {
                event.setCancelled(true);
                player.setFireTicks(0); // Extinguish if protection is active
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStriderCombust(EntityCombustByBlockEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();

        if (!configManager.isItemEnabled("strider_shoes") ||
                !ArtifactUtil.hasEquipped(player, ArtifactItem.STRIDER_SHOES, api)) {
            return;
        }

        // Only block combustion when sneaking near lava (active walker state)
        if (player.isSneaking() && isNearLava(player)) {
            event.setCancelled(true);
        }
    }
}
