package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bg52.artifacts.scheduler.PlatformScheduler;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.event.Event;

public class DoubleJumpListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager configManager;
    private PlatformScheduler.TaskHandle groundCheckTask;
    private boolean useModernInput = false;

    // Set of players with available double jumps (for modern input-based detection)
    private final Set<UUID> availableJumps = new HashSet<>();
    private final Set<UUID> hasReleasedJumpKey = new HashSet<>();
    private final Set<UUID> doubleJumpProtected = new HashSet<>();

    public DoubleJumpListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager configManager) {
        this.plugin = plugin;
        this.api = api;
        this.configManager = configManager;

        // Try to detect and register modern PlayerInputEvent and PlayerJumpEvent dynamically
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> playerInputEventClass = (Class<? extends Event>) Class.forName("org.bukkit.event.player.PlayerInputEvent");
            @SuppressWarnings("unchecked")
            Class<? extends Event> playerJumpEventClass = (Class<? extends Event>) Class.forName("com.destroystokyo.paper.event.player.PlayerJumpEvent");

            if (playerInputEventClass != null && playerJumpEventClass != null) {
                registerModernListeners(playerInputEventClass, playerJumpEventClass);
                useModernInput = true;
                plugin.getLogger().info("[Artifacts] Modern PlayerInputEvent and PlayerJumpEvent detected. Using modern input-based double jump!");
            }
        } catch (Throwable t) {
            // Fall back to legacy PlayerToggleFlightEvent
        }

        if (!useModernInput) {
            plugin.getLogger().info("[Artifacts] Falling back to legacy flight-based double jump.");
        }

        this.groundCheckTask = PlatformScheduler.get().runGlobalTimer(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                PlatformScheduler.get().runEntity(player, () -> {
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                        return;
                    }

                    if (player.isOnGround()) {
                        availableJumps.add(player.getUniqueId());
                        hasReleasedJumpKey.remove(player.getUniqueId());
                        if (configManager.isItemEnabled(ArtifactItem.CLOUD_IN_A_BOTTLE.getId()) &&
                                ArtifactUtil.hasEquipped(player, ArtifactItem.CLOUD_IN_A_BOTTLE, api)) {
                            player.setAllowFlight(true);
                        } else if (player.getAllowFlight()) {
                            player.setAllowFlight(false);
                        }
                    }
                });
            }
        }, 4L, 4L);
    }

    private void registerModernListeners(Class<? extends Event> inputClass, Class<? extends Event> jumpClass) {
        // Register PlayerJumpEvent
        plugin.getServer().getPluginManager().registerEvent(
            jumpClass,
            this,
            EventPriority.NORMAL,
            new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) {
                    try {
                        Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                        if (!canDoubleJump(player)) return;
                        availableJumps.add(player.getUniqueId());
                        hasReleasedJumpKey.remove(player.getUniqueId());
                    } catch (Exception e) {
                        // ignore or log
                    }
                }
            },
            plugin
        );

        // Register PlayerInputEvent
        plugin.getServer().getPluginManager().registerEvent(
            inputClass,
            this,
            EventPriority.NORMAL,
            new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) {
                    try {
                        Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                        if (!canDoubleJump(player)) return;

                        UUID playerId = player.getUniqueId();
                        if (player.isOnGround()) {
                            availableJumps.add(playerId);
                            hasReleasedJumpKey.remove(playerId);
                            return;
                        }

                        Object input = event.getClass().getMethod("getInput").invoke(event);
                        boolean isJump = (boolean) input.getClass().getMethod("isJump").invoke(input);

                        if (!isJump) {
                            hasReleasedJumpKey.add(playerId);
                            return;
                        }

                        if (!availableJumps.contains(playerId) || !hasReleasedJumpKey.contains(playerId)) return;

                        availableJumps.remove(playerId);
                        hasReleasedJumpKey.remove(playerId);
                        applyDoubleJump(player);
                    } catch (Exception e) {
                        // ignore or log
                    }
                }
            },
            plugin
        );
    }

    private boolean canDoubleJump(Player player) {
        return player.getGameMode() != GameMode.CREATIVE &&
               player.getGameMode() != GameMode.SPECTATOR &&
               configManager.isItemEnabled(ArtifactItem.CLOUD_IN_A_BOTTLE.getId()) &&
               ArtifactUtil.hasEquipped(player, ArtifactItem.CLOUD_IN_A_BOTTLE, api);
    }

    private void applyDoubleJump(Player player) {
        // Get the player's physical momentum right before jumping
        Vector currentVelocity = player.getVelocity();

        // Calculate their current horizontal speed (ignoring the Y-axis fall speed)
        double horizontalSpeed = Math.sqrt(Math.pow(currentVelocity.getX(), 2) + Math.pow(currentVelocity.getZ(), 2));

        // Default to strictly an upward jump (0.65 vertical)
        Vector jumpVelocity = new Vector(0, 0.65, 0);

        // If the player is actively moving horizontally (buffer of > 0.01 to ignore
        // micro-drifts)
        if (horizontalSpeed > 0.01) {
            double horizontalBoost = 0.15; // standard boost amount

            // Normalize the X and Z movement vectors and apply the boost
            jumpVelocity.setX((currentVelocity.getX() / horizontalSpeed) * horizontalBoost);
            jumpVelocity.setZ((currentVelocity.getZ() / horizontalSpeed) * horizontalBoost);
        }

        // Apply the final computed velocity
        player.setVelocity(jumpVelocity);

        doubleJumpProtected.add(player.getUniqueId());

        if (configManager.isParticlesEnabled()) {
            Location feetLoc = player.getLocation().add(0, 0.15, 0);
            for (int i = 0; i < 25; ++i) {
                double rx = (Math.random() - 0.5) * 0.45;
                double ry = Math.random() * 0.2;
                double rz = (Math.random() - 0.5) * 0.45;
                Location pLoc = feetLoc.clone().add(rx, ry, rz);
                player.getWorld().spawnParticle(Particle.CLOUD, pLoc, 1, 0.0, 0.05, 0.0, 0.02);
                try {
                    player.getWorld().spawnParticle(Particle.POOF, pLoc, 1, 0.0, 0.05, 0.0, 0.02);
                } catch (Throwable ignored) {}
            }
        }

        if (configManager.isSoundsEnabled()) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.6f);
            try {
                player.getWorld().playSound(player.getLocation(), Sound.valueOf("ITEM_ARMOR_EQUIP_ELYTRA"), 0.5f, 1.4f);
            } catch (Throwable ignored) {}
        }
    }

    public void shutdown() {
        if (groundCheckTask != null) {
            groundCheckTask.cancel();
        }
        doubleJumpProtected.clear();
        availableJumps.clear();
        hasReleasedJumpKey.clear();
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

            applyDoubleJump(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            boolean hasBottle = configManager.isItemEnabled(ArtifactItem.CLOUD_IN_A_BOTTLE.getId())
                    && ArtifactUtil.hasEquipped(player, ArtifactItem.CLOUD_IN_A_BOTTLE, api);
            if (hasBottle && (configManager.isCloudInABottleFallDamageImmune() || doubleJumpProtected.contains(player.getUniqueId()))) {
                event.setCancelled(true);
                doubleJumpProtected.remove(player.getUniqueId());
                player.setFallDistance(0.0f);
                if (configManager.isParticlesEnabled()) {
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.1, 0), 12, 0.3, 0.05, 0.3, 0.02);
                }
                if (configManager.isSoundsEnabled()) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOOL_FALL, 0.8f, 1.3f);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        availableJumps.remove(event.getPlayer().getUniqueId());
        doubleJumpProtected.remove(event.getPlayer().getUniqueId());
    }
}