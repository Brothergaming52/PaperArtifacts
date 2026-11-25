package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Lucky Scarf: Adds +1 Fortune level to mined blocks.
 *
 * Evaluates vanilla loot drops with an extra Fortune level bonus
 * and yields the difference, preserving Silk Touch exclusions.
 */
public class LuckyScarfListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;

    public LuckyScarfListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        event.getBlock().setMetadata("placed", new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakCleanup(BlockBreakEvent event) {
        event.getBlock().removeMetadata("placed", plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getArtifactConfig().isItemEnabled("lucky_scarf")) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.LUCKY_SCARF, api)) return;

        Block block = event.getBlock();

        // Prevent exploit on player-placed blocks
        if (block.hasMetadata("placed")) return;

        ItemStack tool = player.getInventory().getItemInMainHand();

        // If the tool has Silk Touch, Fortune NEVER applies
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        int bonusLevels = plugin.getArtifactConfig().getLuckyScarfFortuneBonus();
        if (bonusLevels <= 0) return;

        ItemStack baseTool = (tool != null && !tool.getType().isAir()) ? tool.clone() : new ItemStack(Material.AIR);
        int currentFortune = baseTool.getEnchantmentLevel(Enchantment.FORTUNE);

        ItemStack boostedTool = baseTool.clone();
        if (boostedTool.getType().isAir()) {
            boostedTool = new ItemStack(Material.WOODEN_PICKAXE);
        }
        boostedTool.addUnsafeEnchantment(Enchantment.FORTUNE, currentFortune + bonusLevels);

        Collection<ItemStack> baseDrops = block.getDrops(baseTool, player);
        Collection<ItemStack> boostedDrops = block.getDrops(boostedTool, player);

        if (boostedDrops.isEmpty() || baseDrops.isEmpty()) return;

        Map<Material, Integer> baseCounts = new HashMap<>();
        for (ItemStack is : baseDrops) {
            baseCounts.put(is.getType(), baseCounts.getOrDefault(is.getType(), 0) + is.getAmount());
        }

        Map<Material, Integer> boostedCounts = new HashMap<>();
        for (ItemStack is : boostedDrops) {
            boostedCounts.put(is.getType(), boostedCounts.getOrDefault(is.getType(), 0) + is.getAmount());
        }

        boolean droppedAny = false;
        for (Map.Entry<Material, Integer> entry : boostedCounts.entrySet()) {
            Material mat = entry.getKey();
            int boosted = entry.getValue();
            int base = baseCounts.getOrDefault(mat, 0);

            // Never duplicate the ore block itself
            if (mat == block.getType()) continue;

            int extra = boosted - base;
            if (extra > 0) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(mat, extra));
                droppedAny = true;
            }
        }

        if (droppedAny) {
            if (plugin.getArtifactConfig().isParticlesEnabled()) {
                block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 6, 0.3, 0.3, 0.3, 0.0);
            }
            if (plugin.getArtifactConfig().isSoundsEnabled()) {
                player.playSound(block.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.8f);
            }
        }
    }
}
