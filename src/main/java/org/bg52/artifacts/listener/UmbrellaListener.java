package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Umbrella: Grants Slow Falling while held in either hand.
 * Effect has no particles and no icon.
 *
 * This listener is purely event-based, dynamically applying/removing
 * the effect on relevant inventory interactions.
 */
public class UmbrellaListener implements Listener {

    private final Artifacts plugin;

    public UmbrellaListener(Artifacts plugin) {
        this.plugin = plugin;
    }

    private void scheduleUpdate(Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateEffect(player);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        scheduleUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        scheduleUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            scheduleUpdate((Player) event.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            scheduleUpdate((Player) event.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        scheduleUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            scheduleUpdate((Player) event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        scheduleUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (ArtifactUtil.isArtifact(event.getItem(), ArtifactItem.UMBRELLA)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleUpdate(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        scheduleUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (event.getAction() == EntityPotionEffectEvent.Action.CLEARED ||
            event.getAction() == EntityPotionEffectEvent.Action.REMOVED) {

            PotionEffectType type = event.getModifiedType();
            
            if (type == null || type.equals(PotionEffectType.SLOW_FALLING)) {
                scheduleUpdate(player);
            }
        }
    }

    private void updateEffect(Player player) {
        if (!plugin.getArtifactConfig().isItemEnabled("umbrella")) {
            removeEffect(player);
            return;
        }

        if (ArtifactUtil.isHolding(player, ArtifactItem.UMBRELLA)) {
            int amplifier = plugin.getArtifactConfig().getUmbrellaSlowFallingAmplifier();
            PotionEffect effect = new PotionEffect(
                    PotionEffectType.SLOW_FALLING,
                    Integer.MAX_VALUE, // Infinite duration
                    amplifier,
                    false, // ambient
                    false, // particles
                    false  // icon
            );
            player.addPotionEffect(effect);
        } else {
            removeEffect(player);
        }
    }

    private void removeEffect(Player player) {
        PotionEffect existing = player.getPotionEffect(PotionEffectType.SLOW_FALLING);
        if (existing != null && !existing.hasParticles()) {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        }
    }

    public void shutdown() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removeEffect(player);
        }
    }
}
