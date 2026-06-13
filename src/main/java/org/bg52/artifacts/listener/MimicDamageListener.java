package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.entity.MimicAI;
import org.bg52.artifacts.entity.MimicEntity;
import org.bg52.artifacts.manager.MimicManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles combat events for mimic entities:
 * - Player attacking mimic (trigger attacked animation)
 * - Mimic death (cleanup)
 * - Prevent slime natural targeting
 * - Prevent slime splitting
 * - Protect the zombie model carrier from damage and attacks
 * - Prevent zombie-to-drowned conversion
 * - Block the zombie from dealing damage to players
 */
public class MimicDamageListener implements Listener {

    private final Artifacts plugin;
    private final MimicManager mimicManager;
    private final NamespacedKey mimicKey;

    public MimicDamageListener(Artifacts plugin, MimicManager mimicManager) {
        this.plugin = plugin;
        this.mimicManager = mimicManager;
        this.mimicKey = new NamespacedKey(plugin, "mimic");
    }

    /**
     * Handle player attacking mimic slime + protect mimic zombie + block zombie
     * attacks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();

        // Block the mimic zombie from dealing damage to anything
        if (damager instanceof Zombie && isMimicZombie((Zombie) damager)) {
            event.setCancelled(true);
            return;
        }

        // Protect the mimic zombie from ALL damage
        if (damaged instanceof Zombie && isMimicZombie((Zombie) damaged)) {
            event.setCancelled(true);
            return;
        }

        // Player attacking mimic slime — trigger attacked animation
        if (damaged instanceof Slime && damager instanceof Player) {
            Slime slime = (Slime) damaged;
            MimicAI ai = mimicManager.getMimic(slime.getUniqueId());
            if (ai != null) {
                Player attacker = (Player) damager;
                ai.onDamaged(attacker);
            }
        }
    }

    /**
     * Prevent all damage to mimic zombies (non-entity damage sources like fire,
     * suffocation).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent)
            return; // Handled above

        Entity damaged = event.getEntity();
        if (damaged instanceof Zombie && isMimicZombie((Zombie) damaged)) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle mimic death: clean up zombie and prevent slime splitting.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Slime))
            return;

        Slime slime = (Slime) event.getEntity();
        MimicAI ai = mimicManager.getMimic(slime.getUniqueId());
        if (ai == null)
            return;
        
        // Prevent slime from splitting by setting size to 1 BEFORE death processing
        // Size-1 slimes do not split on death
        slime.setSize(1);

        // Track death location for slime split prevention (belt-and-suspenders)
        final Location deathLoc = slime.getLocation();
        mimicManager.addDeathLocation(deathLoc);

        // Clear drops and exp
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Drop a random enabled artifact
        java.util.List<org.bg52.artifacts.item.ArtifactItem> enabledArtifacts = new java.util.ArrayList<>();
        for (org.bg52.artifacts.item.ArtifactItem artifact : org.bg52.artifacts.item.ArtifactItem.values()) {
            // Must be enabled globally AND enabled for mimic drops
            if (plugin.getArtifactConfig().isItemEnabled(artifact.getId()) &&
                    plugin.getMimicConfig().isDropEnabled(artifact.getId())) {
                enabledArtifacts.add(artifact);
            }
        }
        if (!enabledArtifacts.isEmpty()) {
            org.bg52.artifacts.item.ArtifactItem randomArtifact = enabledArtifacts
                    .get(new java.util.Random().nextInt(enabledArtifacts.size()));
            org.bukkit.inventory.ItemStack itemStack = plugin.getItemRegistry().createItemStack(randomArtifact);
            if (itemStack != null) {
                slime.getWorld().dropItemNaturally(deathLoc, itemStack);
            }
        }

        // Notify AI and immediately clean up from manager (zombie removal, AI cancel)
        ai.onDeath();
        mimicManager.removeMimic(slime.getUniqueId());

        // Schedule multiple staggered cleanup sweeps to catch any split remnants
        // Sweep at end of current tick
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                cleanupSplitSlimes(deathLoc);
            }
        });
        // Sweep 1 tick later (catches any that spawned after our first sweep)
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                cleanupSplitSlimes(deathLoc);
            }
        }, 1L);
        // Sweep 2 ticks later (final safety pass)
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                cleanupSplitSlimes(deathLoc);
            }
        }, 2L);

        plugin.getLogger().info("Mimic killed at " + deathLoc.getBlockX() + ", "
                + deathLoc.getBlockY() + ", " + deathLoc.getBlockZ());
    }

    /**
     * Remove any small split slimes near a death location.
     */
    private void cleanupSplitSlimes(Location deathLoc) {
        if (deathLoc == null || deathLoc.getWorld() == null)
            return;
        for (Entity entity : deathLoc.getWorld().getNearbyEntities(deathLoc, 3, 3, 3)) {
            if (entity instanceof Slime) {
                Slime nearby = (Slime) entity;
                if (nearby.isValid() && nearby.getSize() < MimicEntity.SLIME_SIZE) {
                    nearby.remove();
                }
            }
        }
    }

    /**
     * Prevent mimic slime from naturally targeting entities.
     * All targeting is handled by MimicAI.
     * NOTE: We do NOT cancel zombie targeting — the zombie needs to target
     * the player so it naturally faces them.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Slime))
            return;

        Slime slime = (Slime) event.getEntity();
        if (mimicManager.getMimic(slime.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent slime splitting when a mimic dies.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SLIME_SPLIT)
            return;
        if (!(event.getEntity() instanceof Slime))
            return;

        if (mimicManager.isRecentDeathLocation(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent the mimic zombie from converting to a drowned in water.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTransform(EntityTransformEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Zombie && isMimicZombie((Zombie) entity)) {
            event.setCancelled(true);
        }
    }

    /**
     * Check if a zombie is a mimic component via PDC or manager lookup.
     */
    private boolean isMimicZombie(Zombie zombie) {
        if (zombie.getPersistentDataContainer().has(mimicKey, PersistentDataType.BYTE)) {
            return true;
        }
        return mimicManager.getMimicByZombie(zombie.getUniqueId()) != null;
    }
}
