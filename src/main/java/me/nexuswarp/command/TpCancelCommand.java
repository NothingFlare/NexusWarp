package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TpCancelCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public TpCancelCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
            return true;
        }

        if (!player.hasPermission("nexuswarp.use")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        String targetPlayerName = (args.length > 0) ? args[0] : null;
        plugin.getTpaManager().cancelRequest(player, targetPlayerName);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String query = args[0].toLowerCase();
            List<String> names = plugin.getTpaManager().getRelevantNamesForCancel(player);
            List<String> matches = new ArrayList<>();
            for (String name : names) {
                if (name.toLowerCase().startsWith(query)) {
                    matches.add(name);
                }
            }
            return matches;
        }

        return Collections.emptyList();
    }
}
