package org.bg52.artifacts.listener;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.artifacts.manager.ArtifactConfigManager;
import org.bg52.artifacts.util.ArtifactUtil;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class HeliumFlamingoListener implements Listener {

    private static final char FLAMINGO_FULL = '\uE521';
    private static final char FLAMINGO_BURST = '\uE522';
    private static final char FLAMINGO_EMPTY = '\uE523';
    private static final int BAR_SIZE = 10;

    private final Artifacts plugin;
    private final CuriosPaperAPI api;
    private final ArtifactConfigManager configManager;

    private final Map<UUID, FlamingoState> playerStates = new HashMap<>();

    public HeliumFlamingoListener(Artifacts plugin, CuriosPaperAPI api, ArtifactConfigManager configManager) {
        this.plugin = plugin;
        this.api = api;
        this.configManager = configManager;
    }

    private enum Phase {
        READY,
        FLYING,
        COOLDOWN
    }

    private class FlamingoState {
        Phase phase = Phase.READY;
        int useTicksRemaining;
        int cooldownTicksRemaining;
        int totalUseTicks;
        int totalCooldownTicks;
        BukkitTask tickTask;

        int lastFullCount = BAR_SIZE;
        int burstTicksRemaining = 0;

        // FIX 1: Track previous flight state to prevent altering survival/creative
        // setups
        boolean previousAllowFlight = false;
    }

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {

        if (!configManager.isItemEnabled(ArtifactItem.HELIUM_FLAMINGO.getId()))
            return;

        Player player = event.getPlayer();

        if (!ArtifactUtil.hasEquipped(player, ArtifactItem.HELIUM_FLAMINGO, api))
            return;

        if (!event.isSprinting())
            return;

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
            return;

        if (player.isOnGround() || player.getLocation().getBlock().isLiquid())
            return;

        UUID uuid = player.getUniqueId();
        FlamingoState state = playerStates.get(uuid);

        if (state != null && state.phase == Phase.FLYING) {
            stopFlying(player, state);
            return;
        }

        if (state != null && state.phase == Phase.COOLDOWN) {
            return;
        }

        activateFlying(player);
    }

    private void activateFlying(Player player) {
        UUID uuid = player.getUniqueId();
        FlamingoState state = new FlamingoState();
        state.totalUseTicks = configManager.getHeliumFlamingoUseSeconds() * 20;
        state.useTicksRemaining = state.totalUseTicks;
        state.totalCooldownTicks = configManager.getHeliumFlamingoCooldown() * 20;
        state.cooldownTicksRemaining = state.totalCooldownTicks;
        state.phase = Phase.FLYING;
        state.lastFullCount = BAR_SIZE;

        // FIX 1: Save flight state and bypass vanilla anti-cheat flying kicks
        state.previousAllowFlight = player.getAllowFlight();
        player.setAllowFlight(true);

        playerStates.put(uuid, state);

        // FIX 1: Gliding forces the horizontal pose in the air. Swimming does not.
        player.setGliding(true);
        player.setMetadata(CuriosPaperAPI.HIDE_MODELS_METADATA, new FixedMetadataValue(plugin, true));

        state.tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cleanupPlayer(uuid);
                    return;
                }

                FlamingoState current = playerStates.get(uuid);
                if (current == null) {
                    this.cancel();
                    return;
                }

                if (current.phase == Phase.FLYING) {
                    if (!ArtifactUtil.hasEquipped(player, ArtifactItem.HELIUM_FLAMINGO, api)
                            || !configManager.isItemEnabled(ArtifactItem.HELIUM_FLAMINGO.getId())) {
                        stopFlying(player, current);
                    } else {
                        tickFlying(player, current);
                    }
                } else if (current.phase == Phase.COOLDOWN) {
                    tickCooldown(player, current);
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void tickFlying(Player player, FlamingoState state) {
        if (player.isOnGround()) {
            stopFlying(player, state);
            return;
        }

        // FIX 1: Maintain Elytra pose in the air
        if (!player.isGliding()) {
            player.setGliding(true);
        }

        // FIX 1: Prevent them from double-jumping into actual creative flight
        if (player.isFlying()) {
            player.setFlying(false);
        }

        Vector direction = player.getLocation().getDirection().normalize().multiply(0.3);
        player.setVelocity(direction);

        state.useTicksRemaining--;
        sendBar(player, state);

        if (state.useTicksRemaining <= 0) {
            stopFlying(player, state);
        }
    }

    private void tickCooldown(Player player, FlamingoState state) {
        state.cooldownTicksRemaining--;
        sendBar(player, state);

        if (state.cooldownTicksRemaining <= 0) {
            state.phase = Phase.READY;
            playerStates.remove(player.getUniqueId());
            if (state.tickTask != null) {
                state.tickTask.cancel();
            }
        }
    }

    private void stopFlying(Player player, FlamingoState state) {
        // FIX 1: Remove glide and restore original flight rules
        player.setGliding(false);
        player.setAllowFlight(state.previousAllowFlight);
        player.removeMetadata(CuriosPaperAPI.HIDE_MODELS_METADATA, plugin);

        state.phase = Phase.COOLDOWN;

        // FIX 2: Calculate proportional cooldown based on time used
        double fractionUsed = 1.0 - ((double) state.useTicksRemaining / state.totalUseTicks);
        state.cooldownTicksRemaining = (int) (state.totalCooldownTicks * fractionUsed);

        state.lastFullCount = 0;
        state.burstTicksRemaining = 0;
    }

    private void sendBar(Player player, FlamingoState state) {
        int fullIcons;

        if (state.phase == Phase.FLYING) {
            if (state.totalUseTicks > 0) {
                fullIcons = (int) Math.ceil((double) state.useTicksRemaining / state.totalUseTicks * BAR_SIZE);
            } else {
                fullIcons = 0;
            }
        } else {
            if (state.totalCooldownTicks > 0) {
                int elapsed = state.totalCooldownTicks - state.cooldownTicksRemaining;
                fullIcons = (int) Math.floor((double) elapsed / state.totalCooldownTicks * BAR_SIZE);
            } else {
                fullIcons = BAR_SIZE;
            }
        }

        fullIcons = Math.max(0, Math.min(BAR_SIZE, fullIcons));
        int emptyIcons = BAR_SIZE - fullIcons;

        if (state.phase == Phase.FLYING && fullIcons < state.lastFullCount) {
            state.burstTicksRemaining = 2;
        }
        state.lastFullCount = fullIcons;

        StringBuilder bar = new StringBuilder();

        boolean showBurst = state.burstTicksRemaining > 0
                && state.phase == Phase.FLYING
                && emptyIcons > 0;

        if (showBurst) {
            for (int i = 0; i < emptyIcons - 1; i++) {
                bar.append(FLAMINGO_EMPTY);
            }
            bar.append(FLAMINGO_BURST);
        } else {
            for (int i = 0; i < emptyIcons; i++) {
                bar.append(FLAMINGO_EMPTY);
            }
        }
        for (int i = 0; i < fullIcons; i++) {
            bar.append(FLAMINGO_FULL);
        }

        if (state.burstTicksRemaining > 0) {
            state.burstTicksRemaining--;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId());
    }

    private void cleanupPlayer(UUID uuid) {
        FlamingoState state = playerStates.remove(uuid);
        if (state != null) {
            if (state.tickTask != null) {
                state.tickTask.cancel();
            }
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // FIX 1: Clean up pose and flight logic
                player.setGliding(false);
                player.setAllowFlight(state.previousAllowFlight);
                player.removeMetadata(CuriosPaperAPI.HIDE_MODELS_METADATA, plugin);
            }
        }
    }

    public void shutdown() {
        Iterator<Map.Entry<UUID, FlamingoState>> it = playerStates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, FlamingoState> entry = it.next();
            FlamingoState state = entry.getValue();
            if (state.tickTask != null) {
                state.tickTask.cancel();
            }
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                // FIX 1: Clean up pose and flight logic
                player.setGliding(false);
                player.setAllowFlight(state.previousAllowFlight);
                player.removeMetadata(CuriosPaperAPI.HIDE_MODELS_METADATA, plugin);
            }
            it.remove();
        }
    }
}