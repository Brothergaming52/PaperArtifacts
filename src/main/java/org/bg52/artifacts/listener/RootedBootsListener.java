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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bg52.artifacts.scheduler.PlatformScheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RootedBootsListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final Map<UUID, Double> pendingFood = new HashMap<>();
    private PlatformScheduler.TaskHandle task;

    public RootedBootsListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
        startTask();
    }

    private void startTask() {
        this.task = PlatformScheduler.get().runGlobalTimer(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                PlatformScheduler.get().runEntity(player, () -> {
                    if (plugin.getArtifactConfig().isItemEnabled("rooted_boots") &&
                        ArtifactUtil.hasEquipped(player, ArtifactItem.ROOTED_BOOTS, api)) {
                        
                        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                        if (below.getType() == Material.GRASS_BLOCK) {
                            replenishHunger(player);
                        }
                    }
                });
            }
        }, 40L, 40L);
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

            if (plugin.getArtifactConfig().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.COMPOSTER, player.getLocation().add(0.0, 0.2, 0.0), 8, 0.3, 0.2, 0.3, 0.02);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0, 0.2, 0.0), 5, 0.3, 0.2, 0.3, 0.0);
            }
            if (plugin.getArtifactConfig().isSoundsEnabled()) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 0.6f, 1.6f);
            }
        }

        pendingFood.put(player.getUniqueId(), pending);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        pendingFood.remove(event.getPlayer().getUniqueId());
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        pendingFood.clear();
    }
}
