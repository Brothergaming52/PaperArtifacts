package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.manager.MimicManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listens for player interaction with mimic chests.
 * Right-clicking a mimic chest breaks it and spawns the mimic entity.
 * Mining a mimic chest is prevented.
 */
public class MimicChestListener implements Listener {

    private final Artifacts plugin;
    private final MimicManager mimicManager;

    public MimicChestListener(Artifacts plugin, MimicManager mimicManager) {
        this.plugin = plugin;
        this.mimicManager = mimicManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;
        if (block.getType() != Material.CHEST)
            return;

        if (!mimicManager.isMimicChest(block.getLocation()))
            return;

        // Cancel the chest opening
        event.setCancelled(true);

        // Remove the chest tracking
        mimicManager.removeMimicChest(block.getLocation());

        Location spawnLoc = block.getLocation().add(0.5, 0, 0.5);

        // Break the chest block
        block.setType(Material.AIR);

        // Play custom mimic open sound
        spawnLoc.getWorld().playSound(spawnLoc, "artifacts:entity.mimic.open", org.bukkit.SoundCategory.HOSTILE, 1.0f,
                1.0f);

        try {
            // Try BLOCK_CRACK first (1.14-1.20.4), fallback to BLOCK (1.20.5+)
            Particle blockParticle = getBlockParticle();
            if (blockParticle != null) {
                spawnLoc.getWorld().spawnParticle(blockParticle,
                        spawnLoc.clone().add(0, 0.5, 0), 30, 0.3, 0.3, 0.3, 0.1,
                        Material.CHEST.createBlockData());
            }
        } catch (Throwable ignored) {
        }

        // Spawn the mimic
        mimicManager.spawnMimic(spawnLoc);

        plugin.getLogger().info("Mimic spawned at " + spawnLoc.getBlockX() + ", "
                + spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ()
                + " triggered by " + event.getPlayer().getName());
    }

    /**
     * Prevent players from mining a mimic chest.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST)
            return;

        if (mimicManager.isMimicChest(block.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7eThis chest feels... odd.");
        }
    }

    /**
     * Get the correct Particle for block breaking across versions.
     * BLOCK_CRACK (1.14-1.20.4) was renamed to BLOCK (1.20.5+).
     */
    private static Particle getBlockParticle() {
        for (Particle p : Particle.values()) {
            String name = p.name();
            if (name.equals("BLOCK_CRACK") || name.equals("BLOCK")) {
                return p;
            }
        }
        return null;
    }
}
