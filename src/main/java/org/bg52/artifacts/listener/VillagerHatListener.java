package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Villager Hat: Reduces villager trade prices while equipped.
 * On unequip, prices reset.
 *
 * It uses the Hero of the Village potion effect to provide discounts.
 * The effect gracefully stacks with native Hero of the Village buffs.
 */
public class VillagerHatListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;

    // Prevent infinite loops when modifying potion effects inside the listener
    private final Set<UUID> updating = new HashSet<>();

    public VillagerHatListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    private int getHatAmplifier() {
        int level = plugin.getArtifactConfig().getVillagerHatHeroOfVillageLevel() - 1;
        return Math.max(level, 0);
    }

    // Helper to safely apply an effect bypassing our own listener
    private void applyEffect(Player player, PotionEffect effect) {
        updating.add(player.getUniqueId());
        player.addPotionEffect(effect);
        updating.remove(player.getUniqueId());
    }

    private void removeEffect(Player player) {
        updating.add(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        updating.remove(player.getUniqueId());
    }

    private void applyHatOnlyEffect(Player player) {
        PotionEffect effect = new PotionEffect(
                PotionEffectType.HERO_OF_THE_VILLAGE,
                Integer.MAX_VALUE, // Effectively infinite
                getHatAmplifier(),
                false, // ambient
                false, // particles
                false  // icon
        );
        applyEffect(player, effect);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        if (!plugin.getArtifactConfig().isItemEnabled("villager_hat")) return;

        Player player = event.getPlayer();

        if (event.getPreviousItem() != null && ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.VILLAGER_HAT)) {
            onUnequip(player);
        }

        if (event.getNewItem() != null && ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.VILLAGER_HAT)) {
            onEquip(player);
        }
    }

    private void onEquip(Player player) {
        PotionEffect existing = player.getPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        
        if (existing != null) {
            if (existing.getDuration() > 1000000) {
                // Must be a leftover hat effect, apply fresh hat effect
                applyHatOnlyEffect(player);
            } else {
                // Have a native/finite effect! Stack our hat over it.
                int hatAmp = getHatAmplifier();
                int combinedAmp = existing.getAmplifier() + hatAmp + 1;
                PotionEffect combined = new PotionEffect(
                    PotionEffectType.HERO_OF_THE_VILLAGE,
                    existing.getDuration(),
                    combinedAmp,
                    existing.isAmbient(),
                    existing.hasParticles(),
                    existing.hasIcon()
                );
                applyEffect(player, combined);
            }
        } else {
            // Apply standard infinite hat effect
            applyHatOnlyEffect(player);
        }
    }

    private void onUnequip(Player player) {
        PotionEffect existing = player.getPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        if (existing != null) {
            if (existing.getDuration() > 1000000) {
                // Hat only effect, completely remove it
                removeEffect(player);
            } else {
                // Return to their native level before the hat
                int hatAmp = getHatAmplifier();
                int nativeAmp = existing.getAmplifier() - hatAmp - 1;
                if (nativeAmp < 0) {
                    removeEffect(player);
                } else {
                    PotionEffect nativeEffect = new PotionEffect(
                        PotionEffectType.HERO_OF_THE_VILLAGE,
                        existing.getDuration(),
                        nativeAmp,
                        existing.isAmbient(),
                        existing.hasParticles(),
                        existing.hasIcon()
                    );
                    applyEffect(player, nativeEffect);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getModifiedType() != PotionEffectType.HERO_OF_THE_VILLAGE) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (updating.contains(player.getUniqueId())) return;
        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.VILLAGER_HAT, api)) return;

        EntityPotionEffectEvent.Action action = event.getAction();
        
        if (action == EntityPotionEffectEvent.Action.CLEARED || action == EntityPotionEffectEvent.Action.REMOVED) {
            // The combined/finite effect expired or was removed by milk. 
            // Player still has hat equipped, so reapply the hat effect on next tick!
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && ArtifactUtil.hasEquipped(player, ArtifactItem.VILLAGER_HAT, api)) {
                    applyHatOnlyEffect(player);
                }
            });
        } else if (action == EntityPotionEffectEvent.Action.ADDED || action == EntityPotionEffectEvent.Action.CHANGED) {
            PotionEffect newEffect = event.getNewEffect();
            if (newEffect != null && newEffect.getDuration() < 1000000) {
                // A finite effect (e.g., from an external plugin command) was applied over our hat!
                int hatAmp = getHatAmplifier();
                int combinedAmp = newEffect.getAmplifier() + hatAmp + 1;
                
                PotionEffect combined = new PotionEffect(
                    PotionEffectType.HERO_OF_THE_VILLAGE,
                    newEffect.getDuration(),
                    combinedAmp,
                    newEffect.isAmbient(),
                    newEffect.hasParticles(),
                    newEffect.hasIcon()
                );
                
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline() && ArtifactUtil.hasEquipped(player, ArtifactItem.VILLAGER_HAT, api)) {
                        applyEffect(player, combined);
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRaidFinish(RaidFinishEvent event) {
        for (Player player : event.getWinners()) {
            if (!ArtifactUtil.hasEquipped(player, ArtifactItem.VILLAGER_HAT, api)) continue;
            
            // Native vanilla applies RaidLevel - 1 for 48000 ticks (40 minutes).
            int nativeAmp = event.getRaid().getBadOmenLevel() - 1;
            if (nativeAmp < 0) nativeAmp = 0;
            
            int hatAmp = getHatAmplifier();
            int combinedAmp = nativeAmp + hatAmp + 1;
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                PotionEffect combined = new PotionEffect(
                    PotionEffectType.HERO_OF_THE_VILLAGE,
                    48000, 
                    combinedAmp,
                    false, false, true
                );
                applyEffect(player, combined);
            });
        }
    }
}
