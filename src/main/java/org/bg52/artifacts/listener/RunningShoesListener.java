package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.artifacts.util.VersionUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.ArrayList;

public class RunningShoesListener implements Listener {

    private static final UUID SPEED_ID = UUID.fromString("0a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d");
    private static final UUID STEP_ID = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d");

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager config;

    public RunningShoesListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager config) {
        this.plugin = plugin;
        this.api = api;
        this.config = config;
    }

    public void shutdown() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removeModifiers(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Initial check in case they logged out sprinting
        updateShoesEffect(event.getPlayer(), event.getPlayer().isSprinting());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeModifiers(event.getPlayer());
    }

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        updateShoesEffect(event.getPlayer(), event.isSprinting());
    }

    @EventHandler
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();
        boolean isRunningShoes = ArtifactUtil.isArtifact(event.getNewItem(), ArtifactItem.RUNNING_SHOES)
                || ArtifactUtil.isArtifact(event.getPreviousItem(), ArtifactItem.RUNNING_SHOES);

        if (isRunningShoes) {
            // Run on next tick to ensure hasEquipped is accurate
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    updateShoesEffect(player, player.isSprinting());
                }
            });
        }
    }

    private void updateShoesEffect(Player player, boolean isSprinting) {
        if (!config.isItemEnabled("running_shoes") ||
                !isSprinting ||
                !ArtifactUtil.hasEquipped(player, ArtifactItem.RUNNING_SHOES, api)) {
            removeModifiers(player);
            return;
        }

        applyModifiers(player);
    }

    private void applyModifiers(Player player) {
        // 1. Movement Speed
        Attribute speedAttr = VersionUtil.getMovementSpeedAttribute();
        if (speedAttr != null) {
            AttributeInstance instance = player.getAttribute(speedAttr);
            if (instance != null) {
                removeModifier(instance, SPEED_ID);
                double amount = config.getRunningShoesSpeedAmplifier();
                AttributeModifier modifier = new AttributeModifier(
                        SPEED_ID,
                        "Running Shoes Speed",
                        amount,
                        AttributeModifier.Operation.ADD_SCALAR);
                instance.addModifier(modifier);
            }
        }

        // 2. Modern Step Height (1.20.5+)
        Attribute stepAttr = VersionUtil.getStepHeightAttribute();
        if (stepAttr != null) {
            AttributeInstance instance = player.getAttribute(stepAttr);
            if (instance != null) {
                removeModifier(instance, STEP_ID);
                double amount = config.getRunningShoesStepHeightAmplifier();
                AttributeModifier modifier = new AttributeModifier(
                        STEP_ID,
                        "Running Shoes Step Height",
                        amount,
                        AttributeModifier.Operation.ADD_NUMBER);
                instance.addModifier(modifier);
            }
        }
    }

    private void removeModifiers(Player player) {
        Attribute speedAttr = VersionUtil.getMovementSpeedAttribute();
        if (speedAttr != null) {
            AttributeInstance instance = player.getAttribute(speedAttr);
            if (instance != null) removeModifier(instance, SPEED_ID);
        }

        Attribute stepAttr = VersionUtil.getStepHeightAttribute();
        if (stepAttr != null) {
            AttributeInstance instance = player.getAttribute(stepAttr);
            if (instance != null) removeModifier(instance, STEP_ID);
        }
    }

    private void removeModifier(AttributeInstance instance, UUID id) {
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            if (modifier.getUniqueId().equals(id)) {
                instance.removeModifier(modifier);
            }
        }
    }

    /**
     * Legacy Step Height Support (< 1.20.5)
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (VersionUtil.getStepHeightAttribute() != null) return; // Modern version handles this via attributes

        Player player = event.getPlayer();
        if (!player.isSprinting()) return;
        if (!config.isItemEnabled("running_shoes")) return;
        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.RUNNING_SHOES, api)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Ensure horizontal movement
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) return;
        
        // Horizontal distance
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        
        // Check if there's a block in front that is 1 block high
        Location front = to.clone().add(dx * 2, 0, dz * 2);
        Material type = front.getBlock().getType();
        
        // If the block we are moving towards is solid and the space above is clear
        if (type.isSolid() && !front.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
            // Apply a small boost/teleport to simulate stepping
            // We only do this if they are on the ground
            if (((org.bukkit.entity.Entity) player).isOnGround()) {
                Vector vel = player.getVelocity();
                // 0.42 is enough to reach 1 block height
                player.setVelocity(vel.setY(0.42));
            }
        }
    }
}
