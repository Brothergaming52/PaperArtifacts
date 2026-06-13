package org.bg52.artifacts.entity;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.util.VersionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Wraps the Slime + Zombie pair that forms a mimic entity.
 *
 * The slime provides the hitbox and physics (invisible, size-2).
 * The zombie rides the slime as a passenger and serves as the model
 * display via a custom-model-data item on its head.
 *
 * The zombie's natural AI handles facing toward the target player
 * (yaw only — pitch is locked to 0 each tick). Its attacks and
 * damage-taking are suppressed via MimicDamageListener.
 */
public class MimicEntity {

    private final Artifacts plugin;
    private Slime slime;
    private Zombie zombie;
    private MimicModel currentModel = MimicModel.DORMANT;

    public static final double MAX_HEALTH = 60.0; // 30 hearts
    public static final int SLIME_SIZE = 2;
    public static final double ATTACK_DAMAGE = 2.5; // 1.25 hearts

    public MimicEntity(Artifacts plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawn the mimic at the given location.
     * Creates an invisible size-2 slime with an invisible zombie passenger
     * that holds the display model on its head.
     */
    public void spawn(Location location) {
        NamespacedKey mimicKey = new NamespacedKey(plugin, "mimic");

        // ── Slime: hitbox + physics ──
        slime = (Slime) location.getWorld().spawnEntity(location, EntityType.SLIME);
        slime.setSize(SLIME_SIZE);
        slime.setRemoveWhenFarAway(false);
        slime.setSilent(true);
        slime.setAI(false);
        slime.setCustomName("Mimic");
        slime.setCustomNameVisible(false);

        // Make slime invisible via potion effect (works on all versions)
        slime.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

        // Set max health to 30 hearts (60 HP)
        Attribute maxHealthAttr = VersionUtil.getMaxHealthAttribute();
        if (maxHealthAttr != null) {
            AttributeInstance healthInstance = slime.getAttribute(maxHealthAttr);
            if (healthInstance != null) {
                healthInstance.setBaseValue(MAX_HEALTH);
                slime.setHealth(MAX_HEALTH);
            }
        }

        slime.getPersistentDataContainer().set(mimicKey, PersistentDataType.BYTE, (byte) 1);

        // ── Zombie: model display + natural player-facing ──
        zombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        zombie.setSilent(true);
        zombie.setAI(false); // Toggled by MimicAI alongside the slime
        zombie.setInvulnerable(true);
        zombie.setCanPickupItems(false);
        zombie.setRemoveWhenFarAway(false);
        zombie.setGravity(false); // Physics handled by the slime
        zombie.setBaby(false); // Adult zombie
        zombie.setCustomNameVisible(false);

        // Invisible body — only the helmet model item renders
        zombie.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

        // Fire resistance — prevents burning in sunlight
        zombie.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

        // Suppress fire ticks and visual fire immediately
        zombie.setFireTicks(0);

        // Clear ALL equipment slots to prevent random spawned items/armor
        if (zombie.getEquipment() != null) {
            zombie.getEquipment().setHelmet(null);
            zombie.getEquipment().setChestplate(null);
            zombie.getEquipment().setLeggings(null);
            zombie.getEquipment().setBoots(null);
            zombie.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
            zombie.getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
            // Zero out all equipment drop chances
            zombie.getEquipment().setHelmetDropChance(0f);
            zombie.getEquipment().setChestplateDropChance(0f);
            zombie.getEquipment().setLeggingsDropChance(0f);
            zombie.getEquipment().setBootsDropChance(0f);
            zombie.getEquipment().setItemInMainHandDropChance(0f);
            zombie.getEquipment().setItemInOffHandDropChance(0f);
        }

        // Tag the zombie as mimic component
        zombie.getPersistentDataContainer().set(mimicKey, PersistentDataType.BYTE, (byte) 1);

        // Set initial model (dormant = chest appearance)
        forceSetModel(MimicModel.DORMANT);

        // Mount zombie on slime
        slime.addPassenger(zombie);
    }

    /**
     * Update the displayed model by changing the carrot on a stick's
     * CustomModelData/ItemModel on the zombie's head.
     */
    public void setModel(MimicModel model) {
        if (zombie == null || !zombie.isValid())
            return;
        if (model == currentModel)
            return;

        this.currentModel = model;

        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            VersionUtil.setMimicModel(meta, model);
            item.setItemMeta(meta);
        }

        ensureMounted();
        if (zombie.getEquipment() != null) {
            zombie.getEquipment().setHelmet(item);
        }
    }

    /**
     * Force-set a model even if it's the same as current (used for initialization).
     */
    public void forceSetModel(MimicModel model) {
        this.currentModel = null; // Reset so setModel won't skip
        setModel(model);
    }

