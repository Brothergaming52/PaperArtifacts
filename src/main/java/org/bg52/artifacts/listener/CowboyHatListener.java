package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.api.event.AccessoryEquipEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

/**
 * Cowboy Hat: While riding a mount, grants Speed I to the mount.
 *
 * Uses a periodic check to detect riding state and apply the speed
 * effect to the vehicle entity. This avoids needing to hook into
 * every possible mount interaction.
 */
public class CowboyHatListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;

    public CowboyHatListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player))
            return;
        Player player = (Player) event.getEntered();
        applyMountSpeed(player, event.getVehicle());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityMount(org.bukkit.event.entity.EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        applyMountSpeed(player, event.getMount());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(org.bukkit.event.vehicle.VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player))
            return;
        removeMountSpeed(event.getVehicle());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDismount(org.bukkit.event.entity.EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        removeMountSpeed(event.getDismounted());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        if (!plugin.getArtifactConfig().isItemEnabled("cowboy_hat"))
            return;

        Player player = event.getPlayer();
        boolean wasCowboyHat = ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.COWBOY_HAT);
        boolean isCowboyHat = ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.COWBOY_HAT);

        if (event.getAction() == AccessoryEquipEvent.Action.EQUIP ||
                event.getAction() == AccessoryEquipEvent.Action.SWAP) {
            if (isCowboyHat && player.isInsideVehicle()) {
                applyMountSpeed(player, player.getVehicle());
            }
        }

        if (event.getAction() == AccessoryEquipEvent.Action.UNEQUIP ||
                event.getAction() == AccessoryEquipEvent.Action.SWAP) {
            if (wasCowboyHat && !isCowboyHat && player.isInsideVehicle()) {
                removeMountSpeed(player.getVehicle());
            }
        }
    }

    private void applyMountSpeed(Player player, Entity vehicleOverride) {
        if (!plugin.getArtifactConfig().isItemEnabled("cowboy_hat"))
            return;
        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.COWBOY_HAT, api))
            return;

        Entity vehicle = vehicleOverride != null ? vehicleOverride : player.getVehicle();
        if (vehicle instanceof LivingEntity) {
            LivingEntity mount = (LivingEntity) vehicle;
            int amplifier = plugin.getArtifactConfig().getCowboyHatSpeedAmplifier();

            PotionEffect speed = new PotionEffect(
                    PotionEffectType.SPEED,
                    Integer.MAX_VALUE, // Pure event based, so effect lasts until dismount
                    amplifier,
                    false,
                    false,
                    false);
            mount.addPotionEffect(speed);

            if (plugin.getArtifactConfig().isParticlesEnabled()) {
                try {
                    mount.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, mount.getLocation().add(0.0, 0.2, 0.0), 6, 0.3, 0.1, 0.3, 0.01);
                } catch (Throwable ignored) {
                    mount.getWorld().spawnParticle(Particle.CLOUD, mount.getLocation().add(0.0, 0.2, 0.0), 4, 0.2, 0.1, 0.2, 0.02);
                }
            }

            if (plugin.getArtifactConfig().isSoundsEnabled()) {
                player.getWorld().playSound(mount.getLocation(), Sound.ENTITY_HORSE_GALLOP, 0.7f, 1.3f);
            }
        }
    }

    private void removeMountSpeed(Entity vehicle) {
        if (vehicle instanceof LivingEntity) {
            ((LivingEntity) vehicle).removePotionEffect(PotionEffectType.SPEED);
        }
    }

    public void shutdown() {
        // Nothing to shut down anymore since there's no tick task!
    }
}
