package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MsgCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public MsgCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.msg")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("usage_msg"));
            return true;
        }

        Player recipient = Bukkit.getPlayer(args[0]);
        if (recipient == null || !recipient.isOnline()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return true;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(args[i]);
        }

        plugin.getMessageManager().sendPrivateMessage(sender, recipient, sb.toString());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.msg")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String query = args[0].toLowerCase();
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player ps && ps.getUniqueId().equals(p.getUniqueId())) {
                    continue;
                }
                if (p.getName().toLowerCase().startsWith(query)) {
                    list.add(p.getName());
                }
            }
            return list;
        }

        return Collections.emptyList();
    }
}
