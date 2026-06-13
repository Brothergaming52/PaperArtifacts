package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.artifacts.util.VersionUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.entity.EnderPearl;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class PassiveArtifactsListener implements Listener {

    private static final UUID FERAL_CLAWS_ID = UUID.fromString("f3b1a2c3-d4e5-4f60-8a1a-2b3c4d5e6f7a");
    private static final UUID POWER_GLOVE_ID = UUID.fromString("e4c2b3d4-f5a6-4b71-9c82-3d4e5f6a7b8c");
    private static final UUID DIGGING_CLAWS_ID = UUID.fromString("d3b1a2c3-d4e5-4f60-8a1a-1b2c3d4e5f6a");
    private static final UUID STEADFAST_SPIKES_ID = UUID.fromString("b1a2c3d4-e5f6-4a7b-8c9d-1e1f1a2b3c4d");
    private static final UUID CHARM_OF_SHRINKING_ID = UUID.fromString("c1a2c3d4-e5f6-4a7b-8c9d-1e1f1a2b3c4f");
    private final java.util.Set<UUID> warpDriveNoDamage = new java.util.HashSet<>();

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final java.util.Map<Material, Material> smeltingMap = new java.util.EnumMap<>(Material.class);

    public PassiveArtifactsListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
        initSmeltingMap();
    }

    private void initSmeltingMap() {
        smeltingMap.clear();

        File configFile = new File(plugin.getDataFolder(), "SmeltingRecipes.yml");
        if (!configFile.exists()) {
            plugin.saveResource("SmeltingRecipes.yml", false);
        }

        org.bukkit.configuration.file.FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(configFile);

        for (String key : config.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null)
                continue;

            String resultStr = section.getString("result");
            if (resultStr == null)
                continue;

            Material resultMat = org.bukkit.Material.matchMaterial(resultStr);
            if (resultMat == null)
                continue;

            Object inputObj = section.get("input");
            if (inputObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> inputs = (List<String>) inputObj;
                for (String input : inputs) {
                    resolveAndAddSmelting(input, resultMat);
                }
            } else if (inputObj instanceof String) {
                resolveAndAddSmelting((String) inputObj, resultMat);
            }
        }
    }

    private void resolveAndAddSmelting(String input, Material result) {
        if (input.startsWith("#")) {
            // It's a tag
            String tagName = input.substring(1);
            org.bukkit.NamespacedKey key;
            if (tagName.contains(":")) {
                String[] parts = tagName.split(":");
                key = new org.bukkit.NamespacedKey(parts[0], parts[1]);
            } else {
                key = org.bukkit.NamespacedKey.minecraft(tagName);
            }

            org.bukkit.Tag<Material> tag = (org.bukkit.Tag<Material>) org.bukkit.Bukkit.getTag("items", key,
                    Material.class);
            if (tag != null) {
                for (Material mat : tag.getValues()) {
                    smeltingMap.put(mat, result);
                }
            }
        } else {
            // It's a single material
            Material mat = org.bukkit.Material.matchMaterial(input);
            if (mat != null) {
                smeltingMap.put(mat, result);
            }
        }
    }

    // FIX 1: Event handlers must be public to be registered correctly
    @EventHandler
    public void onAccessoryEquip(AccessoryEquipEvent event) {

        // FIX 2: Check if the event involves the Crystal Heart in ANY way (Equip,
        // Unequip, or Swap)
        boolean involvesCrystalHeart = false;

        if (event.getNewItem() != null && ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.CRYSTAL_HEART)) {
            involvesCrystalHeart = true;
        }
        if (event.getPreviousItem() != null
                && ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.CRYSTAL_HEART)) {
            involvesCrystalHeart = true;
        }

        boolean involvesFeralClaws = false;

        if (event.getNewItem() != null && ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.FERAL_CLAWS)) {
            involvesFeralClaws = true;
        }
        if (event.getPreviousItem() != null
                && ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.FERAL_CLAWS)) {
            involvesFeralClaws = true;
        }

        boolean involvesPowerGlove = false;

        if (event.getNewItem() != null && ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.POWER_GLOVE)) {
            involvesPowerGlove = true;
        }
        if (event.getPreviousItem() != null
                && ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.POWER_GLOVE)) {
            involvesPowerGlove = true;
        }

        boolean involvesDiggingClaws = false;

        if (event.getNewItem() != null && ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.DIGGING_CLAWS)) {
            involvesDiggingClaws = true;
        }
        if (event.getPreviousItem() != null
                && ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.DIGGING_CLAWS)) {
            involvesDiggingClaws = true;
        }

        // If the crystal heart was involved, run a general update check
        if (involvesCrystalHeart) {
            updateCrystalHeartHealth(event.getPlayer());
        }

        if (involvesFeralClaws) {
            updateFeralClawsAttackSpeed(event.getPlayer());
        }

        if (involvesPowerGlove) {
            updatePowerGloveAttackDamage(event.getPlayer());
        }

        if (involvesDiggingClaws) {
            updateDiggingClawsSpeed(event.getPlayer());
        }

        boolean involvesSteadfastSpikes = false;
        if (event.getNewItem() != null && ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.STEADFAST_SPIKES)) {
            involvesSteadfastSpikes = true;
        }
        if (event.getPreviousItem() != null
                && ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.STEADFAST_SPIKES)) {
            involvesSteadfastSpikes = true;
        }

        if (involvesSteadfastSpikes) {
            updateSteadfastSpikesKnockback(event.getPlayer());
        }
        boolean involvesShrinking = false;
        if (event.getNewItem() != null
                && ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.CHARM_OF_SHRINKING)) {
            involvesShrinking = true;
        }
        if (event.getPreviousItem() != null
                && ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.CHARM_OF_SHRINKING)) {
            involvesShrinking = true;
        }
        if (involvesShrinking) {
            updateCharmOfShrinkingScale(event.getPlayer());
        }

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Ensure attribute is correct on login (some versions reset it)
        updateFeralClawsAttackSpeed(event.getPlayer());
        updatePowerGloveAttackDamage(event.getPlayer());
        updateDiggingClawsSpeed(event.getPlayer());
        updateSteadfastSpikesKnockback(event.getPlayer());
        updateCharmOfShrinkingScale(event.getPlayer());
        warpDriveNoDamage.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Apply health boost from Crystal Heart
     */
    private void updateCrystalHeartHealth(Player player) {
        // FIX 3: Use your VersionUtil to prevent cross-version attribute crashes
        Attribute maxHealthAttr = VersionUtil.getMaxHealthAttribute();
        if (maxHealthAttr == null)
            return;

        // If the item is disabled or they don't have it equipped, remove the health
        if (!plugin.getArtifactConfig().isItemEnabled("crystal_heart") ||
                !ArtifactUtil.hasEquipped(player, ArtifactItem.CRYSTAL_HEART, api)) {

            removeCrystalHeartHealth(player);
            return;
        }

        int bonusHealth = plugin.getArtifactConfig().getCrystalHeartBonusHealth() * 2;
        double currentMaxHealth = player.getAttribute(maxHealthAttr).getBaseValue();

        // Only apply if not already applied
        if (currentMaxHealth < (20.0 + bonusHealth)) {
            player.getAttribute(maxHealthAttr).setBaseValue(20.0 + bonusHealth);
        }
    }

    /**
     * Remove health boost from Crystal Heart
     */
    private void removeCrystalHeartHealth(Player player) {
        Attribute maxHealthAttr = VersionUtil.getMaxHealthAttribute();
        if (maxHealthAttr == null)
            return;

        double currentMaxHealth = player.getAttribute(maxHealthAttr).getBaseValue();
        double baseHealth = 20.0;

        // Only remove if health is above base
        if (currentMaxHealth > baseHealth) {
            player.getAttribute(maxHealthAttr).setBaseValue(baseHealth);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();

        if (ArtifactUtil.hasEquipped(player, ArtifactItem.CHORUS_TOTEM, api)) {
            event.setCancelled(false);
            ItemStack totem = plugin.getItemRegistry().createItemStack(ArtifactItem.CHORUS_TOTEM.getId());
            triggerChorusTotemEffect(player, totem);
            ArtifactUtil.removeArtifact(player, ArtifactItem.CHORUS_TOTEM, api);
            Location safeLocation = findSafeChorusLocation(player.getLocation());
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);

            // Delay teleport by 1 tick to ensure resurrection completes first
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.teleport(safeLocation, PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT);
                player.getWorld().playSound(safeLocation, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
            });
        }
    }

    private void triggerChorusTotemEffect(Player player, ItemStack totem) {
        ItemStack MainHandItem = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(totem);
        player.playEffect(EntityEffect.TOTEM_RESURRECT);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getInventory().setItemInMainHand(MainHandItem);
        }, 1L);
    }

    /**
     * Finds a safe location within a 16-block radius for teleportation.
     * A safe location requires a solid block to stand on and a 2-block high
     * passable space.
     */
    private Location findSafeChorusLocation(Location location) {
        World world = location.getWorld();
        if (world == null)
            return location;

        List<Location> safeSpots = new ArrayList<>();

        // Scan a grid around the player (32x32 area)
        // We use a step of 2 to cover more ground quickly, but 1 is more thorough.
        // Let's use 2 but increase the vertical scan.
        for (int x = -16; x <= 16; x += 2) {
            for (int z = -16; z <= 16; z += 2) {
                // Scan from 16 blocks above to 16 blocks below
                for (int y = 16; y >= -16; y--) {
                    Location dest = location.clone().add(x + 0.5, y, z + 0.5);
                    if (isSafeLocation(dest)) {
                        safeSpots.add(dest);
                        break; // Found a floor at this X/Z, move to next column
                    }
                }
            }
        }

        if (safeSpots.isEmpty()) {
            return location;
        }

        // Shuffle to avoid always teleporting to the same relative corner if all spots
        // are equal
        java.util.Collections.shuffle(safeSpots);

        // Find the spot with the fewest mobs nearby
        Location bestSpot = safeSpots.get(0);
        int minMobs = Integer.MAX_VALUE;

        // Limit the number of spots we check for mobs to keep performance high
        int spotsToCheck = Math.min(safeSpots.size(), 20);
        for (int i = 0; i < spotsToCheck; i++) {
            Location spot = safeSpots.get(i);
            int mobCount = 0;
            // Count living entities in a 6 block radius
            java.util.Collection<org.bukkit.entity.Entity> nearby = world.getNearbyEntities(spot, 6, 6, 6);
            for (org.bukkit.entity.Entity entity : nearby) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    mobCount++;
                }
            }

            if (mobCount < minMobs) {
                minMobs = mobCount;
                bestSpot = spot;
                if (minMobs == 0)
                    break; // Perfect spot found
            }
        }

        return bestSpot;
    }

    /**
     * Checks if a location is safe for a player to teleport to.
     */
    private boolean isSafeLocation(Location loc) {
        Block feet = loc.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block floor = feet.getRelative(BlockFace.DOWN);

        // Body space must be passable (air, grass, etc.)
        if (!feet.isPassable() || !head.isPassable()) {
            return false;
        }

        // Must have a solid block to stand on
        if (!floor.getType().isSolid()) {
            return false;
        }

        // Avoid hazardous blocks
        Material feetType = feet.getType();
        if (feetType == Material.LAVA || feetType == Material.FIRE || feet.isLiquid()) {
            return false;
        }

        // Avoid suffocating in non-passable blocks (though isPassable covers most)
        if (head.getType().isOccluding() || feet.getType().isOccluding()) {
            return false;
        }

        return true;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (ArtifactUtil.hasEquipped(player, ArtifactItem.DIGGING_CLAWS, api)) {
            Block block = event.getBlock();
            if (isStoneTierBlock(block.getType())) {
                event.setDropItems(false);
                block.breakNaturally(new ItemStack(Material.STONE_PICKAXE));
            }
        }

        if (plugin.getArtifactConfig().isItemEnabled("pickaxe_heater") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.PICKAXE_HEATER, api)) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem.containsEnchantment(Enchantment.SILK_TOUCH))
                return;

            Block block = event.getBlock();
            java.util.Collection<ItemStack> drops = block.getDrops(handItem);
            if (drops.isEmpty())
                return;

            boolean smeltedAny = false;
            java.util.List<ItemStack> finalDrops = new java.util.ArrayList<>();

            for (ItemStack drop : drops) {
                Material smelted = smeltingMap.get(drop.getType());
                if (smelted != null) {
                    finalDrops.add(new ItemStack(smelted, drop.getAmount()));
                    smeltedAny = true;
                } else {
                    finalDrops.add(drop);
                }
            }

            if (smeltedAny) {
                event.setDropItems(false);
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                for (ItemStack item : finalDrops) {
                    block.getWorld().dropItemNaturally(loc, item);
                }
                try {
                    block.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc, 5, 0.2, 0.2, 0.2, 0.05);
                } catch (Throwable ignored) {
                }
                block.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.5f);
            }
        }
    }

    private boolean isStoneTierBlock(Material type) {
        return plugin.getStoneTierManager().isStoneTierBlock(type);
    }

    private void updateCharmOfShrinkingScale(Player player) {
        if (plugin.getArtifactConfig().isItemEnabled("charm_of_shrinking")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.CHARM_OF_SHRINKING, api)) {
            applyCharmOfShrinkingScale(player);
        } else {
            removeCharmOfShrinkingScale(player);
        }
    }

    private void applyCharmOfShrinkingScale(Player player) {
        Attribute scaleAttr = VersionUtil.getScaleAttribute();
        if (scaleAttr == null)
            return;
        AttributeInstance instance = player.getAttribute(scaleAttr);
        if (instance == null)
            return;

        removeCharmOfShrinkingScale(player);

        AttributeModifier modifier = new AttributeModifier(CHARM_OF_SHRINKING_ID, "Charm of Shrinking Scale", -0.5,
                AttributeModifier.Operation.ADD_SCALAR);
        instance.addModifier(modifier);
    }

    private void removeCharmOfShrinkingScale(Player player) {
        Attribute scaleAttr = VersionUtil.getScaleAttribute();
        if (scaleAttr == null)
            return;
        AttributeInstance instance = player.getAttribute(scaleAttr);
        if (instance == null)
            return;

        for (AttributeModifier modifier : new java.util.ArrayList<>(instance.getModifiers())) {
            if (modifier.getUniqueId().equals(CHARM_OF_SHRINKING_ID)) {
                instance.removeModifier(modifier);
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof EnderPearl) {
            if (event.getEntity().getShooter() instanceof Player) {
                Player player = (Player) event.getEntity().getShooter();
                if (plugin.getArtifactConfig().isItemEnabled("warp_drive") &&
                        ArtifactUtil.hasEquipped(player, ArtifactItem.WARP_DRIVE, api)) {

                    int cost = plugin.getArtifactConfig().getWarpDriveHungerCost();
                    if (player.getFoodLevel() < cost) {
                        // Normal interaction: don't cancel, don't add back.
                        return;
                    }

                    player.setFoodLevel(Math.max(0, player.getFoodLevel() - cost));

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
                    });
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getEntity() instanceof EnderPearl) {
            EnderPearl pearl = (EnderPearl) event.getEntity();
            if (pearl.getShooter() instanceof Player) {
                Player player = (Player) pearl.getShooter();
                if (plugin.getArtifactConfig().isItemEnabled("warp_drive") &&
                        ArtifactUtil.hasEquipped(player, ArtifactItem.WARP_DRIVE, api)) {
                    warpDriveNoDamage.add(player.getUniqueId());
                    // We also keep it in onPlayerTeleport, but this covers us if damage happens
                    // before teleport
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            if (plugin.getArtifactConfig().isItemEnabled("warp_drive") &&
                    ArtifactUtil.hasEquipped(event.getPlayer(), ArtifactItem.WARP_DRIVE, api)) {
                warpDriveNoDamage.add(event.getPlayer().getUniqueId());
                // Keep for a few ticks to ensure we catch the damage event which might be
                // slightly delayed
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> warpDriveNoDamage.remove(event.getPlayer().getUniqueId()), 5L);
            }
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeFeralClawsAttackSpeed(player);
            removePowerGloveAttackDamage(player);
            removeDiggingClawsSpeed(player);
            removeSteadfastSpikesKnockback(player);
            removeCharmOfShrinkingScale(player);
        }
    }

    private void updateFeralClawsAttackSpeed(Player player) {
        // If enabled and equipped, apply. Otherwise, remove.
        if (plugin.getArtifactConfig().isItemEnabled("feral_claws") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.FERAL_CLAWS, api)) {
            applyFeralClawsAttackSpeed(player);
        } else {
            removeFeralClawsAttackSpeed(player);
        }
    }

    private void applyFeralClawsAttackSpeed(Player player) {
        Attribute attackSpeedAttr = VersionUtil.getAttackSpeedAttribute();
        if (attackSpeedAttr == null)
            return;

        AttributeInstance instance = player.getAttribute(attackSpeedAttr);
        if (instance == null)
            return;

        // Ensure we don't double-apply
        removeFeralClawsAttackSpeed(player);

        double amount = plugin.getArtifactConfig().getFeralClawsAttackSpeed();
        AttributeModifier modifier = new AttributeModifier(
                FERAL_CLAWS_ID,
                "Feral Claws Attack Speed",
                amount,
                AttributeModifier.Operation.ADD_NUMBER);
        instance.addModifier(modifier);
    }

    private void removeFeralClawsAttackSpeed(Player player) {
        Attribute attackSpeedAttr = VersionUtil.getAttackSpeedAttribute();
        if (attackSpeedAttr == null)
            return;

        AttributeInstance instance = player.getAttribute(attackSpeedAttr);
        if (instance == null)
            return;

        // Safely remove only our specific modifier
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            if (modifier.getUniqueId().equals(FERAL_CLAWS_ID)) {
                instance.removeModifier(modifier);
            }
        }
    }

    private void updatePowerGloveAttackDamage(Player player) {
        // If enabled and equipped, apply. Otherwise, remove.
        if (plugin.getArtifactConfig().isItemEnabled("power_glove") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.POWER_GLOVE, api)) {
            applyPowerGloveAttackDamage(player);
        } else {
            removePowerGloveAttackDamage(player);
        }
    }

    private void applyPowerGloveAttackDamage(Player player) {
        Attribute damageAttr = VersionUtil.getAttackDamageAttribute();
        if (damageAttr == null)
            return;

        AttributeInstance instance = player.getAttribute(damageAttr);
        if (instance == null)
            return;

        // Ensure we don't double-apply
        removePowerGloveAttackDamage(player);

        double amount = plugin.getArtifactConfig().getPowerGloveAttackDamage();
        AttributeModifier modifier = new AttributeModifier(
                POWER_GLOVE_ID,
                "Power Glove Attack Damage",
                amount,
                AttributeModifier.Operation.ADD_NUMBER);
        instance.addModifier(modifier);
    }

    private void removePowerGloveAttackDamage(Player player) {
        Attribute damageAttr = VersionUtil.getAttackDamageAttribute();
        if (damageAttr == null)
            return;

        AttributeInstance instance = player.getAttribute(damageAttr);
        if (instance == null)
            return;

        // Safely remove only our specific modifier
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            if (modifier.getUniqueId().equals(POWER_GLOVE_ID)) {
                instance.removeModifier(modifier);
            }
        }
    }

    private void updateDiggingClawsSpeed(Player player) {
        // If enabled and equipped, apply. Otherwise, remove.
        if (plugin.getArtifactConfig().isItemEnabled("digging_claws") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.DIGGING_CLAWS, api)) {
            applyDiggingClawsSpeed(player);
        } else {
            removeDiggingClawsSpeed(player);
        }
    }

    private void applyDiggingClawsSpeed(Player player) {
        Attribute speedAttr = VersionUtil.getBlockBreakSpeedAttribute();

        if (speedAttr != null) {
            // Modern 1.20.5+ Attribute approach
            AttributeInstance instance = player.getAttribute(speedAttr);
            if (instance != null) {
                removeDiggingClawsSpeed(player);
                int amplifier = plugin.getArtifactConfig().getDiggingClawsSpeedAmplifier();
                double bonus = 0.2 * (amplifier + 1); // Haste formula: +20% per level (Haste I = 0.2)
                AttributeModifier modifier = new AttributeModifier(
                        DIGGING_CLAWS_ID,
                        "Digging Claws Mining Speed",
                        bonus,
                        AttributeModifier.Operation.ADD_SCALAR);
                instance.addModifier(modifier);
                return;
            }
        }

        // Fallback for older versions: Potion Effect (Haste)
        int amplifier = plugin.getArtifactConfig().getDiggingClawsSpeedAmplifier();
        org.bukkit.potion.PotionEffectType haste = org.bukkit.potion.PotionEffectType.getByName("FAST_DIGGING");
        if (haste == null)
            haste = org.bukkit.potion.PotionEffectType.getByName("HASTE");

        if (haste != null) {
            player.addPotionEffect(
                    new org.bukkit.potion.PotionEffect(haste, Integer.MAX_VALUE, amplifier, false, false, true));
        }
    }

    private void removeDiggingClawsSpeed(Player player) {
        Attribute speedAttr = VersionUtil.getBlockBreakSpeedAttribute();

        // 1. Remove Attribute Modifier if it exists
        if (speedAttr != null) {
            AttributeInstance instance = player.getAttribute(speedAttr);
            if (instance != null) {
                for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                    if (modifier.getUniqueId().equals(DIGGING_CLAWS_ID)) {
                        instance.removeModifier(modifier);
                    }
                }
            }
        }

        // 2. Remove Potion Effect if it exists
        org.bukkit.potion.PotionEffectType haste = org.bukkit.potion.PotionEffectType.getByName("FAST_DIGGING");
        if (haste == null)
            haste = org.bukkit.potion.PotionEffectType.getByName("HASTE");

        if (haste != null && player.hasPotionEffect(haste)) {
            // Only remove if it's our "permanent" one (duration > 1 hour)
            org.bukkit.potion.PotionEffect effect = player.getPotionEffect(haste);
            if (effect != null && effect.getDuration() > 72000) {
                player.removePotionEffect(haste);
            }
        }
    }

    @EventHandler
    public void onFireGauntletHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;

        Player player = (Player) event.getDamager();
        if (plugin.getArtifactConfig().isItemEnabled("fire_gauntlet") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.FIRE_GAUNTLET, api)) {
            LivingEntity victim = (LivingEntity) event.getEntity();
            int tick = plugin.getArtifactConfig().getFireGauntletFireDuration();
            victim.setFireTicks(tick * 20);
            Vector direction = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            victim.setVelocity(victim.getVelocity().add(direction.multiply(0.8).setY(0.2)));
        }

        if (plugin.getArtifactConfig().isItemEnabled("pocket_piston") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.POCKET_PISTON, api)) {
            LivingEntity victim = (LivingEntity) event.getEntity();
            double amplifier = plugin.getArtifactConfig().getPocketPistonKnockbackAmplifier();
            Vector direction = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            direction.multiply(amplifier).setY(0.25);
            victim.setVelocity(victim.getVelocity().add(direction));
        }

        if (plugin.getArtifactConfig().isItemEnabled("vampiric_glove") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.VAMPIRIC_GLOVE, api)) {
            double healthAbsorbed = plugin.getArtifactConfig().getVampiricGloveHealthAbsorbed();
            double healthToHeal = event.getDamage() * healthAbsorbed;
            double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + healthToHeal);
            player.setHealth(newHealth);
        }

        if (plugin.getArtifactConfig().isItemEnabled("withered_bracelet") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.WITHERED_BRACELET, api)) {
            if (Math.random() < plugin.getArtifactConfig().getWitheredBraceletWitherChance()) {
                LivingEntity victim = (LivingEntity) event.getEntity();
                int tick = plugin.getArtifactConfig().getWitheredBraceletWitherDuration();
                double amplifier = plugin.getArtifactConfig().getWitheredBraceletWitherAmplifier();
                victim.addPotionEffect(
                        new PotionEffect(PotionEffectType.WITHER, tick * 20, (int) amplifier, false, false));
            }
        }
    }

    @EventHandler
    public void onWarpDriveDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();

        if (event.getDamager() instanceof EnderPearl) {
            if (warpDriveNoDamage.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.getKiller() == null)
            return;
        Player player = victim.getKiller();
        if (player != null && plugin.getArtifactConfig().isItemEnabled("golden_hook") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.GOLDEN_HOOK, api)) {
            int OriginalExp = event.getDroppedExp();
            if (OriginalExp == 0)
                return;
            int BonusExp = (int) Math.round(OriginalExp * plugin.getArtifactConfig().getGoldenHookExpBonus());
            event.setDroppedExp(OriginalExp + BonusExp);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item.getType().isEdible() && plugin.getArtifactConfig().isItemEnabled("onion_ring") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.ONION_RING, api)) {
            int amplifier = plugin.getArtifactConfig().getOnionRingSpeedAmplifier();
            int duration = plugin.getArtifactConfig().getOnionRingDurationSeconds();
            org.bukkit.potion.PotionEffectType haste = org.bukkit.potion.PotionEffectType.getByName("FAST_DIGGING");
            if (haste == null)
                haste = org.bukkit.potion.PotionEffectType.getByName("HASTE");
            if (haste != null) {
                player.addPotionEffect(
                        new org.bukkit.potion.PotionEffect(haste, duration * 20, amplifier, false, false, true));
            }
        }
    }

    @EventHandler
    public void onPlayerFallDamage(EntityDamageEvent event) {
        if (plugin.getArtifactConfig().isBunnyHoppersFallDamageImmune() == true) {
            if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                Player player = (Player) event.getEntity();
                if (plugin.getArtifactConfig().isItemEnabled("bunny_hoppers") &&
                        ArtifactUtil.hasEquipped(player, ArtifactItem.BUNNY_HOPPERS, api)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void updateSteadfastSpikesKnockback(Player player) {
        if (plugin.getArtifactConfig().isItemEnabled("steadfast_spikes") &&
                ArtifactUtil.hasEquipped(player, ArtifactItem.STEADFAST_SPIKES, api)) {
            applySteadfastSpikesKnockback(player);
        } else {
            removeSteadfastSpikesKnockback(player);
            removeCharmOfShrinkingScale(player);
        }
    }

    private void applySteadfastSpikesKnockback(Player player) {
        Attribute attr = VersionUtil.getKnockbackResistanceAttribute();
        if (attr == null)
            return;

        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null)
            return;

        removeSteadfastSpikesKnockback(player);

        double amount = plugin.getArtifactConfig().getSteadfastSpikesKnockbackAmplifier();
        AttributeModifier modifier = new AttributeModifier(
                STEADFAST_SPIKES_ID,
                "Steadfast Spikes Knockback Resistance",
                amount,
                AttributeModifier.Operation.ADD_NUMBER);
        instance.addModifier(modifier);
    }

    private void removeSteadfastSpikesKnockback(Player player) {
        Attribute attr = VersionUtil.getKnockbackResistanceAttribute();
        if (attr == null)
            return;

        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null)
            return;

        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            if (modifier.getUniqueId().equals(STEADFAST_SPIKES_ID)) {
                instance.removeModifier(modifier);
            }
        }
    }
}