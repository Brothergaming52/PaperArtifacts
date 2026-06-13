package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Snorkel: Grants Water Breathing potion effect.
 *
 * Grants a configurable duration of Water Breathing when the player
 * first submerges (loses air). The cooldown resets when fully resurfacing.
 */
public class SnorkelListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;

    public SnorkelListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();

        if (event.getPreviousItem() != null) {
            if (ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.SNORKEL)) {
                removeEffect(player, PotionEffectType.WATER_BREATHING);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAirChange(EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // Surfacing / regaining full air
        if (event.getAmount() == player.getMaximumAir()) {
            player.removeScoreboardTag("artifacts_snorkel_used");
            return;
        }

        // Losing air
        if (event.getAmount() < player.getRemainingAir()) {
            if (!player.getScoreboardTags().contains("artifacts_snorkel_used")) {
                if (plugin.getArtifactConfig().isItemEnabled("snorkel") && ArtifactUtil.hasEquipped(player, ArtifactItem.SNORKEL, api)) {
                    player.addScoreboardTag("artifacts_snorkel_used");
                    int duration = plugin.getArtifactConfig().getSnorkelOxygenDuration() * 20;
                    PotionEffect effect = new PotionEffect(
                            PotionEffectType.WATER_BREATHING,
                            duration,
                            0,
                            false, // ambient
                            false, // particles
                            false  // icon
                    );
                    player.addPotionEffect(effect);
                    event.setCancelled(true);
                }
            }
        }
    }

    private void removeEffect(Player player, PotionEffectType type) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing != null && !existing.hasParticles()) {
            player.removePotionEffect(type);
        }
    }

    public void shutdown() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removeEffect(player, PotionEffectType.WATER_BREATHING);
        }
    }
}
