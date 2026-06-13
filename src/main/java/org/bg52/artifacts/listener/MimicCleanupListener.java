package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.manager.MimicManager;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Cleanup listener for orphaned mimic entities.
 * On chunk load, removes any PDC-tagged mimic entities
 * that are not tracked by the MimicManager (e.g. from a server restart).
 */
public class MimicCleanupListener implements Listener {

    private final Artifacts plugin;
    private final MimicManager mimicManager;
    private final NamespacedKey mimicKey;

    public MimicCleanupListener(Artifacts plugin, MimicManager mimicManager) {
        this.plugin = plugin;
        this.mimicManager = mimicManager;
        this.mimicKey = new NamespacedKey(plugin, "mimic");
    }

    /**
     * When a chunk loads, check for orphaned mimic entities and remove them.
     * This handles the case where the server restarted with mimic entities saved.
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        for (Entity entity : chunk.getEntities()) {
            // Check for orphaned mimic slimes
            if (entity instanceof Slime) {
                Slime slime = (Slime) entity;
                if (slime.getPersistentDataContainer().has(mimicKey, PersistentDataType.BYTE)) {
                    // If not tracked by manager, it's orphaned
                    if (mimicManager.getMimic(slime.getUniqueId()) == null) {
                        slime.remove();
                        plugin.getLogger().info("Removed orphaned mimic slime at "
                                + slime.getLocation().getBlockX() + ", "
                                + slime.getLocation().getBlockY() + ", "
                                + slime.getLocation().getBlockZ());
                    }
                }
            }

            // Check for orphaned mimic zombies
            if (entity instanceof Zombie) {
                Zombie zombie = (Zombie) entity;
                if (zombie.getPersistentDataContainer().has(mimicKey, PersistentDataType.BYTE)) {
                    if (mimicManager.getMimicByZombie(zombie.getUniqueId()) == null) {
                        zombie.remove();
                        plugin.getLogger().info("Removed orphaned mimic zombie at "
                                + zombie.getLocation().getBlockX() + ", "
                                + zombie.getLocation().getBlockY() + ", "
                                + zombie.getLocation().getBlockZ());
                    }
                }
            }
        }
    }

    /**
     * When a chunk unloads, revert any active mimics in it back to chests.
     * This prevents stale references and stops active mimics from saving to region files.
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Slime) {
                Slime slime = (Slime) entity;
                if (slime.getPersistentDataContainer().has(mimicKey, PersistentDataType.BYTE)) {
                    if (mimicManager.getMimic(slime.getUniqueId()) != null) {
                        mimicManager.revertMimic(slime.getUniqueId());
                    }
                }
            } else if (entity instanceof Zombie) {
                Zombie zombie = (Zombie) entity;
                if (zombie.getPersistentDataContainer().has(mimicKey, PersistentDataType.BYTE)) {
                    org.bg52.artifacts.entity.MimicAI ai = mimicManager.getMimicByZombie(zombie.getUniqueId());
                    if (ai != null) {
                        mimicManager.revertMimic(ai.getMimicEntity().getSlime().getUniqueId());
                    }
                }
            }
        }
    }

    /**
     * When a player disconnects, immediately revert any mimics targeting them.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        mimicManager.onPlayerQuit(event.getPlayer());
    }
}
