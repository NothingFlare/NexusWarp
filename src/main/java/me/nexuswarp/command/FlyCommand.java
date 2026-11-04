package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FlyCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public FlyCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /fly (toggle self)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                return true;
            }

            if (!sender.hasPermission("nexuswarp.fly") && !sender.hasPermission("essentials.fly")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }

            boolean newState = !player.getAllowFlight();
            setFlight(player, newState);

            String key = newState ? "fly_enabled_self" : "fly_disabled_self";
            player.sendMessage(plugin.getLanguageManager().getMessage(key));
            return true;
        }

        // /fly on|off (self explicitly)
        if (args.length == 1 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")
                || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")
                || args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("false"))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                return true;
            }

            if (!sender.hasPermission("nexuswarp.fly") && !sender.hasPermission("essentials.fly")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }

            boolean newState = isEnable(args[0]);
            setFlight(player, newState);

            String key = newState ? "fly_enabled_self" : "fly_disabled_self";
            player.sendMessage(plugin.getLanguageManager().getMessage(key));
            return true;
        }

        // /fly <player> [on|off]
        if (!sender.hasPermission("nexuswarp.fly.others") && !sender.hasPermission("essentials.fly.others")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found"));
            return true;
        }

        boolean newState;
        if (args.length >= 2) {
            newState = isEnable(args[1]);
        } else {
            newState = !target.getAllowFlight();
        }

        setFlight(target, newState);

        String senderKey = newState ? "fly_enabled_other" : "fly_disabled_other";
        sender.sendMessage(plugin.getLanguageManager().getMessage(senderKey, "player", target.getName()));

        String targetKey = newState ? "fly_target_enabled" : "fly_target_disabled";
        target.sendMessage(plugin.getLanguageManager().getMessage(targetKey, "sender", sender.getName()));

        return true;
    }

    private void setFlight(Player player, boolean enable) {
        player.setAllowFlight(enable);
        if (!enable) {
            player.setFlying(false);
        } else {
            player.setFallDistance(0.0f);
        }
    }

    private boolean isEnable(String arg) {
        return arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("enable") || arg.equalsIgnoreCase("true") || arg.equals("1");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);

            if (sender.hasPermission("nexuswarp.fly") || sender.hasPermission("essentials.fly")) {
                if ("on".startsWith(prefix)) list.add("on");
                if ("off".startsWith(prefix)) list.add("off");
            }

            if (sender.hasPermission("nexuswarp.fly.others") || sender.hasPermission("essentials.fly.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        list.add(p.getName());
                    }
                }
            }
            return list;
        }

        if (args.length == 2 && (sender.hasPermission("nexuswarp.fly.others") || sender.hasPermission("essentials.fly.others"))) {
            List<String> list = new ArrayList<>();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if ("on".startsWith(prefix)) list.add("on");
            if ("off".startsWith(prefix)) list.add("off");
            return list;
        }

        return Collections.emptyList();
    }
}
