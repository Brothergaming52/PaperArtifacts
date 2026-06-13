package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

/**
 * Superstitious Hat: Adds +1 Looting level to mob drops.
 *
 * Works by listening to EntityDeathEvent and modifying the drops
 * to simulate an additional looting level. This is more reliable
 * than trying to modify enchantment levels at kill time.
 *
 * Each drop has a chance to get an extra item (same as vanilla looting).
 */
public class SuperstitiousHatListener implements Listener {

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final Random random = new Random();

    public SuperstitiousHatListener(Artifacts plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getArtifactConfig().isItemEnabled("superstitious_hat")) return;

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        if (!ArtifactUtil.hasEquipped(killer, ArtifactItem.SUPERSTITIOUS_HAT, api)) return;

        int bonusLevels = plugin.getArtifactConfig().getSuperstitiousHatLootingBonus();

        // Each looting level gives a chance to add 1 extra of each drop type
        // Vanilla looting: each level adds a random(0 to 1) item per drop
        List<ItemStack> drops = event.getDrops();
        for (int i = 0; i < drops.size(); i++) {
            ItemStack drop = drops.get(i);
            if (drop == null) continue;

            // For each bonus looting level, 50% chance to add 1 extra
            for (int level = 0; level < bonusLevels; level++) {
                if (random.nextFloat() < 0.5f) {
                    drop.setAmount(drop.getAmount() + 1);
                }
            }
        }
    }
}
