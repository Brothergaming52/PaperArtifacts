package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RootedBootsListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final Map<UUID, Double> pendingFood = new HashMap<>();

    public RootedBootsListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (plugin.getArtifactConfig().isItemEnabled("rooted_boots") &&
                        ArtifactUtil.hasEquipped(player, ArtifactItem.ROOTED_BOOTS, api)) {
                        
                        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                        if (below.getType() == Material.GRASS_BLOCK) {
                            replenishHunger(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // Default interval is 40 ticks, but I'll make it dynamic if needed
        // Note: interval is from config, but changing it requires task restart. 
        // For simplicity I'll use a fixed 20 tick check and compare with last update time or just fixed 40.
    }

    private void replenishHunger(Player player) {
        double speed = plugin.getArtifactConfig().getRootedBootsHungerReplenishSpeed();
        int currentFood = player.getFoodLevel();
        if (currentFood >= 20) return;

        double pending = pendingFood.getOrDefault(player.getUniqueId(), 0.0);
        pending += speed;

        if (pending >= 1.0) {
            int toAdd = (int) pending;
            player.setFoodLevel(Math.min(20, currentFood + toAdd));
            pending -= toAdd;
        }

        pendingFood.put(player.getUniqueId(), pending);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        pendingFood.remove(event.getPlayer().getUniqueId());
    }
}
