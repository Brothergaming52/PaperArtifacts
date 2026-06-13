package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class SnowshoesListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager config;
    private final Set<Material> iceMaterials = new HashSet<>();
    private final java.util.Map<java.util.UUID, Set<Block>> ghostBlocks = new java.util.HashMap<>();
    private Material powderSnowMaterial = null;

    public SnowshoesListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager config) {
        this.plugin = plugin;
        this.api = api;
        this.config = config;

        iceMaterials.add(Material.ICE);
        iceMaterials.add(Material.PACKED_ICE);
        try {
            iceMaterials.add(Material.BLUE_ICE);
        } catch (NoSuchFieldError ignored) {
        }
        try {
            iceMaterials.add(Material.FROSTED_ICE);
        } catch (NoSuchFieldError ignored) {
        }
        try {
            powderSnowMaterial = Material.valueOf("POWDER_SNOW");
        } catch (IllegalArgumentException | NoSuchFieldError ignored) {
        }
    }

    public void shutdown() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            clearGhostBlocks(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updateSnowshoesState(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearGhostBlocks(event.getPlayer());
    }

    @EventHandler
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();
        if (ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.SNOWSHOES) ||
                ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.SNOWSHOES)) {

            // Run on next tick to ensure hasEquipped is accurate
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    updateSnowshoesState(player);
                }
            });
        }
    }

    private void updateSnowshoesState(Player player) {
        boolean enabled = config.isItemEnabled("snowshoes") &&
                config.isSnowshoesPowderSnowImmune() &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.SNOWSHOES, api);

        if (!enabled) {
            clearGhostBlocks(player);
        }
    }

    private void clearGhostBlocks(Player player) {
        Set<Block> blocks = ghostBlocks.remove(player.getUniqueId());
        if (blocks != null) {
            for (Block b : blocks) {
                player.sendBlockChange(b.getLocation(), b.getBlockData());
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        boolean hasSnowshoes = config.isItemEnabled("snowshoes")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.SNOWSHOES, api);

        // Handle Powder Snow ghost blocks to prevent sinking
        if (hasSnowshoes && config.isSnowshoesPowderSnowImmune() && powderSnowMaterial != null) {
            Block feet = player.getLocation().getBlock();
            Block below = feet.getRelative(BlockFace.DOWN);

            Set<Block> playerGhostBlocks = ghostBlocks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
            Set<Block> newDetected = new HashSet<>();

            if (below.getType() == powderSnowMaterial) {
                newDetected.add(below);
                player.sendBlockChange(below.getLocation(), Material.SNOW_BLOCK.createBlockData());
            }
            if (feet.getType() == powderSnowMaterial) {
                newDetected.add(feet);
                player.sendBlockChange(feet.getLocation(), Material.SNOW_BLOCK.createBlockData());
            }

            // Revert blocks no longer near
            java.util.Iterator<Block> it = playerGhostBlocks.iterator();
            while (it.hasNext()) {
                Block b = it.next();
                if (!newDetected.contains(b)) {
                    player.sendBlockChange(b.getLocation(), b.getBlockData());
                    it.remove();
                }
            }
            playerGhostBlocks.addAll(newDetected);
        } else {
            clearGhostBlocks(player);
        }

        // Horizontal move check for ice logic
        if (event.getFrom().getX() == event.getTo().getX() && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }

        if (!hasSnowshoes)
            return;

        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (iceMaterials.contains(below.getType())) {
            double reduction = config.getSnowshoesIceSlipperinessReduction();
            if (reduction <= 0)
                return;

            Vector vel = player.getVelocity();
            // Only apply if they are moving horizontally
            if (Math.abs(vel.getX()) > 0.01 || Math.abs(vel.getZ()) > 0.01) {
                // If reduction is 1.0 (full reduction), we want it to feel like stone.
                // Ice slipperiness is usually very high. Multiplying horizontal velocity
                // by a damping factor on every move reduces the "sliding" distance.
                double factor = 1.0 - (reduction * 0.25); // Max 25% damping per move event
                player.setVelocity(vel.setX(vel.getX() * factor).setZ(vel.getZ() * factor));
            }
        }
    }
}
