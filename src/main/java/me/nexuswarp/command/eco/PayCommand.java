package me.nexuswarp.command.eco;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.EconomyManager;
import me.nexuswarp.economy.EconomyUtil;
import me.nexuswarp.economy.MaxMoneyException;
import me.nexuswarp.economy.NoLoanPermittedException;
import me.nexuswarp.economy.event.UserBalanceUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;

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

        if (!player.hasPermission("nexuswarp.pay") && !player.hasPermission("essentials.pay")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("usage_pay"));
            return true;
        }

        BigDecimal amount;
        try {
            amount = EconomyUtil.parseStringToBigDecimal(args[1]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_invalid_amount"));
            return true;
        }

        if (amount.signum() <= 0) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_must_be_positive"));
            return true;
        }

        BigDecimal minPay = plugin.getEconomyManager().getMinimumPayAmount();
        if (amount.compareTo(minPay) < 0) {
            String formattedMin = plugin.getEconomyManager().format(minPay);
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_minimum_pay_amount", "amount", formattedMin));
            return true;
        }

        String targetArg = args[0];

        // Multi-pay wildcard "*"
        if (targetArg.equals("*")) {
            if (!player.hasPermission("nexuswarp.pay.multiple") && !player.hasPermission("essentials.pay.multiple")) {
                player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }

            List<Player> recipients = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getUniqueId().equals(player.getUniqueId()) && plugin.getEconomyManager().isAcceptingPay(p.getUniqueId())) {
                    recipients.add(p);
                }
            }

            if (recipients.isEmpty()) {
                player.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
                return true;
            }

            BigDecimal totalCost = amount.multiply(BigDecimal.valueOf(recipients.size()));
            boolean hasLoan = player.hasPermission("nexuswarp.eco.loan") || player.hasPermission("essentials.eco.loan");
            if (!plugin.getEconomyManager().canAfford(player.getUniqueId(), totalCost, hasLoan)) {
                player.sendMessage(plugin.getLanguageManager().getMessage("eco_insufficient_funds"));
                return true;
            }

            for (Player recipient : recipients) {
                paySingle(player, recipient, amount, label, false);
            }
            return true;
        }

        // Single target
        UUID targetUuid = plugin.getEconomyManager().getUuidByName(targetArg);
        if (targetUuid == null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(targetArg);
            if (op.hasPlayedBefore() || op.isOnline()) {
                targetUuid = op.getUniqueId();
            }
        }

        if (targetUuid == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("target_not_online"));
            return true;
        }

        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_self"));
            return true;
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUuid);
        if (!targetPlayer.isOnline()) {
            boolean canPayOffline = player.hasPermission("nexuswarp.pay.offline") || player.hasPermission("essentials.pay.offline");
            if (!canPayOffline) {
                player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_offline_not_allowed"));
                return true;
            }
        }

        if (!plugin.getEconomyManager().isAcceptingPay(targetUuid)) {
            String targetName = plugin.getEconomyManager().getNameByUuid(targetUuid);
            player.sendMessage(plugin.getLanguageManager().getMessage("eco_not_accepting_pay", "player", targetName));
            return true;
        }

        // Pay confirmation check
        if (plugin.getEconomyManager().isPromptingPayConfirm(player.getUniqueId())) {
            BigDecimal pending = plugin.getEconomyManager().getPendingPayConfirm(player.getUniqueId(), targetUuid);
            if (pending == null || pending.compareTo(amount) != 0) {
                plugin.getEconomyManager().setPendingPayConfirm(player.getUniqueId(), targetUuid, amount);
                String cmd = "/" + label + " " + targetArg + " " + args[1];
                player.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_confirm_prompt",
                        "amount", plugin.getEconomyManager().format(amount),
                        "command", cmd));
                return true;
            }
            plugin.getEconomyManager().removePendingPayConfirm(player.getUniqueId(), targetUuid);
        }

        paySingle(player, targetPlayer, amount, label, true);
        return true;
    }

    private void paySingle(Player sender, OfflinePlayer target, BigDecimal amount, String label, boolean notifyErrors) {
        boolean hasLoan = sender.hasPermission("nexuswarp.eco.loan") || sender.hasPermission("essentials.eco.loan");
        if (!plugin.getEconomyManager().canAfford(sender.getUniqueId(), amount, hasLoan)) {
            if (notifyErrors) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("eco_insufficient_funds"));
            }
            return;
        }

        BigDecimal targetBalance = plugin.getEconomyManager().getBalance(target.getUniqueId());
        BigDecimal maxMoney = plugin.getEconomyManager().getMaxBalance();
        if (targetBalance.add(amount, EconomyManager.MATH_CONTEXT).compareTo(maxMoney) > 0) {
            if (notifyErrors) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("eco_max_money_error"));
            }
            return;
        }

        try {
            // Withdraw from sender
            plugin.getEconomyManager().withdraw(sender.getUniqueId(), amount, true, UserBalanceUpdateEvent.Cause.COMMAND_PAY);
        } catch (NoLoanPermittedException e) {
            if (notifyErrors) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("eco_insufficient_funds"));
            }
            return;
        } catch (MaxMoneyException ignored) {
            return;
        }

        try {
            // Deposit to target
            plugin.getEconomyManager().deposit(target.getUniqueId(), amount, UserBalanceUpdateEvent.Cause.COMMAND_PAY);
        } catch (MaxMoneyException e) {
            // Refund sender in case deposit fails
            try {
                plugin.getEconomyManager().deposit(sender.getUniqueId(), amount, UserBalanceUpdateEvent.Cause.COMMAND_PAY);
            } catch (MaxMoneyException ignored) {}
            if (notifyErrors) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("eco_max_money_error"));
            }
            return;
        }

        String formattedAmount = plugin.getEconomyManager().format(amount);
        String targetName = plugin.getEconomyManager().getNameByUuid(target.getUniqueId());

        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_pay_sent",
                "player", targetName,
                "amount", formattedAmount));

        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("eco_pay_received",
                    "player", sender.getName(),
                    "amount", formattedAmount));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String query = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player player && p.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(query)) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }

        if (args.length == 2) {
            return Arrays.asList("10", "100", "500", "1k", "10k");
        }

        return Collections.emptyList();
    }
}
