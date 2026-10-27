package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ReplyCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public ReplyCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.msg")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("usage_reply"));
            return true;
        }

        UUID senderUUID = (sender instanceof Player p) ? p.getUniqueId() : plugin.getMessageManager().getConsoleUUID();
        UUID targetUUID = plugin.getMessageManager().getReplyTarget(senderUUID);

        if (targetUUID == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_reply_target"));
            return true;
        }

        Player recipient = Bukkit.getPlayer(targetUUID);
        if (recipient == null || !recipient.isOnline()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_online"));
            return true;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(args[i]);
        }

        plugin.getMessageManager().sendPrivateMessage(sender, recipient, sb.toString());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
