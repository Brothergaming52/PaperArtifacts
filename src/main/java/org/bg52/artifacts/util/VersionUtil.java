package org.bg52.artifacts.util;

import java.util.HashMap;
import java.util.Map;

import org.bg52.artifacts.entity.MimicModel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Version detection and cross-version compatibility for Artifacts plugin.
 * Supports Minecraft 1.14 through 1.21+.
 */
public class VersionUtil {

    private static int majorVersion = -1;
    private static int minorVersion = -1;
    private static int patchVersion = -1;
    private static Boolean supportsItemModel = null;

    // Reflection cache for setItemModel
    private static java.lang.reflect.Method setItemModelMethod = null;
    private static boolean setItemModelMethodChecked = false;

    static {
        parseVersion();
    }

    private static void parseVersion() {
        try {
            String versionString = Bukkit.getBukkitVersion();
            String[] parts = versionString.split("-")[0].split("\\.");
            if (parts.length >= 2) {
                majorVersion = Integer.parseInt(parts[0]);
                minorVersion = Integer.parseInt(parts[1]);
            }
            if (parts.length >= 3) {
                patchVersion = Integer.parseInt(parts[2]);
            } else {
                patchVersion = 0;
            }
        } catch (Exception e) {
            majorVersion = 1;
            minorVersion = 14;
            patchVersion = 0;
        }
    }

    public static int getMajorVersion() {
        return majorVersion;
    }

    public static int getMinorVersion() {
        return minorVersion;
    }

    public static int getPatchVersion() {
        return patchVersion;
    }

    public static boolean isAtLeast(int major, int minor, int patch) {
        if (majorVersion > major)
            return true;
        if (majorVersion < major)
            return false;
        if (minorVersion > minor)
            return true;
        if (minorVersion < minor)
            return false;
        return patchVersion >= patch;
    }

    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    public static String getVersionString() {
        return majorVersion + "." + minorVersion + "." + patchVersion;
    }

    /**
     * Check if ItemMeta.setItemModel(NamespacedKey) is available (1.21.3+).
     */
    public static boolean supportsItemModel() {
        if (supportsItemModel == null) {
            if (!isAtLeast(1, 21, 3)) {
                supportsItemModel = false;
            } else {
                try {
                    Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                    itemMetaClass.getMethod("setItemModel", NamespacedKey.class);
                    supportsItemModel = true;
                } catch (Exception e) {
                    supportsItemModel = false;
                }
            }
        }
        return supportsItemModel;
    }

    /**
     * Set the mimic model on a carrot on a stick's ItemMeta.
     * Always sets CustomModelData for backwards compatibility.
     * Additionally sets item model on 1.21.3+ for modern resource packs.
     */
    @SuppressWarnings("deprecation")
    public static void setMimicModel(ItemMeta meta, MimicModel model) {
        if (meta == null || model == null)
            return;

        // Always set CustomModelData for all versions
        meta.setCustomModelData(model.getCustomModelData());

        // Additionally set item model on 1.21.3+
        if (supportsItemModel()) {
            try {
                if (!setItemModelMethodChecked) {
                    setItemModelMethodChecked = true;
                    Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                    setItemModelMethod = itemMetaClass.getMethod("setItemModel", NamespacedKey.class);
                }
                if (setItemModelMethod != null) {
                    NamespacedKey key = new NamespacedKey("artifacts", model.getItemModelKey());
                    setItemModelMethod.invoke(meta, key);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Artifacts] Failed to set item model via reflection: " + e.getMessage());
            }
        }
    }

