package org.bg52.artifacts;

import java.util.Arrays;

import org.bg52.artifacts.command.ArtifactsCommand;
import org.bg52.artifacts.item.ItemRegistry;
import org.bg52.artifacts.listener.*;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.manager.CampsiteConfig;
import org.bg52.artifacts.manager.CampsitePopulator;
import org.bg52.artifacts.manager.CooldownManager;
import org.bg52.artifacts.manager.MimicConfig;
import org.bg52.artifacts.manager.MimicManager;
import org.bg52.artifacts.manager.StoneTierManager;
import org.bg52.artifacts.util.VersionUtil;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public final class Artifacts extends JavaPlugin {

        private MimicManager mimicManager;
        private CooldownManager cooldownManager;
        private CuriosPaperAPI curiosPaperAPI;
        private ArtifactConfigManager artifactConfig;
        private MimicConfig mimicConfig;
        private StoneTierManager stoneTierManager;
        private ItemRegistry itemRegistry;

        // Listeners that need shutdown cleanup
        private UmbrellaListener umbrellaListener;
        private SnorkelListener snorkelListener;
        private CowboyHatListener cowboyHatListener;
        private DrinkingHatListener drinkingHatListener;
        private PassiveEffectListener passiveEffectListener;
        private SinkingListener sinkingListener;
        private DoubleJumpListener doubleJumpListener;
        private NegativeEffectListener negativeEffectListener;
        private UniversalAttractorListener universalAttractorListener;
        private HeliumFlamingoListener heliumFlamingoListener;
        private PassiveArtifactsListener passiveArtifactsListener;
        private AquaDashersListener aquaDashersListener;
        private RunningShoesListener runningShoesListener;
        private SnowshoesListener snowshoesListener;
        private RootedBootsListener rootedBootsListener;
        private StriderShoesListener striderShoesListener;

        // campsite
        private CampsiteConfig campsiteConfig;
        private CampsitePopulator populator;

        @Override
        public void onEnable() {
                getLogger().info("Artifacts enabling on MC " + VersionUtil.getVersionString() + "...");

                // Initialize config manager (loads config.yml)
                artifactConfig = new ArtifactConfigManager(this);

                // Load food data for the Drinking Hat
                org.bg52.artifacts.util.FoodUtil.loadFoodInfo(this);

                // Initialize mimic config
                mimicConfig = new MimicConfig(this);

                // Initialize mimic manager
                mimicManager = new MimicManager(this);

                // Initialize campsite config
                campsiteConfig = new CampsiteConfig(this);
                populator = new CampsitePopulator(this);

                // Initialize stone tier manager
                stoneTierManager = new StoneTierManager(this);

                // Initialize cooldown manager
                cooldownManager = new CooldownManager();

                CuriosPaper curiosPaper = CuriosPaper.getInstance();
                this.curiosPaperAPI = curiosPaper.getCuriosPaperAPI();

                if (curiosPaperAPI == null) {
                        getLogger().severe("CuriosPaper API not available! Disabling.");
                        getServer().getPluginManager().disablePlugin(this);
                        return;
                }

                // Register resource pack assets from this JAR
                curiosPaperAPI.registerResourcePackAssetsFromJar(this);

                // Register a new Slot
                curiosPaperAPI.registerSlot("feet", ChatColor.GOLD + "Feet", Material.IRON_BOOTS, "artifacts:feet",
                                201100, 1,
                                Arrays.asList("&7Feet accessary items Slot"), 15);

                // Initialize item registry and register all artifact items
                itemRegistry = new ItemRegistry(this, curiosPaperAPI);
                itemRegistry.registerAll();

                // ── Register Commands ──

                ArtifactsCommand artifactsCommand = new ArtifactsCommand(this);
                getCommand("artifacts").setExecutor(artifactsCommand);
                getCommand("artifacts").setTabCompleter(artifactsCommand);

                // ── Register Mimic Listeners ──

                getServer().getPluginManager().registerEvents(
                                new MimicChestListener(this, mimicManager), this);
                getServer().getPluginManager().registerEvents(
                                new MimicDamageListener(this, mimicManager), this);
                getServer().getPluginManager().registerEvents(
                                new MimicCleanupListener(this, mimicManager), this);
                getServer().getPluginManager().registerEvents(
                                new SlimeballListener(), this);

                // ── Register Artifact Item Listeners ──

                // Umbrella — slow falling while held
                umbrellaListener = new UmbrellaListener(this);
                getServer().getPluginManager().registerEvents(umbrellaListener, this);

                // Everlasting Beef / Eternal Steak — infinite food
                getServer().getPluginManager().registerEvents(
                                new EverlastingFoodListener(this, cooldownManager), this);

                // Snorkel — underwater oxygen
                snorkelListener = new SnorkelListener(this, curiosPaperAPI);
                getServer().getPluginManager().registerEvents(snorkelListener, this);

                // Villager Hat — trade price reduction
                getServer().getPluginManager().registerEvents(
                                new VillagerHatListener(this, curiosPaperAPI), this);

                // Superstitious Hat — +1 looting
                getServer().getPluginManager().registerEvents(
                                new SuperstitiousHatListener(this, curiosPaperAPI), this);

                // Cowboy Hat — mount speed
                cowboyHatListener = new CowboyHatListener(this, curiosPaperAPI);
                getServer().getPluginManager().registerEvents(cowboyHatListener, this);

                // Lucky Scarf — +1 fortune
                getServer().getPluginManager().registerEvents(
                                new LuckyScarfListener(this, curiosPaperAPI), this);

                // Damage artifacts — Cross Necklace, Panic Necklace, Shock Pendant, Flame
                // Pendant
                getServer().getPluginManager().registerEvents(
                                new DamageArtifactListener(this, curiosPaperAPI, cooldownManager), this);

                // Drinking Hats — faster eating
                drinkingHatListener = new DrinkingHatListener(this, curiosPaperAPI);
                getServer().getPluginManager().registerEvents(drinkingHatListener, this);

                // Passive Effects — Night Vision Goggles, Scarf of Invisibility
                passiveEffectListener = new PassiveEffectListener(this, curiosPaperAPI);
                getServer().getPluginManager().registerEvents(passiveEffectListener, this);

                // Negative Effects —
                negativeEffectListener = new NegativeEffectListener(this, curiosPaperAPI);
                getServer().getPluginManager().registerEvents(negativeEffectListener, this);

                // Charm of Sinking — sink in water
                sinkingListener = new SinkingListener(this, curiosPaperAPI, artifactConfig);
                getServer().getPluginManager().registerEvents(sinkingListener, this);

                // Cloud in a Bottle — double jump
                doubleJumpListener = new DoubleJumpListener(this, curiosPaperAPI, artifactConfig);
                getServer().getPluginManager().registerEvents(doubleJumpListener, this);

                // Angler's Hat — fishing bonuses
                getServer().getPluginManager().registerEvents(
                                new AnglersHatListener(this, curiosPaperAPI), this);

                // Universal Attractor — attracts nearby items
                universalAttractorListener = new UniversalAttractorListener(this, curiosPaperAPI, cooldownManager);
                getServer().getPluginManager().registerEvents(universalAttractorListener, this);

                // Passive Artifacts — Crystal Heart health boost
                passiveArtifactsListener = new PassiveArtifactsListener(this, curiosPaperAPI);
                getServer().getPluginManager().registerEvents(passiveArtifactsListener, this);

                // Helium Flamingo — swim in air
                heliumFlamingoListener = new HeliumFlamingoListener(this, curiosPaperAPI, artifactConfig);
                getServer().getPluginManager().registerEvents(heliumFlamingoListener, this);

                // Aqua Dashers — walk on fluids while sprinting
                aquaDashersListener = new AquaDashersListener(this, curiosPaperAPI, artifactConfig);
                getServer().getPluginManager().registerEvents(aquaDashersListener, this);

                // Running Shoes — speed and step height while sprinting
                runningShoesListener = new RunningShoesListener(this, curiosPaperAPI, artifactConfig);
                getServer().getPluginManager().registerEvents(runningShoesListener, this);

                // Snowshoes — walk on powder snow and less slippery ice
                snowshoesListener = new SnowshoesListener(this, curiosPaperAPI, artifactConfig);
                getServer().getPluginManager().registerEvents(snowshoesListener, this);

                // Rooted Boots — hunger replenishment on grass
                rootedBootsListener = new RootedBootsListener(this, curiosPaperAPI);
                getServer().getPluginManager().registerEvents(rootedBootsListener, this);

                // Strider Shoes — walk on lava while shifting
                striderShoesListener = new StriderShoesListener(this, curiosPaperAPI, artifactConfig);
                getServer().getPluginManager().registerEvents(striderShoesListener, this);

                // Register the block populator for all worlds
                getServer().getWorlds().forEach(world -> world.getPopulators().add(populator));

                // Also hook new worlds when they are loaded
                getServer().getPluginManager().registerEvents(new WorldLoadListener(populator), this);

                // bStats
                int pluginId = 30728;
                new Metrics(this, pluginId);

                getLogger().info("Artifacts enabled. " + ArtifactItem().length + " items registered. "
                                + "Item model support: " + VersionUtil.supportsItemModel());
        }

        @Override
        public void onDisable() {
                // Shutdown listeners with cleanup tasks
                if (umbrellaListener != null)
                        umbrellaListener.shutdown();
                if (snorkelListener != null)
                        snorkelListener.shutdown();
                if (cowboyHatListener != null)
                        cowboyHatListener.shutdown();
                if (drinkingHatListener != null)
                        drinkingHatListener.shutdown();
                if (passiveEffectListener != null)
                        passiveEffectListener.shutdown();
                if (sinkingListener != null)
                        sinkingListener.shutdown();
                if (doubleJumpListener != null)
                        doubleJumpListener.shutdown();
                if (heliumFlamingoListener != null)
                        heliumFlamingoListener.shutdown();
                if (passiveArtifactsListener != null)
                        passiveArtifactsListener.shutdown();
                if (aquaDashersListener != null)
                        aquaDashersListener.shutdown();
                if (runningShoesListener != null)
                        runningShoesListener.shutdown();
                if (snowshoesListener != null)
                        snowshoesListener.shutdown();
                if (striderShoesListener != null)
                        striderShoesListener.shutdown();

                if (populator != null) {
                        getServer().getWorlds().forEach(world -> world.getPopulators().remove(populator));
                }

                if (mimicManager != null) {
                        int count = mimicManager.getActiveMimicCount();
                        mimicManager.shutdown();
                        getLogger().info("Artifacts disabled. Cleaned up " + count + " active mimics.");
                }
        }

        private org.bg52.artifacts.item.ArtifactItem[] ArtifactItem() {
                return org.bg52.artifacts.item.ArtifactItem.values();
        }

        public MimicManager getMimicManager() {
                return mimicManager;
        }

        public CuriosPaperAPI getCuriosPaperAPI() {
                return curiosPaperAPI;
        }

        public ArtifactConfigManager getArtifactConfig() {
                return artifactConfig;
        }

        public StoneTierManager getStoneTierManager() {
                return stoneTierManager;
        }

        public ItemRegistry getItemRegistry() {
                return itemRegistry;
        }

        public MimicConfig getMimicConfig() {
                return mimicConfig;
        }

        public CampsiteConfig getCampsiteConfig() {
                return campsiteConfig;
        }
}
