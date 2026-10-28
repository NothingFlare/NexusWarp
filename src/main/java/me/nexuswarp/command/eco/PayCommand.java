package me.nexuswarp.command.eco;

import me.nexuswarp.NexusWarpPlugin;
import net.milkbowl.vault.economy.EconomyResponse;
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

        if (!plugin.getEconomyManager().isAvailable()) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_vault_not_found"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("usage_pay"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_self"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_invalid_amount"));
            return true;
        }

        if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_must_be_positive"));
            return true;
        }

        double minPay = plugin.getEconomyManager().getMinimumPayAmount().doubleValue();
        if (amount < minPay) {
            String formattedMin = plugin.getEconomyManager().format(minPay);
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_minimum_pay_amount", "amount", formattedMin));
            return true;
        }

        if (!plugin.getEconomyManager().has(player, amount)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_insufficient_funds"));
            return true;
        }

        EconomyResponse withdrawResp = plugin.getEconomyManager().withdraw(player, amount);
        if (!withdrawResp.transactionSuccess()) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_insufficient_funds"));
            return true;
        }

        EconomyResponse depositResp = plugin.getEconomyManager().deposit(target, amount);
        if (!depositResp.transactionSuccess()) {
            // Refund sender in case deposit failed
            plugin.getEconomyManager().deposit(player, amount);
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_max_money_error"));
            return true;
        }

        String formattedAmount = plugin.getEconomyManager().format(amount);

        player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_sent",
                "player", target.getName(),
                "amount", formattedAmount));

        target.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_received",
                "player", player.getName(),
                "amount", formattedAmount));

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
