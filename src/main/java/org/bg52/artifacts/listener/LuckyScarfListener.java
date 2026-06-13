package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Random;

/**
 * Lucky Scarf: Adds +1 Fortune level to mined blocks.
 *
 * Simulates additional fortune by giving extra drops.
 * Fortune formula: for each bonus level, there's a chance to multiply drops.
 * This mimics vanilla fortune behavior (chance for +1 extra drop per level).
 */
public class LuckyScarfListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final Random random = new Random();

    public LuckyScarfListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getArtifactConfig().isItemEnabled("lucky_scarf")) return;

        Player player = event.getPlayer();

        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.LUCKY_SCARF, api)) return;

        Block block = event.getBlock();
        int bonusLevels = plugin.getArtifactConfig().getLuckyScarfFortuneBonus();

        // Get the block drops using the player's current tool
        ItemStack tool = player.getInventory().getItemInMainHand();
        Collection<ItemStack> drops = block.getDrops(tool);

        if (drops.isEmpty()) return;

        // For ore-type blocks, fortune adds extra items
        // Simplified: for each bonus level, 33% chance to add 1 extra of each drop
        for (ItemStack drop : drops) {
            for (int i = 0; i < bonusLevels; i++) {
                if (random.nextFloat() < 0.33f) {
                    // Drop extra item naturally at the block location
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(drop.getType(), 1));
                }
            }
        }
    }
}
