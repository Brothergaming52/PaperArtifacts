package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.artifacts.util.FoodUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class DrinkingHatListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;

    public DrinkingHatListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (!event.hasItem())
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 1. Ensure the item is something you can consume
        if (item == null || (!item.getType().isEdible() && item.getType() != Material.POTION
                && item.getType() != Material.MILK_BUCKET)) {
            return;
        }

        // 2. Check if they are wearing the hat and get the speed
        int targetSpeed = -1;
        if (plugin.getArtifactConfig().isItemEnabled("novelty_drinking_hat")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.NOVELTY_DRINKING_HAT, api)) {
            targetSpeed = plugin.getArtifactConfig().getNoveltyDrinkingHatSpeed();
        } else if (plugin.getArtifactConfig().isItemEnabled("plastic_drinking_hat")
                && ArtifactUtil.hasEquipped(player, ArtifactItem.PLASTIC_DRINKING_HAT, api)) {
            targetSpeed = plugin.getArtifactConfig().getPlasticDrinkingHatSpeed();
        }

        if (targetSpeed < 0)
            return;

        // 3. Execute the consumption logic
        handleConsume(player, item, targetSpeed, event.getHand());
    }

    /**
     * Schedules a single future event for fast consumption.
     * Zero tick loops. Purely event driven.
     */
    private void handleConsume(Player player, ItemStack startedItem, int targetSpeed, EquipmentSlot hand) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Verify player is still online
                if (!player.isOnline())
                    return;

                // Verify they didn't let go of right click (fallback for 1.14/1.15)
                boolean stillHolding = false;
                try {
                    stillHolding = player.isHandRaised();
                } catch (NoSuchMethodError e) {
                    stillHolding = true; // Assume true on versions where isHandRaised doesn't exist
                }

                if (!stillHolding)
                    return;

                // Verify they didn't swap items while right-clicking
                ItemStack currentItem = hand == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand()
                        : player.getInventory().getItemInOffHand();

                if (currentItem == null || !currentItem.isSimilar(startedItem))
                    return;

                // --- 1. YOUR FOOD UTIL GOES HERE ---
                FoodUtil.applyFoodEffects(player, currentItem);

                // --- 2. Consume the item manually ---
                if (currentItem.getAmount() > 1) {
                    currentItem.setAmount(currentItem.getAmount() - 1);
                } else {
                    if (hand == EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(null);
                    } else {
                        player.getInventory().setItemInOffHand(null);
                    }
                }

                // --- 3. Play Audio ---
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);

                // --- 4. Cancel Vanilla Animation ---
                // Forcing an inventory update breaks the client's active eating state
                player.updateInventory();
            }
        }.runTaskLater(plugin, targetSpeed);
    }

    public void shutdown() {
        // No loops to cancel anymore!
    }
}