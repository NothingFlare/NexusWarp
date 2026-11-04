package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.DateUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class UptimeCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public UptimeCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.uptime") && !sender.hasPermission("essentials.uptime")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String lang = plugin.getLanguageManager().getCurrentLanguage();
        String uptimeStr = DateUtil.formatDuration(uptimeMs, lang);

        Runtime rt = Runtime.getRuntime();
        long maxMb = rt.maxMemory() / 1048576L;
        long totalMb = rt.totalMemory() / 1048576L;
        long freeMb = rt.freeMemory() / 1048576L;
        long usedMb = totalMb - freeMb;

        double[] tps = Bukkit.getTPS();
        String tps1m = formatTps(tps.length > 0 ? tps[0] : 20.0);
        String tps5m = formatTps(tps.length > 1 ? tps[1] : 20.0);
        String tps15m = formatTps(tps.length > 2 ? tps[2] : 20.0);

        sender.sendMessage(plugin.getLanguageManager().getMessage("uptime_header"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("uptime_time", "time", uptimeStr));
        sender.sendMessage(plugin.getLanguageManager().getMessage("uptime_tps",
                "tps_1m", tps1m,
                "tps_5m", tps5m,
                "tps_15m", tps15m));
        sender.sendMessage(plugin.getLanguageManager().getMessage("uptime_memory",
                "used", String.valueOf(usedMb),
                "total", String.valueOf(totalMb),
                "max", String.valueOf(maxMb),
                "free", String.valueOf(freeMb)));

        for (World world : Bukkit.getWorlds()) {
            int chunks = world.getLoadedChunks().length;
            int entities = world.getEntities().size();
            sender.sendMessage(plugin.getLanguageManager().getMessage("uptime_world_entry",
                    "world", world.getName(),
                    "chunks", String.valueOf(chunks),
                    "entities", String.valueOf(entities)));
        }

        return true;
    }

    private String formatTps(double tpsVal) {
        double bounded = Math.min(20.0, Math.max(0.0, tpsVal));
        String color;
        if (bounded >= 18.0) {
            color = "&a";
        } else if (bounded >= 15.0) {
            color = "&e";
        } else {
            color = "&c";
        }
        return color + String.format(Locale.US, "%.2f", bounded);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
