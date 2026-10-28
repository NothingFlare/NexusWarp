package me.nexuswarp.command.eco;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class PayToggleCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public PayToggleCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.paytoggle") && !sender.hasPermission("essentials.paytoggle")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        Player target = null;
        Boolean enable = null;

        String lowerLabel = label.toLowerCase(Locale.ROOT);
        if (lowerLabel.contains("payon")) {
            enable = true;
        } else if (lowerLabel.contains("payoff")) {
            enable = false;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                return true;
            }
            target = player;
            if (enable == null) {
                enable = !plugin.getEconomyManager().isAcceptingPay(target.getUniqueId());
            }
        } else if (args.length == 1) {
            String arg = args[0].toLowerCase(Locale.ROOT);
            if (arg.equals("on") || arg.equals("enable") || arg.equals("true")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                    return true;
                }
                target = player;
                enable = true;
            } else if (arg.equals("off") || arg.equals("disable") || arg.equals("false")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                    return true;
                }
                target = player;
                enable = false;
            } else {
                // Toggling other player
                if (!sender.hasPermission("nexuswarp.paytoggle.others") && !sender.hasPermission("essentials.paytoggle.others")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                    return true;
                }
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
                    return true;
                }
                if (enable == null) {
                    enable = !plugin.getEconomyManager().isAcceptingPay(target.getUniqueId());
                }
            }
        } else {
            // /paytoggle <player> <on|off>
            if (!sender.hasPermission("nexuswarp.paytoggle.others") && !sender.hasPermission("essentials.paytoggle.others")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
                return true;
            }
            String state = args[1].toLowerCase(Locale.ROOT);
            enable = state.equals("on") || state.equals("enable") || state.equals("true");
        }

        plugin.getEconomyManager().setAcceptingPay(target.getUniqueId(), enable);

        String msgKey = enable ? "eco_pay_toggle_on" : "eco_pay_toggle_off";
        target.sendMessage(plugin.getLanguageManager().getMessage(msgKey));

        if (!(sender instanceof Player p) || !p.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(msgKey));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("on", "off"));
            if (sender.hasPermission("nexuswarp.paytoggle.others") || sender.hasPermission("essentials.paytoggle.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    options.add(p.getName());
                }
            }
            return options;
        }
        if (args.length == 2 && (sender.hasPermission("nexuswarp.paytoggle.others") || sender.hasPermission("essentials.paytoggle.others"))) {
            return Arrays.asList("on", "off");
        }
        return Collections.emptyList();
    }
}
