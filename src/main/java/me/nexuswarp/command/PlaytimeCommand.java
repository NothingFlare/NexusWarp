package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.DateUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PlaytimeCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;
    private final Statistic playTimeStat;

    public PlaytimeCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
        Statistic stat;
        try {
            stat = Statistic.valueOf("PLAY_ONE_MINUTE");
        } catch (IllegalArgumentException e) {
            stat = Statistic.valueOf("PLAY_ONE_TICK");
        }
        this.playTimeStat = stat;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /playtime (self)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                return true;
            }

            if (!sender.hasPermission("nexuswarp.playtime") && !sender.hasPermission("essentials.playtime")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }

            long ticks = player.getStatistic(playTimeStat);
            long millis = ticks * 50L;
            String formatted = DateUtil.formatDuration(millis, plugin.getLanguageManager().getCurrentLanguage());
            sender.sendMessage(plugin.getLanguageManager().getMessage("playtime_self", "time", formatted));
            return true;
        }

        // /playtime <player> (others)
        if (!sender.hasPermission("nexuswarp.playtime.others") && !sender.hasPermission("essentials.playtime.others")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getPlayer(targetName);

        if (target == null) {
            UUID uuid = plugin.getEconomyManager().getUuidByName(targetName);
            if (uuid != null) {
                target = Bukkit.getOfflinePlayer(uuid);
            } else {
                target = Bukkit.getOfflinePlayer(targetName);
            }
        }

        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return true;
        }

        long ticks = 0;
        if (target.isOnline() && target.getPlayer() != null) {
            ticks = target.getPlayer().getStatistic(playTimeStat);
        } else {
            try {
                ticks = target.getStatistic(playTimeStat);
            } catch (Throwable ignored) {
                ticks = 0;
            }
        }

        long millis = ticks * 50L;
        String formatted = DateUtil.formatDuration(millis, plugin.getLanguageManager().getCurrentLanguage());
        String resolvedName = target.getName() != null ? target.getName() : targetName;

        sender.sendMessage(plugin.getLanguageManager().getMessage("playtime_other",
                "player", resolvedName,
                "time", formatted));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.playtime.others") && !sender.hasPermission("essentials.playtime.others")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String query = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(query)) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }

        return Collections.emptyList();
    }
}
