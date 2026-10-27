package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetSpawnCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public SetSpawnCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
            return true;
        }

        if (!player.hasPermission("nexuswarp.setspawn")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        plugin.getConfigManager().setSpawnLocation(player.getLocation());
        player.sendMessage(plugin.getLanguageManager().getMessage("setspawn_success"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
