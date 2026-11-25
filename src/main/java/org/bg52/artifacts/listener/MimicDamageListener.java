package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.MimicManager;
import org.bg52.synthetix.api.event.SynthetixEntityDamageEvent;
import org.bg52.synthetix.api.event.SynthetixEntityDeathEvent;
import org.bg52.synthetix.entity.runtime.CustomEntityInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles combat and death events for Mimic entities:
 * - Triggers animation responses on attack/damage
 * - Injects configured artifact loot drops upon death
 * - Manages cleanup from the MimicManager
 */
public class MimicDamageListener implements Listener {

    private final Artifacts plugin;
    private final MimicManager mimicManager;

    public MimicDamageListener(Artifacts plugin, MimicManager mimicManager) {
        this.plugin = plugin;
        this.mimicManager = mimicManager;
    }

    /**
     * Intercept damage events on Synthetix custom entities to play animations.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSynthetixEntityDamage(SynthetixEntityDamageEvent event) {
        CustomEntityInstance instance = event.getEntityInstance();
        if (instance == null || !"mimic".equalsIgnoreCase(instance.getDefinition().getId())) {
            return;
        }

        // Trigger hurt animation if available
        if (instance.getAnimationController() != null && instance.getAnimationController().hasTrack("hurt")) {
            instance.getAnimationController().play("hurt", false, 0.1f);
        }
    }

    /**
     * Handle death of custom Synthetix entity and drop configured artifacts.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSynthetixEntityDeath(SynthetixEntityDeathEvent event) {
        CustomEntityInstance instance = event.getEntityInstance();
        if (instance == null || !"mimic".equalsIgnoreCase(instance.getDefinition().getId())) {
            return;
        }

        // Clean up tracking in MimicManager
        if (instance.getDriverMob() != null) {
            mimicManager.removeMimic(instance.getDriverMob().getUniqueId());
        }

        // Roll artifact drop from mimic.yml
        List<String> validDrops = new ArrayList<>();
        for (ArtifactItem artifact : ArtifactItem.values()) {
            if (plugin.getArtifactConfig().isItemEnabled(artifact.getId()) &&
                plugin.getMimicConfig().isDropEnabled(artifact.getId())) {
                validDrops.add(artifact.getId());
            }
        }

        if (!validDrops.isEmpty()) {
            String chosenId = validDrops.get(ThreadLocalRandom.current().nextInt(validDrops.size()));
            ItemStack item = plugin.getItemRegistry().createItemStack(chosenId);
            if (item != null) {
                event.getDrops().add(item);
            }
        }
    }

    /**
     * Clean up driver mob if killed via standard Bukkit events.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (mimicManager.isMimicEntity(entity)) {
            mimicManager.removeMimic(entity.getUniqueId());
        }
    }

    /**
     * When player attacks mimic mob directly, trigger attack or hurt response.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        if (mimicManager.isMimicEntity(damaged) && event.getDamager() instanceof Player) {
            CustomEntityInstance instance = mimicManager.getMimic(damaged.getUniqueId());
            if (instance != null && instance.getAnimationController() != null && instance.getAnimationController().hasTrack("hurt")) {
                instance.getAnimationController().play("hurt", false, 0.1f);
            }
        }
    }
}
