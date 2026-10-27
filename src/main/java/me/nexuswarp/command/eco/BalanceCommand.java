package me.nexuswarp.command.eco;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public BalanceCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.balance")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        // /balance (own balance)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                return true;
            }

            BigDecimal balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            String formatted = plugin.getEconomyManager().format(balance);
            sender.sendMessage(plugin.getLanguageManager().getMessage("eco_balance_self", "amount", formatted));
            return true;
        }

        // /balance <player> (other balance)
        if (!sender.hasPermission("nexuswarp.balance.others")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        String targetName = args[0];
        UUID targetUuid = plugin.getEconomyManager().getUuidByName(targetName);
        if (targetUuid == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return true;
        }

        BigDecimal balance = plugin.getEconomyManager().getBalance(targetUuid);
        String formatted = plugin.getEconomyManager().format(balance);
        String resolvedName = plugin.getEconomyManager().getNameByUuid(targetUuid);
        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_balance_other",
                "player", resolvedName,
                "amount", formatted));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.balance.others")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
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
