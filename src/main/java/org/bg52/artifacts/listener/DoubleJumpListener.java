package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class DoubleJumpListener implements Listener {

    @SuppressWarnings("unused")
    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager configManager;
    private final BukkitTask groundCheckTask;

    public DoubleJumpListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager configManager) {
        this.plugin = plugin;
        this.api = api;
        this.configManager = configManager;

        this.groundCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }

                    if (player.isOnGround()) {
                        if (configManager.isItemEnabled(ArtifactItem.CLOUD_IN_A_BOTTLE.getId()) &&
                                ArtifactUtil.hasEquipped(player, ArtifactItem.CLOUD_IN_A_BOTTLE, api)) {
                            player.setAllowFlight(true);
                        } else if (player.getAllowFlight()) {
                            player.setAllowFlight(false);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 4L, 4L);
    }

    public void shutdown() {
        if (groundCheckTask != null) {
            groundCheckTask.cancel();
        }
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // If they double jumped with the item
        if (configManager.isItemEnabled(ArtifactItem.CLOUD_IN_A_BOTTLE.getId()) &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.CLOUD_IN_A_BOTTLE, api)) {

            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);

            // Get the player's physical momentum right before jumping
            Vector currentVelocity = player.getVelocity();

            // Calculate their current horizontal speed (ignoring the Y-axis fall speed)
            double horizontalSpeed = Math
                    .sqrt(Math.pow(currentVelocity.getX(), 2) + Math.pow(currentVelocity.getZ(), 2));

            // Default to strictly an upward jump (0.65 vertical)
            Vector jumpVelocity = new Vector(0, 0.65, 0);

            // If the player is actively moving horizontally (buffer of > 0.01 to ignore
            // micro-drifts)
            if (horizontalSpeed > 0.01) {
                double horizontalBoost = 0.15; // Your requested boost amount

                // Normalize the X and Z movement vectors and apply the boost
                jumpVelocity.setX((currentVelocity.getX() / horizontalSpeed) * horizontalBoost);
                jumpVelocity.setZ((currentVelocity.getZ() / horizontalSpeed) * horizontalBoost);
            }

            // Apply the final computed velocity
            player.setVelocity(jumpVelocity);

            // Cloud of white particles where they jumped
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.2, 0), 30, 0.5, 0.1, 0.5,
                    0.05);

            // Minor wind-like sound
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
        }
    }
}