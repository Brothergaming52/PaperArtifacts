package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.CooldownManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Everlasting Beef / Eternal Steak: Items can be eaten infinitely.
 * The item is NOT consumed when eaten.
 * Has a configurable cooldown (default 15 seconds).
 *
 * Prevents the normal consumption from reducing the stack and
 * applies a custom cooldown to avoid blocking regular food.
 */
public class EverlastingFoodListener implements Listener {

    private final Artifacts plugin;
    private final CooldownManager cooldownManager;

    public EverlastingFoodListener(Artifacts plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (!event.hasItem())
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        ArtifactItem artifact = null;

        if (ArtifactUtil.isArtifact(item, ArtifactItem.EVERLASTING_BEEF)) {
            if (!plugin.getArtifactConfig().isItemEnabled("everlasting_beef"))
                return;
            artifact = ArtifactItem.EVERLASTING_BEEF;
        } else if (ArtifactUtil.isArtifact(item, ArtifactItem.ETERNAL_STEAK)) {
            if (!plugin.getArtifactConfig().isItemEnabled("eternal_steak"))
                return;
            artifact = ArtifactItem.ETERNAL_STEAK;
        }

        if (artifact == null)
            return;

        if (cooldownManager.hasCooldown(player, artifact.getId())) {
            event.setCancelled(true);
            long remaining = cooldownManager.getRemainingCooldown(player, artifact.getId());
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(
                            "\u00a7cThis item is on cooldown for " + remaining + "s"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack consumed = event.getItem();

        ArtifactItem artifact = null;

        if (ArtifactUtil.isArtifact(consumed, ArtifactItem.EVERLASTING_BEEF)) {
            if (!plugin.getArtifactConfig().isItemEnabled("everlasting_beef"))
                return;
            artifact = ArtifactItem.EVERLASTING_BEEF;
            cooldownManager.setCooldown(player, artifact.getId(),
                    plugin.getArtifactConfig().getEverlastingBeefCooldown());
        } else if (ArtifactUtil.isArtifact(consumed, ArtifactItem.ETERNAL_STEAK)) {
            if (!plugin.getArtifactConfig().isItemEnabled("eternal_steak"))
                return;
            artifact = ArtifactItem.ETERNAL_STEAK;
            cooldownManager.setCooldown(player, artifact.getId(), plugin.getArtifactConfig().getEternalSteakCooldown());
        }

        if (artifact == null)
            return;

        // Prevent item consumption — replace with the same item after the event
        final ItemStack originalItem = consumed.clone();
        final Player finalPlayer = player;
        final boolean mainHand = ArtifactUtil.isArtifact(
                player.getInventory().getItemInMainHand(), artifact);

        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (mainHand) {
                    finalPlayer.getInventory().setItemInMainHand(originalItem);
                } else {
                    finalPlayer.getInventory().setItemInOffHand(originalItem);
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack result = event.getResult();
        if (ArtifactUtil.isArtifact(result, ArtifactItem.ETERNAL_STEAK)) {
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                try {
                    java.lang.reflect.Method setMaxStackSize = meta.getClass().getMethod("setMaxStackSize", int.class);
                    setMaxStackSize.invoke(meta, 1);
                } catch (Exception e) {
                    meta.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey(plugin, "unstackable_id"),
                            org.bukkit.persistence.PersistentDataType.STRING,
                            UUID.randomUUID().toString());
                }
                result.setItemMeta(meta);
                event.setResult(result);
            }
        }
    }
}
