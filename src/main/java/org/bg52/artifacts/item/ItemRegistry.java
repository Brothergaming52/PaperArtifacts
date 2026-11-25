package org.bg52.artifacts.item;

import org.bg52.artifacts.Artifacts;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.synthetix.api.SynthetixAPI;
import org.bg52.synthetix.distribution.LootTableRule;
import org.bg52.synthetix.distribution.MobDropRule;
import org.bg52.synthetix.item.ItemDefinition;
import org.bg52.synthetix.item.food.FoodComponentSpec;
import org.bg52.synthetix.recipe.IngredientChoice;
import org.bg52.synthetix.recipe.RecipeType;
import org.bg52.synthetix.recipe.SynthetixRecipe;
import org.bg52.synthetix.util.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Super-modular registry coordinating all Artifact items, recipes, mob drops, and loot tables
 * directly with the Synthetix core engine.
 */
public class ItemRegistry {

    private final Artifacts plugin;
    private final CuriosPaperAPI curiosApi;
    private final Logger logger;

    public ItemRegistry(Artifacts plugin, CuriosPaperAPI curiosApi) {
        this.plugin = plugin;
        this.curiosApi = curiosApi;
        this.logger = plugin.getLogger();
    }

    /**
     * Register all artifact items, cooking recipes, mob drops, and loot tables with Synthetix.
     */
    public void registerAll() {
        if (!SynthetixAPI.getOptional().isPresent()) {
            logger.severe("[ItemRegistry] Synthetix API not available! Skipping registration.");
            return;
        }

        // Ensure artifacts.yml is loaded before registering
        if (plugin.getArtifactItemsConfig() != null) {
            plugin.getArtifactItemsConfig().load();
        }

        int registeredItems = 0;
        int skippedItems = 0;

        for (ArtifactItem artifact : ArtifactItem.values()) {
            // Check if disabled in config
            if (!plugin.getArtifactConfig().isItemEnabled(artifact.getId())) {
                skippedItems++;
                continue;
            }

            ItemDefinition def = new ItemDefinition(artifact.getId());
            def.setOwningPlugin("PaperArtifacts");
            def.setDisplayName(artifact.getDisplayName());
            def.setMaterial(artifact.getBaseMaterial());
            def.setCustomModelData(artifact.getCustomModelData());
            def.setItemModel("artifacts:" + artifact.getItemModelKey());
            def.setLore(artifact.getLore());

            // Curios tags
            if (artifact.isCurio()) {
                def.setTag("curiospaper:slot_type", artifact.getSlotType());
                def.setTag("curios_slot", artifact.getSlotType());
                def.setTag("curios.slot", artifact.getSlotType());
                def.setTag("slot", artifact.getSlotType());
                def.setTag("curiospaper:render_model", artifact.has3DModel() ? "true" : "false");

                if (artifact.has3DModel()) {
                    def.setTag("curiospaper:model_item", artifact.getBaseMaterial().name());
                    def.setTag("curiospaper:model_cmd", String.valueOf(artifact.get3DCustomModelData()));
                    def.setTag("curiospaper:model_item_model", "artifacts:" + artifact.get3DItemModelKey());

                    if (artifact.getPitchUpLimit() != null) {
                        def.setTag("curiospaper:pitch_up_limit", String.valueOf(artifact.getPitchUpLimit()));
                    }
                    if (artifact.getPitchDownLimit() != null) {
                        def.setTag("curiospaper:pitch_down_limit", String.valueOf(artifact.getPitchDownLimit()));
                    }
                }
            }

            // Item-specific components
            if (artifact == ArtifactItem.UMBRELLA) {
                def.setUnbreakable(true);
            } else if (artifact == ArtifactItem.EVERLASTING_BEEF) {
                FoodComponentSpec food = new FoodComponentSpec();
                food.setNutrition(3);
                food.setSaturation(1.8f);
                food.setCanAlwaysEat(false);
                food.setEatSeconds(1.6f);
                def.setFood(food);
                def.setMaxStackSize(1);
            } else if (artifact == ArtifactItem.ETERNAL_STEAK) {
                FoodComponentSpec food = new FoodComponentSpec();
                food.setNutrition(8);
                food.setSaturation(12.8f);
                food.setCanAlwaysEat(false);
                food.setEatSeconds(1.6f);
                def.setFood(food);
                def.setMaxStackSize(1);
            }

            // Register into Synthetix ItemRegistry
            SynthetixAPI.get().getItemRegistry().register(def);
            registeredItems++;
        }

        logger.info("[ItemRegistry] Successfully registered " + registeredItems + " items into Synthetix (" + skippedItems + " skipped/disabled).");

        // Synchronize registered slots into CuriosPaper so slot buttons show up immediately
        if (curiosApi != null && curiosApi.getSlotRegistry() != null) {
            for (ArtifactItem artifact : ArtifactItem.values()) {
                if (artifact.isCurio() && artifact.getSlotType() != null) {
                    curiosApi.getSlotRegistry().markSlotActive(artifact.getSlotType());
                }
            }
            curiosApi.getSlotRegistry().refreshActiveSlots();
        }

        // ── Register Recipes ──
        registerRecipes();

        // ── Register Mob Drops & Loot Tables ──
        registerDistribution();
    }

