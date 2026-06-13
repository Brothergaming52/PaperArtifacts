package org.bg52.artifacts.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FoodUtil {

    private static final Map<Material, FoodData> FOOD_REGISTRY = new EnumMap<>(Material.class);

    /**
     * Loads the food info from FoodInfo.yml.
     */
    public static void loadFoodInfo(JavaPlugin plugin) {
        FOOD_REGISTRY.clear();

        File customFile = new File(plugin.getDataFolder(), "FoodInfo.yml");
        YamlConfiguration yaml = null;

        if (customFile.exists()) {
            yaml = YamlConfiguration.loadConfiguration(customFile);
        } else {
            InputStream in = plugin.getResource("FoodInfo.yml");
            if (in != null) {
                yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
            }
        }

        if (yaml == null) {
            plugin.getLogger().warning("Could not find FoodInfo.yml! Food effects will not work natively.");
            return;
        }

        for (String key : yaml.getKeys(false)) {
            try {
                Material mat = Material.matchMaterial(key);
                if (mat == null) continue;

                int nutrition = yaml.getInt(key + ".nutrition");
                float saturationModifier = (float) yaml.getDouble(key + ".saturation");

                List<PotionEffect> effects = new ArrayList<>();
                float chance = 1.0f;

                if (yaml.isConfigurationSection(key + ".effects")) {
                    ConfigurationSection effectsSection = yaml.getConfigurationSection(key + ".effects");
                    if (effectsSection != null) {
                        for (String effectKey : effectsSection.getKeys(false)) {
                            String path = key + ".effects." + effectKey;
                            String typeName = yaml.getString(path + ".type");
                            if (typeName != null) {
                                PotionEffectType type = PotionEffectType.getByName(typeName);
                                if (type == null) {
                                    for (PotionEffectType pet : PotionEffectType.values()) {
                                        if (pet != null && pet.getName().replace("_", "").equalsIgnoreCase(typeName.replace("_", ""))) {
                                            type = pet;
                                            break;
                                        }
                                    }
                                }
                                if (type != null) {
                                    int duration = yaml.getInt(path + ".duration_ticks");
                                    int amplifier = yaml.getInt(path + ".amplifier");
                                    float effectChance = (float) yaml.getDouble(path + ".chance", 1.0);
                                    effects.add(new PotionEffect(type, duration, amplifier));
                                    chance = effectChance;
                                }
                            }
                        }
                    }
                }
                
                FOOD_REGISTRY.put(mat, new FoodData(nutrition, saturationModifier, effects, chance));
            } catch (Exception e) {
                // Ignore whatever values are there that caused an issue
            }
        }

        plugin.getLogger().info("Loaded " + FOOD_REGISTRY.size() + " foods from FoodInfo.yml.");
    }

    /**
     * Main entry point to apply food or drink effects to a player.
     */
    public static void applyFoodEffects(Player player, ItemStack item) {
        if (item == null)
            return;
        Material type = item.getType();
        ItemMeta meta = item.getItemMeta();

        // 1. Handle Suspicious Stew
        if (meta instanceof SuspiciousStewMeta) {
            SuspiciousStewMeta stewMeta = (SuspiciousStewMeta) meta;
            // Stew always restores 6 hunger and 7.2 saturation base
            applyHungerAndSaturation(player, 6, 7.2f);

            if (stewMeta.hasCustomEffects()) {
                for (PotionEffect effect : stewMeta.getCustomEffects()) {
                    player.addPotionEffect(effect);
                }
            }
            return;
        }

        // 2. Handle Drinks / Potions
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) meta;

            // Apply custom effects (if it's a custom SMP potion)
            if (potionMeta.hasCustomEffects()) {
                for (PotionEffect effect : potionMeta.getCustomEffects()) {
                    player.addPotionEffect(effect);
                }
            }

            // Apply base vanilla potion effects (e.g., standard Night Vision, Strength)
            // Note: In 1.14-1.20.4, translating PotionType to PotionEffect requires
            // mapping.
            PotionType baseType = potionMeta.getBasePotionData().getType();
            if (baseType != PotionType.WATER && baseType != PotionType.UNCRAFTABLE && baseType != PotionType.AWKWARD
                    && baseType != PotionType.THICK && baseType != PotionType.MUNDANE) {
                PotionEffectType effectType = baseType.getEffectType();
                if (effectType != null) {
                    // You have to calculate duration based on if it's upgraded/extended
                    int duration = potionMeta.getBasePotionData().isExtended() ? 9600 : 3600; // Rough estimates for
                                                                                              // ticks
                    int amplifier = potionMeta.getBasePotionData().isUpgraded() ? 1 : 0;
                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                }
            }
            
            if (type.name().equals("HONEY_BOTTLE")) {
                player.removePotionEffect(PotionEffectType.POISON);
            }
            
            return;
        }

        // 3. Handle Standard Solid Foods
        if (FOOD_REGISTRY.containsKey(type)) {
            FoodData data = FOOD_REGISTRY.get(type);

            // Apply Hunger & Saturation
            float actualSaturation = data.hunger * data.saturationModifier * 2.0f;
            applyHungerAndSaturation(player, data.hunger, actualSaturation);
            
            if (type.name().equals("HONEY_BOTTLE")) {
                player.removePotionEffect(PotionEffectType.POISON);
            }

            // Apply any bonus effects conditionally based on chance
            if (data.effects != null && !data.effects.isEmpty()) {
                if (Math.random() <= data.effectChance) {
                    for (PotionEffect effect : data.effects) {
                        player.addPotionEffect(effect);
                    }
                }
            }
        }
    }

    /**
     * Safely applies hunger and saturation, respecting vanilla maximums.
     */
    private static void applyHungerAndSaturation(Player player, int foodAdd, float satAdd) {
        // Cap hunger at 20
        int currentFood = player.getFoodLevel();
        int newFood = Math.min(20, currentFood + foodAdd);
        player.setFoodLevel(newFood);

        // Saturation can NEVER exceed the player's current food level in vanilla
        float currentSat = player.getSaturation();
        float newSat = Math.min(newFood, currentSat + satAdd);
        player.setSaturation(newSat);
    }

    /**
     * Data object to hold food stats.
     */
    private static class FoodData {
        final int hunger;
        final float saturationModifier;
        final List<PotionEffect> effects;
        final float effectChance;

        FoodData(int hunger, float saturationModifier, List<PotionEffect> effects, float effectChance) {
            this.hunger = hunger;
            this.saturationModifier = saturationModifier;
            this.effects = effects;
            this.effectChance = effectChance;
        }
    }
}