package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.manager.MimicManager;
import org.bg52.synthetix.entity.runtime.CustomEntityInstance;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Cleanup listener for active Mimic entities on chunk unload or player disconnect.
 */
public class MimicCleanupListener implements Listener {

    private final Artifacts plugin;
    private final MimicManager mimicManager;

    public MimicCleanupListener(Artifacts plugin, MimicManager mimicManager) {
        this.plugin = plugin;
        this.mimicManager = mimicManager;
    }

    /**
     * When a chunk unloads, revert any active mimics located within it back to chests.
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (mimicManager.isMimicEntity(entity)) {
                mimicManager.revertMimic(entity.getUniqueId());
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