    /**
     * Sync the zombie's state with the current AI state.
     * Called every tick by MimicAI after determining the state.
     *
     * @param aiEnabled true when the slime/mimic is actively moving (not idle)
     * @param target    the current target player, or null if none
     */
    public void syncZombie(boolean aiEnabled, Player target) {
        if (zombie == null || !zombie.isValid())
            return;

        zombie.setAI(aiEnabled);

        // Suppress fire every tick — belt-and-suspenders with fire resistance potion
        zombie.setFireTicks(0);

        // Set zombie's target to match the slime's target so it faces the same player
        if (target != null && target.isOnline() && !target.isDead()) {
            zombie.setTarget(target);
        } else {
            zombie.setTarget(null);
        }

        // Lock pitch to 0 — only allow horizontal (left/right) rotation
        float currentYaw = zombie.getLocation().getYaw();
        zombie.setRotation(currentYaw, 0f);
    }

    /**
     * Ensure the zombie is still mounted on the slime.
     */
    public void ensureMounted() {
        if (slime != null && zombie != null && slime.isValid() && zombie.isValid()) {
            if (!slime.getPassengers().contains(zombie)) {
                slime.addPassenger(zombie);
            }
        }
    }

    public MimicModel getCurrentModel() {
        return currentModel;
    }

    public Slime getSlime() {
        return slime;
    }

    public Zombie getZombie() {
        return zombie;
    }

    public boolean isAlive() {
        return slime != null && slime.isValid() && !slime.isDead();
    }

    /**
     * Remove both entities from the world.
     */
    public void remove() {
        if (zombie != null) {
            zombie.remove();
        }
        if (slime != null) {
            slime.remove();
        }
    }

    /**
     * Get the spawn/current location of the mimic for reverting to a chest.
     */
    public Location getLocation() {
        if (slime != null && slime.isValid()) {
            return slime.getLocation();
        }
        return null;
    }

    /**
     * Revert the mimic back to a normal chest block and remove the entities.
     * Called when the player leaves detection range or logs out.
     *
     * Uses safe placement: searches upward first for an air block above
     * solid ground, then downward, with a nearby spiral fallback.
     *
     * @return the block location where the chest was placed, or null if placement
     *         failed
     */
    public Location revertToChest() {
        Location loc = getLocation();
        remove();

        if (loc == null || loc.getWorld() == null)
            return null;

        Block placed = findSafePlacement(loc);
        if (placed != null) {
            placed.setType(Material.CHEST);
            return placed.getLocation();
        }

        // Fallback: force place at the origin to prevent despawning
        Block origin = loc.getBlock();
        origin.setType(Material.CHEST);
        return origin.getLocation();
    }

    /**
     * Find a safe block to place a chest: air block with solid ground below.
     * Strategy: search upward first (escape from inside blocks), then downward
     * (handle floating), then nearby spiral as fallback.
     */
    private Block findSafePlacement(Location loc) {
        Block origin = loc.getBlock();
        int originY = origin.getY();
        int maxY = loc.getWorld().getMaxHeight() - 1;
        // World.getMinHeight() only exists in 1.17+ API; use 0 as a safe default
        int minY = 0;

        // If the origin block is already a valid spot (air above solid), use it
        if (isValidChestSpot(origin)) {
            return origin;
        }

        // ── Search UPWARD first (handles suffocating inside blocks) ──
        for (int y = originY + 1; y <= Math.min(originY + 5, maxY); y++) {
            Block candidate = loc.getWorld().getBlockAt(origin.getX(), y, origin.getZ());
            if (isValidChestSpot(candidate)) {
                return candidate;
            }
        }

        // ── Search DOWNWARD (handles floating in air) ──
        for (int y = originY - 1; y >= Math.max(originY - 10, minY + 1); y--) {
            Block candidate = loc.getWorld().getBlockAt(origin.getX(), y, origin.getZ());
            if (isValidChestSpot(candidate)) {
                return candidate;
            }
        }

        // ── Spiral fallback — search nearby at same Y band ──
        int[] offsets = { -1, 0, 1 };
        for (int dx : offsets) {
            for (int dz : offsets) {
                if (dx == 0 && dz == 0)
                    continue;
                for (int dy = 0; dy <= 3; dy++) {
                    Block candidate = loc.getWorld().getBlockAt(
                            origin.getX() + dx, originY + dy, origin.getZ() + dz);
                    if (candidate.getY() <= maxY && candidate.getY() >= minY + 1
                            && isValidChestSpot(candidate)) {
                        return candidate;
                    }
                }
            }
        }

        return null; // No valid spot found (extremely unlikely)
    }

    /**
     * Check if a block is a valid chest placement spot:
     * the block itself must be air, and the block below must be solid.
     */
    private boolean isValidChestSpot(Block block) {
        if (!isAir(block))
            return false;
        Block below = block.getRelative(0, -1, 0);
        return below.getType().isSolid();
    }

    /**
     * Check if a block is any variant of air.
     */
    private boolean isAir(Block block) {
        Material type = block.getType();
        if (type == Material.AIR)
            return true;
        String name = type.name();
        return name.contains("CAVE_AIR") || name.contains("VOID_AIR");
    }
}
