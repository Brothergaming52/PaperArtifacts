package org.bg52.artifacts.manager;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    /**
     * Check if a player has an active cooldown for a specific artifact
     * 
     * @param player      The player to check
     * @param artifactKey The artifact identifier (e.g., "obsidian_skull")
     * @return true if cooldown is active, false otherwise
     */
    public boolean hasCooldown(Player player, String artifactKey) {
        if (!playerCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }

        Map<String, Long> cooldowns = playerCooldowns.get(player.getUniqueId());
        if (!cooldowns.containsKey(artifactKey)) {
            return false;
        }

        long cooldownEnd = cooldowns.get(artifactKey);
        return System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * Get remaining cooldown time in seconds
     * 
     * @param player      The player to check
     * @param artifactKey The artifact identifier
     * @return Remaining cooldown time in seconds, or 0 if no cooldown
     */
    public int getRemainingCooldown(Player player, String artifactKey) {
        if (!playerCooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }

        Map<String, Long> cooldowns = playerCooldowns.get(player.getUniqueId());
        if (!cooldowns.containsKey(artifactKey)) {
            return 0;
        }

        long cooldownEnd = cooldowns.get(artifactKey);
        long remaining = cooldownEnd - System.currentTimeMillis();

        if (remaining <= 0) {
            removeCooldown(player, artifactKey);
            return 0;
        }

        return (int) (remaining / 1000);
    }

    /**
     * Set a cooldown for a player and artifact
     * 
     * @param player      The player
     * @param artifactKey The artifact identifier
     * @param duration    Cooldown duration in seconds
     */
    public void setCooldown(Player player, String artifactKey, int duration) {
        long cooldownEnd = System.currentTimeMillis() + (duration * 1000L);

        playerCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        playerCooldowns.get(player.getUniqueId()).put(artifactKey, cooldownEnd);
    }

    /**
     * Remove a cooldown for a player and artifact
     * 
     * @param player      The player
     * @param artifactKey The artifact identifier
     */
    public void removeCooldown(Player player, String artifactKey) {
        if (playerCooldowns.containsKey(player.getUniqueId())) {
            playerCooldowns.get(player.getUniqueId()).remove(artifactKey);
        }
    }

    /**
     * Clear all cooldowns for a player
     * 
     * @param player The player
     */
    public void clearCooldowns(Player player) {
        playerCooldowns.remove(player.getUniqueId());
    }

    /**
     * Clear all cooldowns for all players
     */
    public void clearAllCooldowns() {
        playerCooldowns.clear();
    }
}
