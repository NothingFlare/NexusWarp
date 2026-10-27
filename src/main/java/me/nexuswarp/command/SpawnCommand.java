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

public class SpawnCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public SpawnCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfigManager().isSpawnEnabled()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("spawn_disabled"));
            return true;
        }

        Location loc = plugin.getConfigManager().getSpawnLocation();
        if (loc == null || loc.getWorld() == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("spawn_world_not_found", "world", plugin.getConfigManager().getSpawnWorldName()));
            return true;
        }

        Location destLoc = loc.clone();
        if (plugin.getConfigManager().isSafeTeleportEnabled()) {
            Location safeLoc = TeleportSafetyUtil.getSafeLocation(
                    destLoc,
                    plugin.getConfigManager().isPreventLava(),
                    plugin.getConfigManager().isPreventVoid(),
                    plugin.getConfigManager().isPreventSuffocation()
            );
            if (safeLoc == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("teleport_unsafe"));
                return true;
            }
            safeLoc.setYaw(destLoc.getYaw());
            safeLoc.setPitch(destLoc.getPitch());
            destLoc = safeLoc;
        }

        // Handle teleporting another player (e.g. /spawn <player>)
        if (args.length >= 1) {
            if (!sender.hasPermission("nexuswarp.spawn.others")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
                return true;
            }

            final Location finalDest = destLoc;
            ServerScheduler.teleportAsync(target, finalDest).thenAccept(success -> {
                if (success) {
                    ServerScheduler.runOnPlayer(plugin, target, () -> {
                        target.sendMessage(plugin.getLanguageManager().getMessage("spawn_teleport_success"));
                    });
                    if (sender instanceof Player ps) {
                        ServerScheduler.runOnPlayer(plugin, ps, () -> {
                            ps.sendMessage(plugin.getLanguageManager().getMessage("spawn_teleport_other", "player", target.getName()));
                        });
                    } else {
                        sender.sendMessage(plugin.getLanguageManager().getMessage("spawn_teleport_other", "player", target.getName()));
                    }
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("teleport_unsafe"));
                }
            });
            return true;
        }

        // Self teleport
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
            return true;
        }

        if (!player.hasPermission("nexuswarp.spawn")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (plugin.getConfigManager().isWorldDisabled(player.getWorld())) {
            if (!player.hasPermission("nexuswarp.bypass.world") || plugin.getConfigManager().isIgnoreWorldBypass()) {
                player.sendMessage(plugin.getLanguageManager().getMessage("disabled_world"));
                return true;
            }
        }

        plugin.getTpaManager().teleportWithWarmup(player, destLoc, "spawn_teleport_success");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("nexuswarp.spawn.others")) {
            String query = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(query)) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }
        return Collections.emptyList();
    }
}
