package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetHubCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public SetHubCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
            return true;
        }

        if (!player.hasPermission("nexuswarp.sethub")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        plugin.getConfigManager().setHubLocation(player.getLocation());
        player.sendMessage(plugin.getLanguageManager().getMessage("sethub_success"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
