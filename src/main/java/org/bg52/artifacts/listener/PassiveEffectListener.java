package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Handles continuously active curio effects:
 *
 * - Night Vision Goggles: Infinite Night Vision (no particles, no icon)
 * - Scarf of Invisibility: Infinite Invisibility (no particles, no icon)
 *
 * This listener is purely event-based, reapplying effects on join, respawn,
 * and when they are cleared (e.g. by drinking milk).
 */
public class PassiveEffectListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;

    public PassiveEffectListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
        startKittySlippersTask();
    }

    /**
     * Handle equip/unequip events for immediate effect application/removal.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();

        // Handle unequip
        if (event.getPreviousItem() != null) {
            if (ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.NIGHT_VISION_GOGGLES)) {
                removeEffect(player, PotionEffectType.NIGHT_VISION);
            }
            if (ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.SCARF_OF_INVISIBILITY)) {
                removeEffect(player, PotionEffectType.INVISIBILITY);
            }
            if (ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.BUNNY_HOPPERS)) {
                removeEffect(player, PotionEffectType.JUMP);
            }
            if (ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.FLIPPERS)) {
                removeEffect(player, PotionEffectType.DOLPHINS_GRACE);
            }
        }

        // Handle equip
        if (event.getNewItem() != null) {
            if (ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.NIGHT_VISION_GOGGLES)) {
                if (plugin.getArtifactConfig().isItemEnabled("night_vision_goggles")) {
                    applyNightVision(player);
                }
            }
            if (ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.SCARF_OF_INVISIBILITY)) {
                if (plugin.getArtifactConfig().isItemEnabled("scarf_of_invisibility")) {
                    applyInvisibility(player);
                }
            }
            if (ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.BUNNY_HOPPERS)) {
                if (plugin.getArtifactConfig().isItemEnabled("bunny_hoppers")) {
                    applyBunnyHoppersJumpHeight(player);
                }
            }
            if (ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.FLIPPERS)) {
                if (plugin.getArtifactConfig().isItemEnabled("flippers")) {
                    applyDolphinsGrace(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkAndApplyEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Delay by 1 tick because inventory might not be fully loaded or effects
        // cleared on same tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                checkAndApplyEffects(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();

        if (event.getAction() == EntityPotionEffectEvent.Action.CLEARED ||
                event.getAction() == EntityPotionEffectEvent.Action.REMOVED) {

            PotionEffectType type = event.getModifiedType();

            if (type == null) {
                // Milk or clear all
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        checkAndApplyEffects(player);
                    }
                });
            } else if (type.equals(PotionEffectType.NIGHT_VISION)) {
                if (plugin.getArtifactConfig().isItemEnabled("night_vision_goggles")
                        && ArtifactUtil.hasEquipped(player, ArtifactItem.NIGHT_VISION_GOGGLES, api)) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            applyNightVision(player);
                        }
                    });
                }
            } else if (type.equals(PotionEffectType.INVISIBILITY)) {
                if (plugin.getArtifactConfig().isItemEnabled("scarf_of_invisibility")
                        && ArtifactUtil.hasEquipped(player, ArtifactItem.SCARF_OF_INVISIBILITY, api)) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            applyInvisibility(player);
                        }
                    });
                }
            } else if (type.equals(PotionEffectType.JUMP)) {
                if (plugin.getArtifactConfig().isItemEnabled("bunny_hoppers")
                        && ArtifactUtil.hasEquipped(player, ArtifactItem.BUNNY_HOPPERS, api)) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            applyBunnyHoppersJumpHeight(player);
                        }
                    });
                }
            } else if (type.equals(PotionEffectType.DOLPHINS_GRACE)) {
                if (plugin.getArtifactConfig().isItemEnabled("flippers")
                        && ArtifactUtil.hasEquipped(player, ArtifactItem.FLIPPERS, api)) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            applyDolphinsGrace(player);
                        }
                    });
                }
            }
        }
    }

    private void checkAndApplyEffects(Player player) {
        if (plugin.getArtifactConfig().isItemEnabled("night_vision_goggles")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.NIGHT_VISION_GOGGLES, api)) {
            applyNightVision(player);
        }
        if (plugin.getArtifactConfig().isItemEnabled("scarf_of_invisibility")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.SCARF_OF_INVISIBILITY, api)) {
            applyInvisibility(player);
        }
        if (plugin.getArtifactConfig().isItemEnabled("bunny_hoppers")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.BUNNY_HOPPERS, api)) {
            applyBunnyHoppersJumpHeight(player);
        }
        if (plugin.getArtifactConfig().isItemEnabled("kitty_slippers")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.KITTY_SLIPPERS, api)) {
            // Deprecated: targeting is now intercepted by EntityTargetEvent below
        }
        if (plugin.getArtifactConfig().isItemEnabled("flippers")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.FLIPPERS, api)) {
            applyDolphinsGrace(player);
        }
    }

    private void applyNightVision(Player player) {
        PotionEffect effect = new PotionEffect(
                PotionEffectType.NIGHT_VISION,
                Integer.MAX_VALUE, // Infinite duration since it's event based
                0,
                false, // ambient
                false, // particles
                false // icon
        );
        player.addPotionEffect(effect);
    }

    private void applyInvisibility(Player player) {
        PotionEffect effect = new PotionEffect(
                PotionEffectType.INVISIBILITY,
                Integer.MAX_VALUE, // Infinite duration since it's event based
                0,
                false, // ambient
                false, // particles
                false // icon
        );
        player.addPotionEffect(effect);
    }

    private void applyBunnyHoppersJumpHeight(Player player) {
        PotionEffect effect = new PotionEffect(
                PotionEffectType.JUMP,
                Integer.MAX_VALUE, // Infinite duration since it's event based
                plugin.getArtifactConfig().getBunnyHoppersJumpHeightAmplifier(),
                false, // ambient
                false, // particles
                false // icon
        );
        player.addPotionEffect(effect);
    }

    private void applyDolphinsGrace(Player player) {
        PotionEffect effect = new PotionEffect(
                PotionEffectType.DOLPHINS_GRACE,
                Integer.MAX_VALUE, // Infinite duration since it's event based
                plugin.getArtifactConfig().getFlippersDolphinsGraceAmplifier(),
                false, // ambient
                false, // particles
                false // icon
        );
        player.addPotionEffect(effect);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreeperTarget(org.bukkit.event.entity.EntityTargetEvent event) {
        if (event.getEntity().getType() == org.bukkit.entity.EntityType.CREEPER) {
            if (event.getTarget() instanceof Player) {
                Player player = (Player) event.getTarget();
                if (plugin.getArtifactConfig().isItemEnabled("kitty_slippers") &&
                        ArtifactUtil.hasEquipped(player, ArtifactItem.KITTY_SLIPPERS, api)) {

                    event.setCancelled(true);

                    // Scaring away logic if within 5 blocks
                    if (player.getLocation().distance(event.getEntity().getLocation()) <= 5) {
                        org.bukkit.entity.Entity entity = event.getEntity();
                        if (entity instanceof org.bukkit.entity.Creeper) {
                            makeCreeperFlee((org.bukkit.entity.Creeper) entity, player.getLocation());
                        }
                    }
                }
            }
        }
    }

    private void makeCreeperFlee(org.bukkit.entity.Creeper creeper, Location avoid) {
        // Try Paper's Pathfinder API via reflection for high-quality fleeing
        try {
            java.lang.reflect.Method getPathfinder = creeper.getClass().getMethod("getPathfinder");
            Object pathfinder = getPathfinder.invoke(creeper);
            java.lang.reflect.Method moveTo = pathfinder.getClass().getMethod("moveTo", Location.class, double.class);

            Vector dir = creeper.getLocation().toVector().subtract(avoid.toVector()).normalize().multiply(12);
            Location target = creeper.getLocation().add(dir);

            // Move at 1.5x speed
            moveTo.invoke(pathfinder, target, 1.5);
            return;
        } catch (Exception ignored) {
        }

        // Fallback for Spigot/Older versions (1.14-1.15): Multi-tick velocity push
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 30 || !creeper.isValid()) {
                    this.cancel();
                    return;
                }
                // Push away from player horizontally
                Vector dir = creeper.getLocation().toVector().subtract(avoid.toVector());
                dir.setY(0); // keep it on ground
                if (dir.lengthSquared() > 0) {
                    dir.normalize().multiply(0.3);
                    dir.setY(0.1); // slight upward force to reduce friction
                    creeper.setVelocity(dir);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void startKittySlippersTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getArtifactConfig().isItemEnabled("kitty_slippers") &&
                        ArtifactUtil.hasEquipped(player, ArtifactItem.KITTY_SLIPPERS, api)) {

                    // random cat sound occasionally
                    if (Math.random() < 0.2) {
                        Sound[] catSounds = { Sound.ENTITY_CAT_PURR, Sound.ENTITY_CAT_PURREOW,
                                Sound.ENTITY_CAT_AMBIENT };
                        Sound sound = catSounds[(int) (Math.random() * catSounds.length)];
                        player.getWorld().playSound(player.getLocation(), sound, 1.0f,
                                0.8f + (float) (Math.random() * 0.4));
                    }
                }
            }
        }, 100L, 100L);
    }

    /**
     * Remove an effect only if it was applied by us (short duration, no particles).
     */
    private void removeEffect(Player player, PotionEffectType type) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing != null && !existing.hasParticles()) {
            player.removePotionEffect(type);
        }
    }

    public void shutdown() {
        // Clean up effects
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removeEffect(player, PotionEffectType.NIGHT_VISION);
            removeEffect(player, PotionEffectType.INVISIBILITY);
            removeEffect(player, PotionEffectType.JUMP);
            removeEffect(player, PotionEffectType.DOLPHINS_GRACE);
        }
    }
}
