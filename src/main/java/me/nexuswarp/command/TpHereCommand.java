package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.ServerScheduler;
import me.nexuswarp.util.TeleportSafetyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TpHereCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public TpHereCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player playerSender)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
            return true;
        }

        if (!playerSender.hasPermission("nexuswarp.tphere")) {
            playerSender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            playerSender.sendMessage(plugin.getLanguageManager().getMessage("usage_tphere"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            playerSender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return true;
        }

        Location dest = playerSender.getLocation();
        if (plugin.getConfigManager().isSafeTeleportEnabled()) {
            Location safe = TeleportSafetyUtil.getSafeLocation(
                    dest,
                    plugin.getConfigManager().isPreventLava(),
                    plugin.getConfigManager().isPreventVoid(),
                    plugin.getConfigManager().isPreventSuffocation()
            );
            if (safe != null) {
                dest = safe;
            }
        }

        final Location finalDest = dest;
        ServerScheduler.teleportAsync(target, finalDest).thenAccept(success -> {
            ServerScheduler.runOnPlayer(plugin, target, () -> {
                if (success) {
                    target.sendMessage(plugin.getLanguageManager().getMessage("teleport_success", "player", playerSender.getName()));
                    playerSender.sendMessage(plugin.getLanguageManager().getMessage("tphere_success", "player", target.getName()));
                } else {
                    playerSender.sendMessage(plugin.getLanguageManager().getMessage("teleport_unsafe"));
                }
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.tphere")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String query = args[0].toLowerCase();
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player ps && ps.getUniqueId().equals(p.getUniqueId())) continue;
                if (p.getName().toLowerCase().startsWith(query)) {
                    list.add(p.getName());
                }
            }
            return list;
        }

        return Collections.emptyList();
    }
}
