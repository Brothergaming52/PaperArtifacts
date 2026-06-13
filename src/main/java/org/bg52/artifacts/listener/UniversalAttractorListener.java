package org.bg52.artifacts.listener;

import java.util.List;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.CooldownManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item; // Changed from LivingEntity
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class UniversalAttractorListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final CooldownManager cooldownManager;

    public UniversalAttractorListener(Artifacts plugin, CuriosPaperAPI api, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.api = api;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onItemPickup(PlayerMoveEvent event) {
        if (!plugin.getArtifactConfig().isItemEnabled("universal_attractor"))
            return;

        Player player = event.getPlayer();

        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.UNIVERSAL_ATTRACTOR, api)) {
            return;
        }

        if (cooldownManager.hasCooldown(player, ArtifactItem.UNIVERSAL_ATTRACTOR.getId())) {
            return;
        }

        // check for nearby items in the given radius
        double radius = plugin.getArtifactConfig().getUniversalAttractorRadius();
        List<Entity> nearbyEntities = player.getNearbyEntities(radius, radius, radius);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                Vector direction = player.getLocation().toVector().subtract(item.getLocation().toVector());
                double distance = direction.length();
                if (distance > 0.5) {
                    double speed = 0.35;
                    direction.normalize().multiply(speed);
                    item.setVelocity(direction);
                }
            }
        }
    }

    public void onItemDrop(PlayerDropItemEvent event) {
        if (!plugin.getArtifactConfig().isItemEnabled("universal_attractor"))
            return;

        Player player = event.getPlayer();

        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.UNIVERSAL_ATTRACTOR, api)) {
            return;
        }

        cooldownManager.setCooldown(player, ArtifactItem.UNIVERSAL_ATTRACTOR.getId(),
                plugin.getArtifactConfig().getUniversalAttractorCooldown());
    }
}
