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

public class EcoCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public EcoCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.eco")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (!plugin.getEconomyManager().isAvailable()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("eco_vault_not_found"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("usage_eco"));
            return true;
        }

        String sub = args[0].toLowerCase();
        String targetArg = args[1];

        if (sub.equals("reset")) {
            handleReset(sender, targetArg);
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("usage_eco"));
            return true;
        }

        String amountStr = args[2];
        boolean isPercent = amountStr.endsWith("%");
        double parsedAmount;
        try {
            String rawNum = isPercent ? amountStr.substring(0, amountStr.length() - 1) : amountStr;
            parsedAmount = Double.parseDouble(rawNum);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("eco_invalid_amount"));
            return true;
        }

        if (Double.isNaN(parsedAmount) || Double.isInfinite(parsedAmount) || parsedAmount < 0) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("eco_invalid_amount"));
            return true;
        }

        switch (sub) {
            case "give":
                handleGive(sender, targetArg, parsedAmount, isPercent);
                break;
            case "take":
                handleTake(sender, targetArg, parsedAmount, isPercent);
                break;
            case "set":
                handleSet(sender, targetArg, parsedAmount);
                break;
            default:
                sender.sendMessage(plugin.getLanguageManager().getMessage("usage_eco"));
                break;
        }

        return true;
    }

    private void handleGive(CommandSender sender, String targetArg, double amount, boolean isPercent) {
        List<OfflinePlayer> targets = resolveTargets(targetArg);
        if (targets.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return;
        }

        for (OfflinePlayer target : targets) {
            double giveAmount = amount;
            if (isPercent) {
                double bal = plugin.getEconomyManager().getBalance(target);
                giveAmount = bal * (amount / 100.0);
            }
            plugin.getEconomyManager().deposit(target, giveAmount);
        }

        String formatted = plugin.getEconomyManager().format(amount);
        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_give_success",
                "target", targetArg,
                "amount", isPercent ? amount + "%" : formatted));
    }

    private void handleTake(CommandSender sender, String targetArg, double amount, boolean isPercent) {
        List<OfflinePlayer> targets = resolveTargets(targetArg);
        if (targets.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return;
        }

        for (OfflinePlayer target : targets) {
            double takeAmount = amount;
            if (isPercent) {
                double bal = plugin.getEconomyManager().getBalance(target);
                takeAmount = bal * (amount / 100.0);
            }
            plugin.getEconomyManager().withdraw(target, takeAmount);
        }

        String formatted = plugin.getEconomyManager().format(amount);
        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_take_success",
                "target", targetArg,
                "amount", isPercent ? amount + "%" : formatted));
    }

    private void handleSet(CommandSender sender, String targetArg, double amount) {
        List<OfflinePlayer> targets = resolveTargets(targetArg);
        if (targets.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return;
        }

        for (OfflinePlayer target : targets) {
            plugin.getEconomyManager().setBalance(target, amount);
        }

        String formatted = plugin.getEconomyManager().format(amount);
        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_set_success",
                "target", targetArg,
                "amount", formatted));
    }

    private void handleReset(CommandSender sender, String targetArg) {
        List<OfflinePlayer> targets = resolveTargets(targetArg);
        if (targets.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return;
        }

        for (OfflinePlayer target : targets) {
            plugin.getEconomyManager().resetBalance(target);
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_reset_success",
                "target", targetArg));
    }

    private List<OfflinePlayer> resolveTargets(String arg) {
        if (arg.equals("*") || arg.equalsIgnoreCase("**")) {
            return new ArrayList<>(Bukkit.getOnlinePlayers());
        }

        Player online = Bukkit.getPlayer(arg);
        if (online != null) {
            return Collections.singletonList(online);
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(arg);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return Collections.singletonList(offline);
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.eco")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = Arrays.asList("give", "take", "set", "reset");
            String query = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(query)) matches.add(s);
            }
            return matches;
        }

        if (args.length == 2) {
            String query = args[1].toLowerCase();
            List<String> matches = new ArrayList<>();
            if ("*".startsWith(query)) matches.add("*");
            if ("**".startsWith(query)) matches.add("**");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(query)) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }

        if (args.length == 3 && !args[0].equalsIgnoreCase("reset")) {
            return Arrays.asList("100", "500", "1000", "10%", "50%");
        }

        return Collections.emptyList();
    }
}
