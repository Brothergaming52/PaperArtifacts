package org.bg52.artifacts.item;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.util.VersionUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Registers all artifact items with the CuriosPaper API and provides
 * methods to create ItemStacks with correct CMD, item models, and metadata.
 *
 * Registration happens once on plugin enable. If the item already exists
 * in CuriosPaper's data store (e.g. from a previous run), it skips creation
 * but still ensures the item is usable.
 */
public class ItemRegistry {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final Logger logger;

    // Reflection cache for setItemModel
    private static java.lang.reflect.Method setItemModelMethod = null;
    private static boolean setItemModelMethodChecked = false;

    public ItemRegistry(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.logger = plugin.getLogger();
    }

    /**
     * Register all artifact items with CuriosPaper.
     * Only creates items that don't already exist.
     */
    public void registerAll() {
        int created = 0;
        int skipped = 0;

        java.util.Map<String, List<org.bg52.curiospaper.data.LootTableData>> lootTables = ArtifactLootTables
                .getLootTables(plugin);

        java.util.Map<String, List<org.bg52.curiospaper.data.MobDropData>> mobDrops = ArtifactMobDrops
                .getMobDrops(plugin);

        for (ArtifactItem artifact : ArtifactItem.values()) {
            // Check if disabled in config
            if (!plugin.getArtifactConfig().isItemEnabled(artifact.getId())) {
                logger.info("Skipping disabled item: " + artifact.getId());
                skipped++;
                continue;
            }

            if (artifact == ArtifactItem.CHARM_OF_SHRINKING && !VersionUtil.isAtLeast(1, 20, 5)) {
                skipped++;
                logger.info("Skipping Charm of Shrinking: Not supported on this Minecraft version");
                continue;
            }

            // Create the item in CuriosPaper's data system if it doesn't already exist
            org.bg52.curiospaper.data.ItemData itemData = api.getItemData(artifact.getId());
            if (itemData == null) {
                itemData = api.createItem(plugin, artifact.getId());
                if (itemData == null) {
                    logger.warning("Failed to create item: " + artifact.getId());
                    continue;
                }

                itemData.setDisplayName(artifact.getDisplayName());
                itemData.setMaterial(artifact.getBaseMaterial().name());
                itemData.setCustomModelData(artifact.getCustomModelData());
                itemData.setItemModel("artifacts:" + artifact.getItemModelKey());
                itemData.setLore(artifact.getLore());

                if (artifact.getSlotType() != null) {
                    itemData.setSlotType(artifact.getSlotType());
                }

                if (lootTables.containsKey(artifact.getId())) {
                    for (org.bg52.curiospaper.data.LootTableData lootTable : lootTables.get(artifact.getId())) {
                        api.registerItemLootTable(artifact.getId(), lootTable);
                    }
                }

                if (mobDrops.containsKey(artifact.getId())) {
                    for (org.bg52.curiospaper.data.MobDropData mobDrop : mobDrops.get(artifact.getId())) {
                        api.registerItemMobDrop(artifact.getId(), mobDrop);
                    }
                }

                api.saveItemData(artifact.getId());
                created++;
            } else {
                skipped++;
            }

            // Always ensure 3D model configuration is up-to-date
            if (artifact.has3DModel()) {
                api.setItemModelConfig(artifact.getId(), true, artifact.getBaseMaterial().name(),
                        artifact.get3DCustomModelData(), "artifacts:" + artifact.get3DItemModelKey(),
                        artifact.getPitchUpLimit(), artifact.getPitchDownLimit());

                if (mobDrops.containsKey(artifact.getId())) {
                    for (org.bg52.curiospaper.data.MobDropData mobDrop : mobDrops.get(artifact.getId())) {
                        api.setMobDropModelConfig(artifact.getId(), mobDrop.getEntityType(), true,
                                artifact.getBaseMaterial().name(), artifact.get3DCustomModelData(),
                                "artifacts:" + artifact.get3DItemModelKey());
                    }
                }
                api.saveItemData(artifact.getId());
            }

            if (artifact == ArtifactItem.ETERNAL_STEAK) {
                // Furnace
                org.bg52.curiospaper.data.RecipeData furnaceRecipe = new org.bg52.curiospaper.data.RecipeData(
                        org.bg52.curiospaper.data.RecipeData.RecipeType.FURNACE);
                furnaceRecipe.setInputItem(ArtifactItem.EVERLASTING_BEEF.getId());
                furnaceRecipe.setCookingTime(plugin.getArtifactConfig().getEternalSteakFurnaceTime() * 20);
                furnaceRecipe.setExperience(0.35f);
                api.registerItemRecipe(artifact.getId(), furnaceRecipe);

                // Smoker
                org.bg52.curiospaper.data.RecipeData smokerRecipe = new org.bg52.curiospaper.data.RecipeData(
                        org.bg52.curiospaper.data.RecipeData.RecipeType.SMOKER);
                smokerRecipe.setInputItem(ArtifactItem.EVERLASTING_BEEF.getId());
                smokerRecipe.setCookingTime(plugin.getArtifactConfig().getEternalSteakSmokerTime() * 20);
                smokerRecipe.setExperience(0.35f);
                api.registerItemRecipe(artifact.getId(), smokerRecipe);

                // campfire
                org.bg52.curiospaper.data.RecipeData campfireRecipe = new org.bg52.curiospaper.data.RecipeData(
                        org.bg52.curiospaper.data.RecipeData.RecipeType.CAMPFIRE);
                campfireRecipe.setInputItem(ArtifactItem.EVERLASTING_BEEF.getId());
                campfireRecipe.setCookingTime(plugin.getArtifactConfig().getEternalSteakCampfireTime() * 20);
                campfireRecipe.setExperience(0.35f);
                api.registerItemRecipe(artifact.getId(), campfireRecipe);
            }

            created++;
        }

        logger.info("Item registration complete: " + created + " created, " + skipped + " skipped.");
    }

