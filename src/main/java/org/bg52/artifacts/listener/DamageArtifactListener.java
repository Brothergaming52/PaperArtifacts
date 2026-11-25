package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.CooldownManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Handles damage-related accessories:
 *
 * - Cross Necklace: Increases invulnerability frames (10 → 30 ticks)
 * - Panic Necklace: Grants Speed I for 7 seconds on damage
 * - Shock Pendant: 15% chance to summon lightning on attacker + lightning
 * immunity
 * - Flame Pendant: 30% chance to deal instant damage + fire to attacker + fire
 * resist for wearer
 *
 * All values are configurable.
 */
public class DamageArtifactListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final Random random = new Random();
    private final CooldownManager cooldownManager;

    public DamageArtifactListener(Artifacts plugin, CuriosPaperAPI api, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.api = api;
        this.cooldownManager = cooldownManager;
    }

    /**
     * Handle all damage-related artifact effects.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        EntityDamageEvent.DamageCause cause = event.getCause();

        // ── Shock Pendant: Lightning immunity ──
        if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            if (plugin.getArtifactConfig().isItemEnabled("shock_pendant")
                    && plugin.getArtifactConfig().getShockPendantLightningImmunity()
                    && ArtifactUtil.hasEquipped(player, ArtifactItem.SHOCK_PENDANT, api)) {
                event.setCancelled(true);
                return;
            }
        }

        // ── Cross Necklace: Extended i-frames ──
        if (plugin.getArtifactConfig().isItemEnabled("cross_necklace")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.CROSS_NECKLACE, api)) {
            int ticks = plugin.getArtifactConfig().getCrossNecklaceInvulnerabilityTicks();
            // Minecraft's actual i-frames are maxNoDamageTicks / 2.
            // If the config specifies 30 ticks of immunity, maxNoDamageTicks must be 60.
            player.setMaximumNoDamageTicks(ticks * 2);
            if (plugin.getArtifactConfig().isParticlesEnabled()) {
                try {
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1.0, 0), 12, 0.3, 0.5, 0.3, 0.1);
                    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.2, 0), 6, 0.2, 0.3, 0.2, 0.02);
                } catch (Throwable ignored) {}
            }
            if (plugin.getArtifactConfig().isSoundsEnabled()) {
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.6f, 1.6f);
            }
        } else {
            // Revert to 20 (vanilla default) if they don't have it equipped
            if (player.getMaximumNoDamageTicks() != 20) {
                player.setMaximumNoDamageTicks(20);
            }
        }

        // ── Panic Necklace: Speed boost on damage ──
        if (plugin.getArtifactConfig().isItemEnabled("panic_necklace")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.PANIC_NECKLACE, api)) {
            int amplifier = plugin.getArtifactConfig().getPanicNecklaceSpeedAmplifier();
            int duration = plugin.getArtifactConfig().getPanicNecklaceSpeedDuration();
            PotionEffect speed = new PotionEffect(
                    PotionEffectType.SPEED,
                    duration * 20,
                    amplifier,
                    false, false, true);
            player.addPotionEffect(speed);
            if (plugin.getArtifactConfig().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.3, 0), 10, 0.3, 0.1, 0.3, 0.05);
                try {
                    player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add(0, 0.2, 0), 5, 0.2, 0.1, 0.2, 0.01);
                } catch (Throwable ignored) {}
            }
            if (plugin.getArtifactConfig().isSoundsEnabled()) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_GALLOP, 0.8f, 1.6f);
            }
        }

        // ── Obsidian Skull: Fire resistance on being damaged by fire ──
        if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            if (plugin.getArtifactConfig().isItemEnabled("obsidian_skull")
                    && ArtifactUtil.hasEquipped(player, ArtifactItem.OBSIDIAN_SKULL, api)) {
                if (player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
                    return;
                }
                if (cooldownManager.hasCooldown(player, "obsidian_skull")) {
                    return;
                }
                int duration = plugin.getArtifactConfig().getObsidianSkullFireResistanceDuration();
                PotionEffect fireResist = new PotionEffect(
                        PotionEffectType.FIRE_RESISTANCE,
                        duration * 20,
                        0,
                        false, false, true);
                player.addPotionEffect(fireResist);
                cooldownManager.setCooldown(player, "obsidian_skull",
                        plugin.getArtifactConfig().getObsidianSkullCooldown());
                if (plugin.getArtifactConfig().isParticlesEnabled()) {
                    player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1.0, 0), 15, 0.3, 0.5, 0.3, 0.02);
                    player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 0.8, 0), 6, 0.3, 0.4, 0.3, 0.0);
                }
                if (plugin.getArtifactConfig().isSoundsEnabled()) {
                    try {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.8f);
                    } catch (Throwable ignored) {
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 0.8f);
                    }
                }
            }
        }
    }

    /**
     * Handle damage-by-entity effects (Shock Pendant + Flame Pendant offensive).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player victim = (Player) event.getEntity();

        // Get the attacker as a LivingEntity
        if (!(event.getDamager() instanceof LivingEntity))
            return;
        LivingEntity attacker = (LivingEntity) event.getDamager();

        // ── Shock Pendant: Lightning strike on attacker ──
        if (plugin.getArtifactConfig().isItemEnabled("shock_pendant")
                && ArtifactUtil.hasEquipped(victim, ArtifactItem.SHOCK_PENDANT, api)) {
            double chance = plugin.getArtifactConfig().getShockPendantChance();
            if (random.nextDouble() < chance) {
                // Only strike if attacker has sky access
                if (attacker.getWorld().getHighestBlockYAt(attacker.getLocation()) <= attacker.getLocation()
                        .getBlockY()) {
                    attacker.getWorld().strikeLightning(attacker.getLocation());
                    if (plugin.getArtifactConfig().isParticlesEnabled()) {
                        attacker.getWorld().spawnParticle(Particle.ENCHANTED_HIT, attacker.getLocation().add(0, 1.0, 0), 20, 0.5, 0.8, 0.5, 0.1);
                        attacker.getWorld().spawnParticle(Particle.FIREWORK, attacker.getLocation().add(0, 1.0, 0), 12, 0.4, 0.6, 0.4, 0.08);
                    }
                }
            }
        }

        // ── Flame Pendant: Fire + instant damage to attacker ──
        if (plugin.getArtifactConfig().isItemEnabled("flame_pendant")
                && ArtifactUtil.hasEquipped(victim, ArtifactItem.FLAME_PENDANT, api)) {
            double chance = plugin.getArtifactConfig().getFlamePendantChance();
            if (random.nextDouble() < chance) {
                double damage = plugin.getArtifactConfig().getFlamePendantDamage();
                int fireDuration = plugin.getArtifactConfig().getFlamePendantFireDuration();

                // Deal instant damage to attacker
                attacker.damage(damage, victim);

                // Set attacker on fire
                attacker.setFireTicks(fireDuration * 20);

                int duration = plugin.getArtifactConfig().getFlamePendantFireResistanceDuration();

                PotionEffect fireResist = new PotionEffect(
                        PotionEffectType.FIRE_RESISTANCE,
                        duration * 20,
                        0,
                        false, false, true);
                victim.addPotionEffect(fireResist);
                if (plugin.getArtifactConfig().isParticlesEnabled()) {
                    attacker.getWorld().spawnParticle(Particle.FLAME, attacker.getLocation().add(0, 1.0, 0), 20, 0.4, 0.6, 0.4, 0.05);
                    attacker.getWorld().spawnParticle(Particle.LAVA, attacker.getLocation().add(0, 1.0, 0), 8, 0.3, 0.5, 0.3, 0.0);
                }
                if (plugin.getArtifactConfig().isSoundsEnabled()) {
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
                }
            }
        }

        // ── Thorn Pendant: Damage attacker ──
        if (plugin.getArtifactConfig().isItemEnabled("thorn_pendant")
                && ArtifactUtil.hasEquipped(victim, ArtifactItem.THORN_PENDANT, api)) {
            double chance = plugin.getArtifactConfig().getThornPendantChance();
            if (random.nextDouble() < chance) {
                double damage_min = plugin.getArtifactConfig().getThornPendantDamageMin();
                double damage_max = plugin.getArtifactConfig().getThornPendantDamageMax();
                double damage = damage_min + (Math.random() * (damage_max - damage_min));
                double real_damage = Math.round(damage);
                attacker.damage(real_damage, victim);
                if (plugin.getArtifactConfig().isParticlesEnabled()) {
                    attacker.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation().add(0, 1.0, 0), 12, 0.3, 0.5, 0.3, 0.1);
                }
                if (plugin.getArtifactConfig().isSoundsEnabled()) {
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENCHANT_THORNS_HIT, 1.0f, 1.0f);
                }
            }
        }
    }
}
