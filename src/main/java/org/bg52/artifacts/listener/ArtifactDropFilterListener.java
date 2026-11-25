package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

/**
 * Drop & Loot Filter: Prevents disabled artifacts from dropping as items upon entity death.
 */
public class ArtifactDropFilterListener implements Listener {

    private final Artifacts plugin;

    public ArtifactDropFilterListener(Artifacts plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getDrops() == null || event.getDrops().isEmpty()) {
            return;
        }

        CuriosPaperAPI api = plugin.getItemRegistry() != null ? plugin.getItemRegistry().getCuriosPaperApi() : null;
        Iterator<ItemStack> iterator = event.getDrops().iterator();

        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            ArtifactItem artifact = ArtifactUtil.getArtifactItem(drop, api);
            if (artifact != null && !plugin.getArtifactConfig().isItemEnabled(artifact.getId())) {
                iterator.remove();
            }
        }
    }
}
