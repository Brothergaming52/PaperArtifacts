package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NegativeEffectListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;

    public NegativeEffectListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        // 1. Ensure it's a negative effect we care about
        if (event.getModifiedType() != PotionEffectType.POISON &&
                event.getModifiedType() != PotionEffectType.WITHER &&
                event.getModifiedType() != PotionEffectType.HUNGER &&
                event.getModifiedType() != PotionEffectType.SLOW &&
                event.getModifiedType() != PotionEffectType.SLOW_DIGGING &&
                event.getModifiedType() != PotionEffectType.CONFUSION &&
                event.getModifiedType() != PotionEffectType.BLINDNESS &&
                event.getModifiedType() != PotionEffectType.WEAKNESS &&
                event.getModifiedType() != PotionEffectType.POISON) {
            return;
        }

        // 2. Ensure the entity is a player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // 3. Check if they have the artifact equipped
        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.ANTIDOTE_VESSEL, api)) {
            return;
        }

        // 4. Ignore events where the effect is being removed or cleared
        if (event.getAction() == EntityPotionEffectEvent.Action.CLEARED ||
                event.getAction() == EntityPotionEffectEvent.Action.REMOVED) {
            return;
        }

        // 5. Null check the new effect just to be safe
        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) {
            return;
        }

        // 6. Check duration and cap if necessary
        int maxDuration = plugin.getArtifactConfig().getAntidoteVesselCapDurationSeconds() * 20;

        if (newEffect.getDuration() > maxDuration) {

            // Build the new capped effect
            PotionEffect cappedEffect = new PotionEffect(
                    newEffect.getType(),
                    maxDuration,
                    newEffect.getAmplifier(),
                    newEffect.isAmbient(),
                    newEffect.hasParticles(),
                    newEffect.hasIcon());

            // Cancel the vanilla application of the long effect
            event.setCancelled(true);

            // Manually apply our new capped effect
            player.addPotionEffect(cappedEffect);
        }
    }
}