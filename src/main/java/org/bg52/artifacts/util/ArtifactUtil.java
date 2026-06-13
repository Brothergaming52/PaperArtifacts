package org.bg52.artifacts.util;

import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Utility methods for checking whether a player has a specific
 * artifact item equipped in a curio slot or held in hand.
 *
 * Uses CustomModelData matching as the primary identification method,
 * which works across all supported versions (1.14+).
 */
public class ArtifactUtil {

    /**
     * Check if a player has the given artifact equipped in any curio slot.
     * Searches all slot types that match the artifact's declared slot.
     */
    public static boolean hasEquipped(Player player, ArtifactItem artifact, CuriosPaperAPI api) {
        if (player == null || artifact == null || api == null)
            return false;
        if (artifact.getSlotType() == null)
            return false;

        List<ItemStack> items = api.getEquippedItems(player, artifact.getSlotType());
        for (ItemStack item : items) {
            if (isArtifact(item, artifact)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a player has any of the given artifacts equipped.
     * Returns the first matching artifact, or null if none found.
     */
    public static ArtifactItem getEquippedArtifact(Player player, CuriosPaperAPI api, ArtifactItem... candidates) {
        if (player == null || api == null)
            return null;

        for (ArtifactItem artifact : candidates) {
            if (artifact.getSlotType() == null)
                continue;
            if (hasEquipped(player, artifact, api)) {
                return artifact;
            }
        }
        return null;
    }

    public static void removeArtifact(Player player, ArtifactItem artifact, CuriosPaperAPI api) {
        if (player == null || artifact == null || api == null)
            return;
        if (artifact.getSlotType() == null)
            return;

        List<ItemStack> items = api.getEquippedItems(player, artifact.getSlotType());
        for (ItemStack item : items) {
            if (isArtifact(item, artifact)) {
                api.removeEquippedItem(player, artifact.getSlotType(), item);
            }
        }
    }

    /**
     * Check if an ItemStack matches a specific artifact by CustomModelData.
     */
    public static boolean isArtifact(ItemStack item, ArtifactItem artifact) {
        if (item == null || item.getType() == Material.AIR)
            return false;
        if (item.getType() != artifact.getBaseMaterial())
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        if (!meta.hasCustomModelData())
            return false;

        return meta.getCustomModelData() == artifact.getCustomModelData();
    }

    /**
     * Check if a player is holding the given artifact in either hand.
     */
    public static boolean isHolding(Player player, ArtifactItem artifact) {
        return isArtifact(player.getInventory().getItemInMainHand(), artifact)
                || isArtifact(player.getInventory().getItemInOffHand(), artifact);
    }

    /**
     * Check if a player is holding the given artifact in their main hand.
     */
    public static boolean isHoldingMainHand(Player player, ArtifactItem artifact) {
        return isArtifact(player.getInventory().getItemInMainHand(), artifact);
    }
}