    /**
     * Get the max health Attribute safely across versions.
     * In 1.21.2+ it was renamed from GENERIC_MAX_HEALTH to MAX_HEALTH.
     */
    public static Attribute getMaxHealthAttribute() {
        for (Attribute attr : Attribute.values()) {
            String name = attr.name();
            if (name.equals("GENERIC_MAX_HEALTH") || name.equals("MAX_HEALTH")) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Get the attack damage Attribute safely across versions.
     */
    public static Attribute getAttackDamageAttribute() {
        for (Attribute attr : Attribute.values()) {
            String name = attr.name();
            if (name.equals("GENERIC_ATTACK_DAMAGE") || name.equals("ATTACK_DAMAGE")) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Get the attack speed Attribute safely across versions.
     */
    public static Attribute getAttackSpeedAttribute() {
        for (Attribute attr : Attribute.values()) {
            String name = attr.name();
            if (name.equals("GENERIC_ATTACK_SPEED") || name.equals("ATTACK_SPEED")) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Get the block break speed Attribute safely across versions.
     * Introduced in 1.20.5.
     */
    public static Attribute getBlockBreakSpeedAttribute() {
        for (Attribute attr : Attribute.values()) {
            String name = attr.name();
            if (name.equals("GENERIC_BLOCK_BREAK_SPEED") || name.equals("PLAYER_BLOCK_BREAK_SPEED")
                    || name.equals("BLOCK_BREAK_SPEED")) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Get the scale Attribute safely across versions.
     * Introduced in 1.20.5.
     */
    public static Attribute getScaleAttribute() {
        for (Attribute attr : Attribute.values()) {
            String name = attr.name();
            if (name.equals("GENERIC_SCALE") || name.equals("SCALE")) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Get the movement speed Attribute safely across versions.
     */
    public static Attribute getMovementSpeedAttribute() {
        for (Attribute attr : Attribute.values()) {
            String name = attr.name();
            if (name.equals("GENERIC_MOVEMENT_SPEED") || name.equals("MOVEMENT_SPEED")) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Get the step height Attribute safely across versions.
     * Introduced in 1.20.5.
     */
    public static Attribute getStepHeightAttribute() {
        for (Attribute attr : Attribute.values()) {
            String name = attr.name();
            if (name.equals("GENERIC_STEP_HEIGHT") || name.equals("STEP_HEIGHT")) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Get the knockback resistance Attribute safely across versions.
     */
    public static Attribute getKnockbackResistanceAttribute() {
        for (Attribute attr : Attribute.values()) {
            String name = attr.name();
            if (name.equals("GENERIC_KNOCKBACK_RESISTANCE") || name.equals("KNOCKBACK_RESISTANCE")) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Set whether an entity can walk on powdered snow safely across versions.
     * Method introduced in 1.17.
     */
    public static void setCanWalkOnPowderedSnow(org.bukkit.entity.Entity entity, boolean canWalk) {
        if (entity == null)
            return;
        try {
            java.lang.reflect.Method method = entity.getClass().getMethod("setCanWalkOnPowderedSnow", boolean.class);
            method.invoke(entity, canWalk);
        } catch (Exception ignored) {
            // Method not available on this version or entity type
        }
    }

    // Fallback table: newer Material -> older fallback (tried in order of version
    // addition)
    private static final Map<String, String> FALLBACKS = new HashMap<>();

    static {
        // 1.17+ blocks
        FALLBACKS.put("COBBLED_DEEPSLATE", "COBBLESTONE");
        FALLBACKS.put("DEEPSLATE", "STONE");
        FALLBACKS.put("COBBLED_DEEPSLATE_WALL", "COBBLESTONE_WALL");
        FALLBACKS.put("DEEPSLATE_BRICK_WALL", "STONE_BRICK_WALL");
        FALLBACKS.put("CAVE_AIR", "AIR");
        FALLBACKS.put("CANDLE", "TORCH");
        FALLBACKS.put("SOUL_CAMPFIRE", "CAMPFIRE");

        // 1.16+ blocks
        FALLBACKS.put("CAMPFIRE", "TORCH");
        FALLBACKS.put("SOUL_LANTERN", "LANTERN");

        // 1.16+ block
        FALLBACKS.put("LANTERN", "TORCH");

        // 1.14+ blocks
        FALLBACKS.put("CARTOGRAPHY_TABLE", "CRAFTING_TABLE");
        FALLBACKS.put("FLETCHING_TABLE", "CRAFTING_TABLE");
        FALLBACKS.put("SMITHING_TABLE", "CRAFTING_TABLE");
        FALLBACKS.put("BARREL", "CHEST");
        FALLBACKS.put("BLAST_FURNACE", "FURNACE");
        FALLBACKS.put("SMOKER", "FURNACE");
        FALLBACKS.put("POTTED_BAMBOO", "POTTED_DEAD_BUSH");
        FALLBACKS.put("STONE_BRICK_WALL", "COBBLESTONE_WALL");

        // 1.13 renames handled by trying via Material.matchMaterial
    }

    public static Material of(String name) {
        String candidate = name.toUpperCase().replace("MINECRAFT:", "");
        while (candidate != null) {
            Material m = Material.matchMaterial(candidate);
            if (m != null && m != Material.AIR || "AIR".equals(candidate) || "CAVE_AIR".equals(candidate)) {
                if (m == null)
                    return Material.AIR;
                return m;
            }
            candidate = FALLBACKS.get(candidate);
        }
        return null; // block genuinely not available — caller skips it
    }

    /** Safe get — returns AIR if not found */
    public static Material safeOf(String name) {
        Material m = of(name);
        return m != null ? m : Material.AIR;
    }

    public static boolean exists(String name) {
        return of(name) != null;
    }

    /** Returns CAVE_AIR on 1.17+, otherwise AIR */
    public static Material caveAir() {
        return safeOf("CAVE_AIR");
    }
}
