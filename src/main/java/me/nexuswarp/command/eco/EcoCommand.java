package me.nexuswarp.command.eco;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.EconomyUtil;
import me.nexuswarp.economy.MaxMoneyException;
import me.nexuswarp.economy.NoLoanPermittedException;
import me.nexuswarp.economy.UserAccount;
import me.nexuswarp.economy.event.UserBalanceUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;

public class EcoCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public EcoCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.eco") && !sender.hasPermission("essentials.eco")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("usage_eco"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
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
        BigDecimal parsedAmount;
        try {
            String rawNum = isPercent ? amountStr.substring(0, amountStr.length() - 1) : amountStr;
            parsedAmount = EconomyUtil.parseStringToBigDecimal(rawNum);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("eco_invalid_amount"));
            return true;
        }

        if (parsedAmount.signum() < 0) {
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

    private List<UUID> resolveTargetUuids(String targetArg) {
        List<UUID> list = new ArrayList<>();
        if (targetArg.equals("*")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                list.add(player.getUniqueId());
            }
        } else if (targetArg.equals("**")) {
            for (UserAccount account : plugin.getEconomyManager().getAllAccounts()) {
                list.add(account.getUuid());
            }
        } else {
            UUID uuid = plugin.getEconomyManager().getUuidByName(targetArg);
            if (uuid != null) {
                list.add(uuid);
            }
        }
        return list;
    }

    private void handleGive(CommandSender sender, String targetArg, BigDecimal amount, boolean isPercent) {
        List<UUID> targets = resolveTargetUuids(targetArg);
        if (targets.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return;
        }

        for (UUID uuid : targets) {
            BigDecimal giveAmount = amount;
            if (isPercent) {
                BigDecimal bal = plugin.getEconomyManager().getBalance(uuid);
                giveAmount = bal.multiply(amount).scaleByPowerOfTen(-2);
            }

            try {
                plugin.getEconomyManager().deposit(uuid, giveAmount, UserBalanceUpdateEvent.Cause.COMMAND_ECO);
            } catch (MaxMoneyException e) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("eco_max_money_error"));
            }
        }

        String formatted = plugin.getEconomyManager().format(amount) + (isPercent ? "%" : "");
        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_give_success",
                "target", targetArg,
                "amount", formatted));
    }

    private void handleTake(CommandSender sender, String targetArg, BigDecimal amount, boolean isPercent) {
        List<UUID> targets = resolveTargetUuids(targetArg);
        if (targets.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return;
        }

        for (UUID uuid : targets) {
            BigDecimal takeAmount = amount;
            if (isPercent) {
                BigDecimal bal = plugin.getEconomyManager().getBalance(uuid);
                takeAmount = bal.multiply(amount).scaleByPowerOfTen(-2);
            }

            try {
                plugin.getEconomyManager().withdraw(uuid, takeAmount, true, UserBalanceUpdateEvent.Cause.COMMAND_ECO);
            } catch (NoLoanPermittedException e) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("eco_min_balance_error",
                        "amount", plugin.getEconomyManager().format(plugin.getEconomyManager().getMinBalance())));
            } catch (MaxMoneyException ignored) {}
        }

        String formatted = plugin.getEconomyManager().format(amount) + (isPercent ? "%" : "");
        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_take_success",
                "target", targetArg,
                "amount", formatted));
    }

    private void handleSet(CommandSender sender, String targetArg, BigDecimal amount) {
        List<UUID> targets = resolveTargetUuids(targetArg);
        if (targets.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return;
        }

        for (UUID uuid : targets) {
            try {
                plugin.getEconomyManager().setBalance(uuid, amount, UserBalanceUpdateEvent.Cause.COMMAND_ECO);
            } catch (NoLoanPermittedException e) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("eco_min_balance_error",
                        "amount", plugin.getEconomyManager().format(plugin.getEconomyManager().getMinBalance())));
            } catch (MaxMoneyException e) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("eco_max_money_error"));
            }
        }

        String formatted = plugin.getEconomyManager().format(amount);
        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_set_success",
                "target", targetArg,
                "amount", formatted));
    }

    private void handleReset(CommandSender sender, String targetArg) {
        List<UUID> targets = resolveTargetUuids(targetArg);
        if (targets.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return;
        }

        for (UUID uuid : targets) {
            try {
                plugin.getEconomyManager().resetBalance(uuid, UserBalanceUpdateEvent.Cause.COMMAND_ECO);
            } catch (Exception ignored) {}
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_reset_success",
                "target", targetArg));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.eco") && !sender.hasPermission("essentials.eco")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = Arrays.asList("give", "take", "set", "reset");
            List<String> matches = new ArrayList<>();
            String query = args[0].toLowerCase(Locale.ROOT);
            for (String s : subs) {
                if (s.startsWith(query)) matches.add(s);
            }
            return matches;
        }

        if (args.length == 2) {
            String query = args[1].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            if ("*".startsWith(query)) matches.add("*");
            if ("**".startsWith(query)) matches.add("**");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(query)) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }

        if (args.length == 3 && !args[0].equalsIgnoreCase("reset")) {
            return Arrays.asList("10", "100", "1k", "10k", "50%");
        }

        return Collections.emptyList();
    }
}
