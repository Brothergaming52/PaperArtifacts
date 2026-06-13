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
 * /artifacts give <item_id> [player] — gives an artifact item to a player.
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
            case "give":
                return handleGive(sender, args);
            case "reload":
                return handleReload(sender);
            case "list":
                return handleList(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /artifacts give <item_id> [player]");
            return true;
        }

        String itemId = args[1].toLowerCase();
        ArtifactItem artifact = ArtifactItem.fromId(itemId);
        if (artifact == null) {
            sender.sendMessage("\u00a7cUnknown artifact: " + itemId);
            sender.sendMessage("\u00a77Use /artifacts list to see available items.");
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("\u00a7cPlayer not found: " + args[2]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("\u00a7cSpecify a player: /artifacts give " + itemId + " <player>");
            return true;
        }

        ItemStack item = plugin.getItemRegistry().createItemStack(artifact);
        if (item == null) {
            sender.sendMessage("\u00a7cFailed to create item: " + itemId);
            return true;
        }

        target.getInventory().addItem(item);
        sender.sendMessage("\u00a7aGave " + artifact.getDisplayName() + " \u00a7ato " + target.getName());

        if (target != sender) {
            target.sendMessage("\u00a7aYou received " + artifact.getDisplayName());
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.getArtifactConfig().reload();
        plugin.getStoneTierManager().load();
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
        sender.sendMessage("  \u00a7e/artifacts give <item_id> [player] \u00a77- Give an artifact");
        sender.sendMessage("  \u00a7e/artifacts list \u00a77- List all artifacts");
        sender.sendMessage("  \u00a7e/artifacts reload \u00a77- Reload config");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<String>();

        if (args.length == 1) {
            completions.add("give");
            completions.add("list");
            completions.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (ArtifactItem artifact : ArtifactItem.values()) {
                if (artifact.getId().startsWith(args[1].toLowerCase())) {
                    completions.add(artifact.getId());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
