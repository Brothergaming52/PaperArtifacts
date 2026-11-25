package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class StriderShoesListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager configManager;

    public StriderShoesListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager configManager) {
        this.plugin = plugin;
        this.api = api;
        this.configManager = configManager;
    }

    public void shutdown() {
        // No persistent or fake block state to clean up
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only run if the item is enabled
        if (!configManager.isItemEnabled("strider_shoes")) {
            return;
        }

        // Check conditions for Strider Shoes - Sneaking
        boolean canWalk = player.isSneaking() && ArtifactUtil.hasEquipped(player, ArtifactItem.STRIDER_SHOES, api);

        // Break surface tension if falling from high up
        if (!canWalk || player.getFallDistance() > 5.0f) {
            return;
        }

        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;

        Double surfaceY = getLavaSurfaceY(to);
        if (surfaceY == null) {
            return;
        }

        // If player is at/near lava surface, clamp them smoothly onto the lava surface
        // without using any fake barrier blocks
        if (to.getY() < surfaceY && to.getY() >= surfaceY - 0.7) {
            to.setY(surfaceY);
            event.setTo(to);

            Vector v = player.getVelocity();
            if (v.getY() < 0) {
                v.setY(0);
                player.setVelocity(v);
            }
            player.setFallDistance(0);

            // Lava surface walk effects
            if (configManager.isParticlesEnabled()) {
                Location particleLoc = new Location(to.getWorld(), to.getX(), surfaceY, to.getZ());
                to.getWorld().spawnParticle(Particle.LAVA, particleLoc, 1, 0.2, 0.05, 0.2, 0.0);
                to.getWorld().spawnParticle(Particle.FLAME, particleLoc, 2, 0.2, 0.05, 0.2, 0.02);
            }
            if (configManager.isSoundsEnabled() && Math.random() < 0.15) {
                to.getWorld().playSound(to, Sound.BLOCK_LAVA_POP, 0.5f, 1.2f);
            }
        } else if (to.getY() >= surfaceY && to.getY() <= surfaceY + 0.1) {
            player.setFallDistance(0);
            if (configManager.isParticlesEnabled() && Math.random() < 0.3) {
                Location particleLoc = new Location(to.getWorld(), to.getX(), surfaceY, to.getZ());
                to.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.15, 0.05, 0.15, 0.01);
            }
        }
    }

    private Double getLavaSurfaceY(Location loc) {
        if (loc.getWorld() == null) return null;
        int bx = loc.getBlockX();
        int bz = loc.getBlockZ();
        int by = loc.getBlockY();

        for (int checkY = by + 1; checkY >= by - 1; checkY--) {
            Block block = loc.getWorld().getBlockAt(bx, checkY, bz);
            if (isLava(block)) {
                Block above = loc.getWorld().getBlockAt(bx, checkY + 1, bz);
                if (!isLava(above) && !above.getType().isSolid()) {
                    return (double) (checkY + 1);
                }
            }
        }
        return null;
    }

    private boolean isLava(Block block) {
        if (block == null)
            return false;
        return block.getType() == Material.LAVA;
    }

    private boolean isNearLava(Player player) {
        Location center = player.getLocation();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (isLava(center.getWorld().getBlockAt(cx + dx, cy + dy, cz + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleStriderDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        EntityDamageEvent.DamageCause cause = event.getCause();

        // Only run if the item is enabled and equipped
        if (!configManager.isItemEnabled("strider_shoes") ||
                !ArtifactUtil.hasEquipped(player, ArtifactItem.STRIDER_SHOES, api)) {
            return;
        }

        // HOT_FLOOR (Magma) is always blocked if equipped
        if (cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setCancelled(true);
            return;
        }

        // Other fire/lava damage is blocked when sneaking near lava or walking on lava
        if (cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.LAVA) {

            if (player.isSneaking() && isNearLava(player)) {
                event.setCancelled(true);
                player.setFireTicks(0); // Extinguish if protection is active
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStriderCombust(EntityCombustByBlockEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();

        if (!configManager.isItemEnabled("strider_shoes") ||
                !ArtifactUtil.hasEquipped(player, ArtifactItem.STRIDER_SHOES, api)) {
            return;
        }

        // Only block combustion when sneaking near lava
        if (player.isSneaking() && isNearLava(player)) {
            event.setCancelled(true);
        }
    }
}
