package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class NexusWarpCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public NexusWarpCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginAsync(() -> {
                sender.sendMessage(plugin.getLanguageManager().getMessage("admin_reload"));
            });
            return true;
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("usage_admin"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                return Collections.singletonList("reload");
            }
        }

        return Collections.emptyList();
    }
}
