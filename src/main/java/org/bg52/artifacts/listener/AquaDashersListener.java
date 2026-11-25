package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class AquaDashersListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager configManager;

    public AquaDashersListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager configManager) {
        this.plugin = plugin;
        this.api = api;
        this.configManager = configManager;
    }

    public void shutdown() {
        // No persistent or fake block state to clean up
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only run if the item is enabled
        if (!configManager.isItemEnabled("aqua_dashers")) {
            return;
        }

        // Check conditions for Aqua Dashers: must be sprinting and have item equipped
        boolean canDash = player.isSprinting() && ArtifactUtil.hasEquipped(player, ArtifactItem.AQUA_DASHERS, api);

        // Break surface tension if falling from high up
        if (!canDash || player.getFallDistance() > 5.0f) {
            return;
        }

        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;

        Double surfaceY = getWaterSurfaceY(to);
        if (surfaceY == null) {
            return;
        }

        // If player is at/near water surface, clamp them smoothly onto the water surface
        // without using any fake barrier blocks
        if (to.getY() < surfaceY && to.getY() >= surfaceY - 0.7) {
            to.setY(surfaceY);
            event.setTo(to);

            Vector v = player.getVelocity();
            if (v.getY() < 0) {
                v.setY(0);
                player.setVelocity(v);
            }
            player.setFallDistance(0);

            // Water surface dash effects
            if (configManager.isParticlesEnabled()) {
                Location particleLoc = new Location(to.getWorld(), to.getX(), surfaceY, to.getZ());
                to.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 4, 0.2, 0.05, 0.2, 0.05);
                to.getWorld().spawnParticle(Particle.BUBBLE, particleLoc, 2, 0.15, 0.05, 0.15, 0.02);
            }
            if (configManager.isSoundsEnabled() && Math.random() < 0.25) {
                to.getWorld().playSound(to, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.25f, 1.4f);
            }
        } else if (to.getY() >= surfaceY && to.getY() <= surfaceY + 0.1) {
            // Player is running right at the surface
            player.setFallDistance(0);
            if (configManager.isParticlesEnabled() && Math.random() < 0.5) {
                Location particleLoc = new Location(to.getWorld(), to.getX(), surfaceY, to.getZ());
                to.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 2, 0.15, 0.05, 0.15, 0.05);
            }
        }
    }

    private Double getWaterSurfaceY(Location loc) {
        if (loc.getWorld() == null) return null;
        int bx = loc.getBlockX();
        int bz = loc.getBlockZ();
        int by = loc.getBlockY();

        // Check feet and adjacent blocks to locate topmost water surface
        for (int checkY = by + 1; checkY >= by - 1; checkY--) {
            Block block = loc.getWorld().getBlockAt(bx, checkY, bz);
            if (isWater(block)) {
                Block above = loc.getWorld().getBlockAt(bx, checkY + 1, bz);
                if (!isWater(above) && !above.getType().isSolid()) {
                    return (double) (checkY + 1);
                }
            }
        }
        return null;
    }

    private boolean isWater(Block block) {
        if (block == null)
            return false;
        Material type = block.getType();

        // Strictly water - NO lava!
        if (type == Material.WATER)
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
