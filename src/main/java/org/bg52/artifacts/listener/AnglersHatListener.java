package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Angler's Hat: Grants +1 Luck of the Sea and +1 Lure while fishing.
 *
 * Works by temporarily boosting the fishing rod's enchantment levels
 * when a fish event occurs, then restoring them afterward.
 */
public class AnglersHatListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;

    // Enchantment constants (resolved at runtime for cross-version compat)
    private static Enchantment LUCK_ENCHANT = null;
    private static Enchantment LURE_ENCHANT = null;

    static {
        // Try modern names first, then legacy
        for (Enchantment ench : Enchantment.values()) {
            String name = ench.getName();
            if (name != null) {
                if (name.equalsIgnoreCase("LUCK_OF_THE_SEA") || name.equalsIgnoreCase("LUCK")) {
                    LUCK_ENCHANT = ench;
                }
                if (name.equalsIgnoreCase("LURE")) {
                    LURE_ENCHANT = ench;
                }
            }
        }
        // Fallback via key check
        if (LUCK_ENCHANT == null || LURE_ENCHANT == null) {
            for (Enchantment ench : Enchantment.values()) {
                String key = ench.getKey().getKey();
                if (key.equals("luck_of_the_sea") && LUCK_ENCHANT == null) {
                    LUCK_ENCHANT = ench;
                }
                if (key.equals("lure") && LURE_ENCHANT == null) {
                    LURE_ENCHANT = ench;
                }
            }
        }
    }

    public AnglersHatListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    private static class BoostData {
        final ItemStack item;
        final int addedLuck;
        final int addedLure;

        BoostData(ItemStack item, int addedLuck, int addedLure) {
            this.item = item;
            this.addedLuck = addedLuck;
            this.addedLure = addedLure;
        }

        void revert() {
            if (LUCK_ENCHANT == null || LURE_ENCHANT == null) return;
            if (item == null || !item.getType().name().contains("FISHING_ROD")) return;
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (addedLuck > 0) {
                    int current = meta.getEnchantLevel(LUCK_ENCHANT);
                    int result = current - addedLuck;
                    if (result <= 0) meta.removeEnchant(LUCK_ENCHANT);
                    else meta.addEnchant(LUCK_ENCHANT, result, true);
                }
                if (addedLure > 0) {
                    int current = meta.getEnchantLevel(LURE_ENCHANT);
                    int result = current - addedLure;
                    if (result <= 0) meta.removeEnchant(LURE_ENCHANT);
                    else meta.addEnchant(LURE_ENCHANT, result, true);
                }
                item.setItemMeta(meta);
            }
        }
    }

    private final java.util.Map<java.util.UUID, java.util.List<BoostData>> activeBoosts = new java.util.HashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!plugin.getArtifactConfig().isItemEnabled("anglers_hat")) return;
        
        org.bukkit.event.block.Action action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.ANGLERS_HAT, api)) return;

        ItemStack hand = event.getItem();
        if (hand == null || !hand.getType().name().contains("FISHING_ROD")) return;
        if (LUCK_ENCHANT == null || LURE_ENCHANT == null) return;

        int luckBonus = plugin.getArtifactConfig().getAnglersHatLuckBonus();
        int lureBonus = plugin.getArtifactConfig().getAnglersHatLureBonus();
        if (luckBonus <= 0 && lureBonus <= 0) return;

        ItemMeta meta = hand.getItemMeta();
        if (meta == null) return;

        int originalLuck = meta.getEnchantLevel(LUCK_ENCHANT);
        int originalLure = meta.getEnchantLevel(LURE_ENCHANT);

        meta.addEnchant(LUCK_ENCHANT, originalLuck + luckBonus, true);
        meta.addEnchant(LURE_ENCHANT, originalLure + lureBonus, true);
        hand.setItemMeta(meta);

        BoostData boost = new BoostData(hand, luckBonus, lureBonus);
        activeBoosts.computeIfAbsent(player.getUniqueId(), k -> new java.util.ArrayList<>()).add(boost);

        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            java.util.List<BoostData> boosts = activeBoosts.remove(player.getUniqueId());
            if (boosts != null) {
                for (BoostData b : boosts) {
                    b.revert();
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerFish(PlayerFishEvent event) {
        // Vanilla has already calculated fishing metrics via the boosted item.
        // Revert immediately so the client doesn't see a glowing rod or get inventory updates 
        // with the boosted enchants!
        Player player = event.getPlayer();
        java.util.List<BoostData> boosts = activeBoosts.remove(player.getUniqueId());
        if (boosts != null) {
            for (BoostData b : boosts) {
                b.revert();
            }
        }
    }
}
