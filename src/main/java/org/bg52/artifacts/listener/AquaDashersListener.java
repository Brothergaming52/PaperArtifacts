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
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class AquaDashersListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager configManager;
    private final Map<UUID, Set<Location>> ghostBlocks = new HashMap<>();
    private final BlockData barrierData = Material.BARRIER.createBlockData();

    public AquaDashersListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager configManager) {
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
        if (!configManager.isItemEnabled("aqua_dashers")) {
            if (ghostBlocks.containsKey(uuid)) {
                restoreGhostBlocks(player);
            }
            return;
        }

        // Check conditions for Aqua Dashers
        boolean canDash = player.isSprinting() && ArtifactUtil.hasEquipped(player, ArtifactItem.AQUA_DASHERS, api);

        if (!canDash) {
            if (ghostBlocks.containsKey(uuid)) {
                restoreGhostBlocks(player);
            }
            return;
        }

        // Handle Ghost Blocks
        Location center = player.getLocation().clone().subtract(0, 0.5, 0); // Check slightly below feet
        Set<Location> targetFakes = new HashSet<>();

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // Check a 3x1x3 area below the player's feet
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = center.getWorld().getBlockAt(cx + x, cy, cz + z);
                if (isFluid(b)) {
                    targetFakes.add(b.getLocation());
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

    private boolean isFluid(Block block) {
        if (block == null)
            return false;
        Material type = block.getType();

        // Includes water and lava (as requested by "fluids")
        if (type == Material.WATER || type == Material.LAVA)
            return true;

        // Check for waterlogged blocks
        if (block.getBlockData() instanceof Waterlogged) {
            return ((Waterlogged) block.getBlockData()).isWaterlogged();
        }

        // Include water vegetation
        return type == Material.SEAGRASS || type == Material.TALL_SEAGRASS ||
                type == Material.KELP || type == Material.KELP_PLANT;
    }
}