    private void registerRecipes() {
        if (!plugin.getArtifactConfig().isItemEnabled("eternal_steak") ||
            !plugin.getArtifactConfig().isItemEnabled("everlasting_beef")) {
            return;
        }

        try {
            // Furnace
            SynthetixRecipe furnaceRecipe = new SynthetixRecipe("eternal_steak_furnace", RecipeType.FURNACE, "synth:eternal_steak", 1);
            furnaceRecipe.setCookingInput(new IngredientChoice("synth:everlasting_beef"));
            furnaceRecipe.setCookingTime(plugin.getArtifactConfig().getEternalSteakFurnaceTime() * 20);
            furnaceRecipe.setExperience(0.35f);
            SynthetixAPI.get().getRecipeManager().registerRecipe(furnaceRecipe, false);

            // Smoker
            SynthetixRecipe smokerRecipe = new SynthetixRecipe("eternal_steak_smoker", RecipeType.SMOKER, "synth:eternal_steak", 1);
            smokerRecipe.setCookingInput(new IngredientChoice("synth:everlasting_beef"));
            smokerRecipe.setCookingTime(plugin.getArtifactConfig().getEternalSteakSmokerTime() * 20);
            smokerRecipe.setExperience(0.35f);
            SynthetixAPI.get().getRecipeManager().registerRecipe(smokerRecipe, false);

            // Campfire
            SynthetixRecipe campfireRecipe = new SynthetixRecipe("eternal_steak_campfire", RecipeType.CAMPFIRE, "synth:eternal_steak", 1);
            campfireRecipe.setCookingInput(new IngredientChoice("synth:everlasting_beef"));
            campfireRecipe.setCookingTime(plugin.getArtifactConfig().getEternalSteakCampfireTime() * 20);
            campfireRecipe.setExperience(0.35f);
            SynthetixAPI.get().getRecipeManager().registerRecipe(campfireRecipe, false);

            logger.info("[ItemRegistry] Registered Eternal Steak cooking recipes with Synthetix RecipeManager!");
        } catch (Throwable t) {
            logger.warning("[ItemRegistry] Failed to register recipes with Synthetix: " + t.getMessage());
        }
    }

    private void registerDistribution() {
        try {
            // Register Mob Drops
            List<MobDropRule> mobDrops = ArtifactMobDrops.getMobDrops(plugin);
            for (MobDropRule rule : mobDrops) {
                SynthetixAPI.get().getDistributionManager().getMobDropManager().registerRule(rule, false);
            }
            logger.info("[ItemRegistry] Registered " + mobDrops.size() + " mob drop rules with Synthetix!");

            // Register Loot Table Injections
            List<LootTableRule> lootTables = ArtifactLootTables.getLootTables(plugin);
            for (LootTableRule rule : lootTables) {
                SynthetixAPI.get().getDistributionManager().getLootTableInjector().registerRule(rule, false);
            }
            logger.info("[ItemRegistry] Registered " + lootTables.size() + " loot table rules with Synthetix!");
        } catch (Throwable t) {
            logger.warning("[ItemRegistry] Failed to register distribution rules with Synthetix: " + t.getMessage());
        }
    }

    /**
     * Create an ItemStack for the given artifact item with all metadata and tags.
     */
    public ItemStack createItemStack(ArtifactItem artifact) {
        if (artifact == null) return null;
        return createItemStack(artifact.getId());
    }

    /**
     * Create an ItemStack by string ID.
     */
    public ItemStack createItemStack(String itemId) {
        if (itemId == null) return null;

        if (SynthetixAPI.getOptional().isPresent()) {
            ItemStack stack = SynthetixAPI.get().createItemStack(itemId);
            if (stack != null) {
                ArtifactItem artifact = ArtifactItem.fromId(itemId);
                if (artifact != null && artifact.isCurio() && curiosApi != null) {
                    stack = curiosApi.tagAccessoryItem(stack, artifact.getSlotType());
                    if (artifact.has3DModel()) {
                        stack = curiosApi.set3DModel(
                                stack,
                                artifact.getBaseMaterial(),
                                artifact.get3DCustomModelData(),
                                "artifacts:" + artifact.get3DItemModelKey(),
                                artifact.getPitchUpLimit(),
                                artifact.getPitchDownLimit()
                        );
                    }
                }
                return stack;
            }
        }

        // Fallback manual builder if Synthetix is unavailable
        ArtifactItem artifact = ArtifactItem.fromId(itemId);
        if (artifact == null) return null;

        ItemStack item = new ItemStack(artifact.getBaseMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (artifact.getDisplayName() != null) {
            meta.displayName(TextUtil.parse(artifact.getDisplayName()));
        }

        if (artifact.getLore() != null) {
            meta.lore(TextUtil.parseList(artifact.getLore()));
        }
        meta.setCustomModelData(artifact.getCustomModelData());

        try {
            java.lang.reflect.Method setItemModelMethod = meta.getClass().getMethod("setItemModel", org.bukkit.NamespacedKey.class);
            setItemModelMethod.invoke(meta, new org.bukkit.NamespacedKey("artifacts", artifact.getItemModelKey()));
        } catch (Throwable ignored) {
        }

        if (artifact == ArtifactItem.UMBRELLA) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        if (curiosApi != null) {
            meta.getPersistentDataContainer().set(curiosApi.getItemIdKey(),
                    org.bukkit.persistence.PersistentDataType.STRING, artifact.getId());
        }

        item.setItemMeta(meta);

        if (artifact.isCurio() && curiosApi != null) {
            item = curiosApi.tagAccessoryItem(item, artifact.getSlotType());
            if (artifact.has3DModel()) {
                item = curiosApi.set3DModel(
                        item,
                        artifact.getBaseMaterial(),
                        artifact.get3DCustomModelData(),
                        "artifacts:" + artifact.get3DItemModelKey(),
                        artifact.getPitchUpLimit(),
                        artifact.getPitchDownLimit()
                );
            }
        }

        return item;
    }

    public CuriosPaperAPI getCuriosPaperApi() {
        return curiosApi;
    }
}
