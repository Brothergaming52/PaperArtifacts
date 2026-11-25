package org.bg52.artifacts.command;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.item.ArtifactItem;
import org.bg52.synthetix.util.TextUtil;
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
            sender.sendMessage(TextUtil.parse("<red>You don't have permission to use this command.</red>"));
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
            sender.sendMessage(TextUtil.parse("<red>Only players can use this command.</red>"));
            return true;
        }

        Player player = (Player) sender;
        org.bukkit.block.Block block = player.getLocation().getBlock();
        block.setType(org.bukkit.Material.CHEST);
        plugin.getMimicManager().addMimicChest(block.getLocation());

        player.sendMessage(TextUtil.parse("<green>Spawned a mimic chest at your location.</green>"));
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
        if (plugin.getPopulator() != null) {
            plugin.getPopulator().reloadConfig(plugin.getCampsiteConfig());
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
        sender.sendMessage(TextUtil.parse("<green>Artifacts config reloaded.</green>"));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(TextUtil.parse("<gold><bold>Available Artifacts:</bold></gold>"));
        for (ArtifactItem artifact : ArtifactItem.values()) {
            boolean enabled = plugin.getArtifactConfig().isItemEnabled(artifact.getId());
            String status = enabled ? "<green>✓</green>" : "<red>✗</red>";
            String slotInfo = artifact.isCurio() ? " <dark_gray>[" + artifact.getSlotType() + "]</dark_gray>" : " <dark_gray>[hand]</dark_gray>";
            sender.sendMessage(TextUtil.parse("  " + status + " " + artifact.getDisplayName() + slotInfo
                    + " <dark_gray>(" + artifact.getId() + ")</dark_gray>"));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(TextUtil.parse("<gold><bold>Artifacts Commands:</bold></gold>"));
        sender.sendMessage(TextUtil.parse("  <yellow>/artifacts mimic</yellow> <gray>- Spawn a mimic chest at your location</gray>"));
        sender.sendMessage(TextUtil.parse("  <yellow>/artifacts list</yellow> <gray>- List all artifacts</gray>"));
        sender.sendMessage(TextUtil.parse("  <yellow>/artifacts reload</yellow> <gray>- Reload config</gray>"));
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
