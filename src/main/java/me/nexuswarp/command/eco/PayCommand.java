package me.nexuswarp.command.eco;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.MaxMoneyException;
import me.nexuswarp.economy.NoLoanPermittedException;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public PayCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
            return true;
        }

        if (!player.hasPermission("nexuswarp.pay")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("usage_pay"));
            return true;
        }

        String targetName = args[0];
        UUID targetUuid = plugin.getEconomyManager().getUuidByName(targetName);
        if (targetUuid == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return true;
        }

        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_self"));
            return true;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_invalid_amount"));
            return true;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_must_be_positive"));
            return true;
        }

        BigDecimal minPay = plugin.getEconomyManager().getMinimumPayAmount();
        if (amount.compareTo(minPay) < 0) {
            String formattedMin = plugin.getEconomyManager().format(minPay);
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_minimum_pay_amount", "amount", formattedMin));
            return true;
        }

        if (!plugin.getEconomyManager().hasEnough(player.getUniqueId(), amount)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_insufficient_funds"));
            return true;
        }

        try {
            plugin.getEconomyManager().withdraw(player.getUniqueId(), amount);
            plugin.getEconomyManager().deposit(targetUuid, amount);

            String formattedAmount = plugin.getEconomyManager().format(amount);
            String resolvedTargetName = plugin.getEconomyManager().getNameByUuid(targetUuid);

            player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_sent",
                    "player", resolvedTargetName,
                    "amount", formattedAmount));

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_received",
                        "player", player.getName(),
                        "amount", formattedAmount));
            }
        } catch (NoLoanPermittedException e) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_insufficient_funds"));
        } catch (MaxMoneyException e) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_max_money_error"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.pay")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String query = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player ps && ps.getUniqueId().equals(p.getUniqueId())) continue;
                if (p.getName().toLowerCase().startsWith(query)) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }

        if (args.length == 2) {
            return Arrays.asList("10", "50", "100", "500", "1000");
        }

        return Collections.emptyList();
    }
}
