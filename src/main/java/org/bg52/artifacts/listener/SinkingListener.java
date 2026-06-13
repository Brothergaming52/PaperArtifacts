package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

public class SinkingListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager configManager;
    private final Map<UUID, SinkingTask> activeTasks = new HashMap<>();
    private final Map<UUID, Double> airborneFallDistances = new HashMap<>();

    public SinkingListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager configManager) {
        this.plugin = plugin;
        this.api = api;
        this.configManager = configManager;
    }

    public void shutdown() {
        for (SinkingTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        airborneFallDistances.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only run if the item is enabled
        if (!configManager.isItemEnabled(ArtifactItem.CHARM_OF_SINKING.getId())
                && !ArtifactUtil.hasEquipped(player, ArtifactItem.CHARM_OF_SINKING, api))
            return;

        boolean inWater = isInWater(player);

        if (!inWater) {
            if (player.getFallDistance() > 0) {
                airborneFallDistances.put(player.getUniqueId(), (double) player.getFallDistance());
            } else {
                airborneFallDistances.remove(player.getUniqueId());
            }
            return;
        }

        if (inWater) {
            if (!activeTasks.containsKey(player.getUniqueId())
                    && ArtifactUtil.hasEquipped(player, ArtifactItem.CHARM_OF_SINKING, api)) {

                double initialFall = player.getFallDistance();
                if (initialFall <= 0 && airborneFallDistances.containsKey(player.getUniqueId())) {
                    initialFall = airborneFallDistances.get(player.getUniqueId());
                }
                airborneFallDistances.remove(player.getUniqueId());

                SinkingTask task = new SinkingTask(player, initialFall);
                activeTasks.put(player.getUniqueId(), task);
                task.runTaskTimer(plugin, 1L, 1L);
            }
        }
    }

    private boolean isInWater(Player player) {
        Block feet = player.getLocation().getBlock();
        Block head = player.getEyeLocation().getBlock();
        return isWater(feet) || isWater(head);
    }

    private boolean isWater(Block block) {
        if (block == null)
            return false;
        Material type = block.getType();
        if (type == Material.WATER)
            return true;
        if (block.getBlockData() instanceof Waterlogged) {
            return ((Waterlogged) block.getBlockData()).isWaterlogged();
        }
        return type == Material.SEAGRASS || type == Material.TALL_SEAGRASS || type == Material.KELP
                || type == Material.KELP_PLANT;
    }

    private class SinkingTask extends BukkitRunnable {
        private final Player player;
        private double fallDistance = 0;
        private double lastY;
        private boolean wasOnGround = true;

        private final Set<Location> ghostBlocks = new HashSet<>();

        public SinkingTask(Player player, double initialFall) {
            this.player = player;
            this.fallDistance = initialFall > 0 ? initialFall : player.getFallDistance();
            this.lastY = player.getLocation().getY();
        }

        private void restoreGhostBlocks() {
            for (Location loc : ghostBlocks) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
            ghostBlocks.clear();
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead() || !isInWater(player)
                    || !ArtifactUtil.hasEquipped(player, ArtifactItem.CHARM_OF_SINKING, api)
                    || !configManager.isItemEnabled(ArtifactItem.CHARM_OF_SINKING.getId())) {
                restoreGhostBlocks();
                activeTasks.remove(player.getUniqueId());
                this.cancel();
                return;
            }

            // FAKE AIR BLOCKS around the player to trick the client physics engine
            Location center = player.getLocation();
            Set<Location> targetFakes = new HashSet<>();

            int cx = center.getBlockX();
            int cy = center.getBlockY();
            int cz = center.getBlockZ();

            // Check a 3x3x3 area around the player
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block b = center.getWorld().getBlockAt(cx + x, cy + y, cz + z);
                        if (isWater(b)) {
                            targetFakes.add(b.getLocation());
                        }
                    }
                }
            }

            // Restore blocks that are no longer in our 3x3x3 bubble
            Iterator<Location> it = ghostBlocks.iterator();
            while (it.hasNext()) {
                Location loc = it.next();
                if (!targetFakes.contains(loc)) {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                    it.remove();
                }
            }

            // Send air for new blocks
            BlockData airData = Material.AIR.createBlockData();
            for (Location loc : targetFakes) {
                if (!ghostBlocks.contains(loc)) {
                    Block b = loc.getBlock();
                    if (b.getType() == Material.WATER || b.getType() == Material.SEAGRASS
                            || b.getType() == Material.TALL_SEAGRASS || b.getType() == Material.KELP
                            || b.getType() == Material.KELP_PLANT) {
                        player.sendBlockChange(loc, airData);
                    } else if (b.getBlockData() instanceof Waterlogged) {
                        Waterlogged wl = (Waterlogged) b.getBlockData().clone();
                        wl.setWaterlogged(false);
                        player.sendBlockChange(loc, wl);
                    }
                    ghostBlocks.add(loc);
                }
            }

            // SERVER-SIDE FALL DAMAGE TRACKING
            double currentY = player.getLocation().getY();

            if (!player.isOnGround()) {
                if (currentY < lastY) {
                    // Player is falling down: accumulate distance
                    fallDistance += (lastY - currentY);
                } else if (currentY > lastY + 0.1) {
                    // Player actually moved UP (swimming up or jumping)
                    // The +0.1 buffer ignores tiny client-side water bounces
                    fallDistance = 0;
                }
                // Note: If currentY == lastY (network jitter), we do nothing.
                // This safely preserves the fallDistance mid-air.

                wasOnGround = false;
            } else {
                if (!wasOnGround) {
                    if (fallDistance > 3.0) {
                        double damage = fallDistance - 3.0;

                        EntityDamageEvent damageEvent = new EntityDamageEvent(player,
                                EntityDamageEvent.DamageCause.FALL, damage);
                        plugin.getServer().getPluginManager().callEvent(damageEvent);

                        if (!damageEvent.isCancelled()) {
                            player.setLastDamageCause(damageEvent);
                            // Passing this back into player.damage() will natively trigger armor/enchant
                            // reductions
                            player.damage(damageEvent.getFinalDamage());
                        }
                    }
                    fallDistance = 0;
                    wasOnGround = true;
                }
            }
            lastY = currentY;
        }
    }
}
