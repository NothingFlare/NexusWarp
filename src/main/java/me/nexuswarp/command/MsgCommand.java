package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.UserAccount;
import me.nexuswarp.util.DateUtil;
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
        if (!sender.hasPermission("nexuswarp.msg") && !sender.hasPermission("essentials.msg")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (sender instanceof Player player) {
            UserAccount account = plugin.getEconomyManager().getAccount(player.getUniqueId());
            if (account != null && account.isMuted()) {
                String lang = plugin.getLanguageManager().getCurrentLanguage();
                long until = account.getMutedUntil();
                String reason = account.getMuteReason() != null ? account.getMuteReason() : plugin.getLanguageManager().getMessage("mute_no_reason");
                if (until > 0L) {
                    long remaining = Math.max(0L, until - System.currentTimeMillis());
                    String timeStr = DateUtil.formatDuration(remaining, lang);
                    sender.sendMessage(plugin.getLanguageManager().getMessage("mute_chat_blocked_temp", "time", timeStr, "reason", reason));
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("mute_chat_blocked_permanent", "reason", reason));
                }
                return true;
            }
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
        if (!sender.hasPermission("nexuswarp.msg") && !sender.hasPermission("essentials.msg")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String query = args[0].toLowerCase();
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(query)) {
                    list.add(p.getName());
                }
            }
            return list;
        }

        return Collections.emptyList();
    }
}
