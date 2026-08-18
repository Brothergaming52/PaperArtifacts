package org.bg52.artifacts.command;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * /artifacts mimic — spawns a mimic chest at the player's current location.
 * /artifacts reload — reloads the config.
 * /artifacts list — lists all artifact items.
 *
 * Permission: artifacts.admin
 */
public class ArtifactsCommand implements CommandExecutor, TabCompleter {

    private final Artifacts plugin;

    public ArtifactsCommand(Artifacts plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("artifacts.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "mimic":
                return handleMimic(sender);
            case "reload":
                return handleReload(sender);
            case "list":
                return handleList(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleMimic(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        org.bukkit.block.Block block = player.getLocation().getBlock();
        block.setType(org.bukkit.Material.CHEST);
        plugin.getMimicManager().addMimicChest(block.getLocation());

        player.sendMessage("\u00a7aSpawned a mimic chest at your location.");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.getArtifactConfig().reload();
        if (plugin.getArtifactItemsConfig() != null) {
            plugin.getArtifactItemsConfig().reload();
        }
        if (plugin.getCampsiteConfig() != null) {
            plugin.getCampsiteConfig().reload();
        }
        if (plugin.getMimicConfig() != null) {
            plugin.getMimicConfig().reload();
        }
        if (plugin.getStoneTierManager() != null) {
            plugin.getStoneTierManager().load();
        }
        if (plugin.getPassiveArtifactsListener() != null) {
            plugin.getPassiveArtifactsListener().reload();
        }
        org.bg52.artifacts.util.FoodUtil.loadFoodInfo(plugin);
        if (plugin.getItemRegistry() != null) {
            plugin.getItemRegistry().registerAll();
        }
        sender.sendMessage("\u00a7aArtifacts config reloaded.");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage("\u00a76\u00a7lAvailable Artifacts:");
        for (ArtifactItem artifact : ArtifactItem.values()) {
            boolean enabled = plugin.getArtifactConfig().isItemEnabled(artifact.getId());
            String status = enabled ? "\u00a7a\u2713" : "\u00a7c\u2717";
            String slotInfo = artifact.isCurio() ? " \u00a78[" + artifact.getSlotType() + "]" : " \u00a78[hand]";
            sender.sendMessage("  " + status + " " + artifact.getDisplayName() + slotInfo
                    + " \u00a78(" + artifact.getId() + ")");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("\u00a76\u00a7lArtifacts Commands:");
        sender.sendMessage("  \u00a7e/artifacts mimic \u00a77- Spawn a mimic chest at your location");
        sender.sendMessage("  \u00a7e/artifacts list \u00a77- List all artifacts");
        sender.sendMessage("  \u00a7e/artifacts reload \u00a77- Reload config");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<String>();

        if (args.length == 1) {
            completions.add("mimic");
            completions.add("list");
            completions.add("reload");
        }

        return completions;
    }
}
