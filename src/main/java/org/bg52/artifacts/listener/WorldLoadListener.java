package org.bg52.artifacts.listener;

import org.bg52.artifacts.manager.CampsitePopulator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldLoadListener implements Listener {

    private final CampsitePopulator populator;

    public WorldLoadListener(CampsitePopulator populator) {
        this.populator = populator;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        // Add the populator to every newly loaded world
        if (!event.getWorld().getPopulators().contains(populator)) {
            event.getWorld().getPopulators().add(populator);
        }
    }
}