    /**
     * Create an ItemStack for the given artifact with all correct metadata.
     * This method builds the stack manually for maximum control, rather
     * than delegating to the CuriosPaper API's createItemStack()
     * (which doesn't handle shield unbreakable flags, etc.).
     */
    public ItemStack createItemStack(ArtifactItem artifact) {
        if (artifact == null)
            return null;

        ItemStack item = new ItemStack(artifact.getBaseMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        // Display name
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', artifact.getDisplayName()));

        // Lore
        List<String> coloredLore = new ArrayList<String>();
        for (String line : artifact.getLore()) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(coloredLore);

        // CustomModelData (always set for backwards compatibility)
        meta.setCustomModelData(artifact.getCustomModelData());

        // Item model (1.21.3+)
        if (VersionUtil.supportsItemModel()) {
            setItemModelSafe(meta, "artifacts:" + artifact.getItemModelKey());
        }

        // Special flags for specific items
        if (artifact == ArtifactItem.UMBRELLA) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        } else if (artifact == ArtifactItem.EVERLASTING_BEEF || artifact == ArtifactItem.ETERNAL_STEAK) {
            // Attempt to set MaxStackSize natively (1.20.5+)
            try {
                java.lang.reflect.Method setMaxStackSize = meta.getClass().getMethod("setMaxStackSize", int.class);
                setMaxStackSize.invoke(meta, 1);
            } catch (Exception e) {
                // Fallback for older versions: add a random UUID to NBT so they won't stack
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "unstackable_id"),
                        org.bukkit.persistence.PersistentDataType.STRING,
                        java.util.UUID.randomUUID().toString());
            }
        }

        // Tag with curios_custom_id so CuriosPaper system recognizes it
        meta.getPersistentDataContainer().set(api.getItemIdKey(),
                org.bukkit.persistence.PersistentDataType.STRING, artifact.getId());

        item.setItemMeta(meta);

        // Tag as curio accessory if applicable
        if (artifact.isCurio()) {
            item = api.tagAccessoryItem(item, artifact.getSlotType());
        }

        return item;
    }

    /**
     * Create an ItemStack by item ID string.
     */
    public ItemStack createItemStack(String itemId) {
        ArtifactItem artifact = ArtifactItem.fromId(itemId);
        if (artifact == null)
            return null;
        return createItemStack(artifact);
    }

    /**
     * Set the item model via reflection for 1.21.3+ compatibility.
     */
    @SuppressWarnings("deprecation")
    private void setItemModelSafe(ItemMeta meta, String modelPath) {
        try {
            if (!setItemModelMethodChecked) {
                setItemModelMethodChecked = true;
                Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                setItemModelMethod = itemMetaClass.getMethod("setItemModel", NamespacedKey.class);
            }
            if (setItemModelMethod != null) {
                String[] parts = modelPath.split(":", 2);
                NamespacedKey key;
                if (parts.length == 2) {
                    key = new NamespacedKey(parts[0], parts[1]);
                } else {
                    key = new NamespacedKey("artifacts", parts[0]);
                }
                setItemModelMethod.invoke(meta, key);
            }
        } catch (Exception e) {
            // Silently fall back to CMD-only
        }
    }
}
