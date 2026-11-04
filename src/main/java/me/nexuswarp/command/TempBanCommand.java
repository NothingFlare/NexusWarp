package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.DateUtil;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TempBanCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public TempBanCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.tempban") && !sender.hasPermission("essentials.tempban")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("usage_tempban"));
            return true;
        }

        String targetName = args[0];
        Player onlineTarget = Bukkit.getPlayer(targetName);

        // Exemption check
        if (onlineTarget != null && (onlineTarget.hasPermission("nexuswarp.tempban.exempt") || onlineTarget.hasPermission("essentials.tempban.exempt"))
                && sender instanceof Player) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("tempban_exempt", "player", onlineTarget.getName()));
            return true;
        }

        long expireEpoch;
        try {
            expireEpoch = DateUtil.parseDateDiff(args[1], true);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("invalid_time_format", "time", args[1]));
            return true;
        }

        Date expireDate = new Date(expireEpoch);
        String reason = null;

        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(args[i]);
            }
            reason = sb.toString();
        }

        String defaultReason = plugin.getLanguageManager().getMessage("tempban_default_reason");
        String finalReason = (reason != null && !reason.trim().isEmpty()) ? reason : defaultReason;

        // Apply ban via Bukkit BanList
        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, finalReason, expireDate, sender.getName());

        String lang = plugin.getLanguageManager().getCurrentLanguage();
        String durationStr = DateUtil.formatDuration(Math.max(0L, expireEpoch - System.currentTimeMillis()), lang);
        String formattedDate = DateUtil.formatDateTime(expireEpoch, lang);

        // Kick if online
        if (onlineTarget != null && onlineTarget.isOnline()) {
            String kickMsg = plugin.getLanguageManager().getMessage("tempban_kick",
                    "reason", finalReason,
                    "time", durationStr,
                    "expires", formattedDate);
            onlineTarget.kickPlayer(kickMsg);
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("tempban_success",
                "player", targetName,
                "time", durationStr,
                "reason", finalReason));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.tempban") && !sender.hasPermission("essentials.tempban")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    list.add(p.getName());
                }
            }
            return list;
        }

        if (args.length == 2) {
            List<String> suggestions = Arrays.asList("1h", "12h", "1d", "3d", "7d", "14d", "30d");
            List<String> list = new ArrayList<>();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (String s : suggestions) {
                if (s.startsWith(prefix)) {
                    list.add(s);
                }
            }
            return list;
        }

        return Collections.emptyList();
    }
}
