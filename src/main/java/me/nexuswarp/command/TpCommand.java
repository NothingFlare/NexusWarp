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

public class TpCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public TpCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.tp")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        // 1. /tp <target>
        if (args.length == 1) {
            if (!(sender instanceof Player playerSender)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
                return true;
            }

            teleportPlayer(playerSender, target.getLocation(), target.getName(), sender);
            return true;
        }

        // 2. /tp <player> <target>
        if (args.length == 2) {
            Player subject = Bukkit.getPlayer(args[0]);
            if (subject == null || !subject.isOnline()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
                return true;
            }

            teleportPlayerToPlayer(subject, target, sender);
            return true;
        }

        // 3. /tp <x> <y> <z>
        if (args.length == 3) {
            if (!(sender instanceof Player playerSender)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                return true;
            }

            Double x = parseCoord(args[0], playerSender.getLocation().getX());
            Double y = parseCoord(args[1], playerSender.getLocation().getY());
            Double z = parseCoord(args[2], playerSender.getLocation().getZ());

            if (x == null || y == null || z == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("invalid_coordinates"));
                return true;
            }

            Location dest = new Location(playerSender.getWorld(), x, y, z, playerSender.getLocation().getYaw(), playerSender.getLocation().getPitch());
            teleportPlayer(playerSender, dest, null, sender);
            return true;
        }

        // 4. /tp <player> <x> <y> <z>
        if (args.length == 4) {
            Player subject = Bukkit.getPlayer(args[0]);
            if (subject == null || !subject.isOnline()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
                return true;
            }

            Double x = parseCoord(args[1], subject.getLocation().getX());
            Double y = parseCoord(args[2], subject.getLocation().getY());
            Double z = parseCoord(args[3], subject.getLocation().getZ());

            if (x == null || y == null || z == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("invalid_coordinates"));
                return true;
            }

            Location dest = new Location(subject.getWorld(), x, y, z, subject.getLocation().getYaw(), subject.getLocation().getPitch());
            teleportPlayer(subject, dest, null, sender);
            return true;
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("usage_tp"));
        return true;
    }

    private void teleportPlayer(Player player, Location targetLocation, String targetName, CommandSender notifySender) {
        Location dest = targetLocation;

        if (plugin.getConfigManager().isSafeTeleportEnabled() && !player.hasPermission("nexuswarp.bypass.world")) {
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
        ServerScheduler.teleportAsync(player, finalDest).thenAccept(success -> {
            ServerScheduler.runOnPlayer(plugin, player, () -> {
                if (success) {
                    if (targetName != null) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("teleport_success", "player", targetName));
                        if (!notifySender.equals(player)) {
                            notifySender.sendMessage(plugin.getLanguageManager().getMessage("teleport_success_other",
                                    "player", player.getName(),
                                    "target", targetName));
                        }
                    } else {
                        String x = String.format("%.1f", finalDest.getX());
                        String y = String.format("%.1f", finalDest.getY());
                        String z = String.format("%.1f", finalDest.getZ());
                        player.sendMessage(plugin.getLanguageManager().getMessage("teleport_success_coords",
                                "x", x, "y", y, "z", z));
                        if (!notifySender.equals(player)) {
                            notifySender.sendMessage(plugin.getLanguageManager().getMessage("teleport_success_coords",
                                    "x", x, "y", y, "z", z));
                        }
                    }
                } else {
                    notifySender.sendMessage(plugin.getLanguageManager().getMessage("teleport_unsafe"));
                }
            });
        });
    }

    private void teleportPlayerToPlayer(Player subject, Player target, CommandSender notifySender) {
        Location dest = target.getLocation();
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

        ServerScheduler.teleportAsync(subject, dest).thenAccept(success -> {
            ServerScheduler.runOnPlayer(plugin, subject, () -> {
                if (success) {
                    subject.sendMessage(plugin.getLanguageManager().getMessage("teleport_success", "player", target.getName()));
                    notifySender.sendMessage(plugin.getLanguageManager().getMessage("teleport_success_other",
                            "player", subject.getName(),
                            "target", target.getName()));
                } else {
                    notifySender.sendMessage(plugin.getLanguageManager().getMessage("teleport_unsafe"));
                }
            });
        });
    }

    private Double parseCoord(String input, double current) {
        if (input == null || input.isEmpty()) return null;
        try {
            if (input.startsWith("~")) {
                if (input.length() == 1) return current;
                return current + Double.parseDouble(input.substring(1));
            }
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.tp")) {
            return Collections.emptyList();
        }

        if (args.length == 1 || args.length == 2) {
            String query = args[args.length - 1].toLowerCase();
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
