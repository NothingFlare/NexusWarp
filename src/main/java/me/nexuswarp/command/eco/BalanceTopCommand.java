package me.nexuswarp.command.eco;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BalanceTopCommand implements CommandExecutor, TabCompleter {

    private static final int PAGE_SIZE = 10;
    private final NexusWarpPlugin plugin;

    public BalanceTopCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.balancetop")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (!plugin.getEconomyManager().isAvailable()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("eco_vault_not_found"));
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException ignored) {}
        }

        List<EconomyManager.BalanceTopEntry> baltop = plugin.getEconomyManager().getBaltopOnline();
        int totalAccounts = baltop.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) totalAccounts / PAGE_SIZE));

        if (page > maxPages) {
            page = maxPages;
        }

        double serverTotal = 0;
        for (EconomyManager.BalanceTopEntry e : baltop) {
            serverTotal += e.getBalance();
        }
        String formattedTotal = plugin.getEconomyManager().format(serverTotal);

        sender.sendMessage(plugin.getLanguageManager().getMessage("eco_baltop_header",
                "page", String.valueOf(page),
                "max_pages", String.valueOf(maxPages),
                "total", formattedTotal));

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalAccounts);

        for (int i = startIndex; i < endIndex; i++) {
            EconomyManager.BalanceTopEntry entry = baltop.get(i);
            int rank = i + 1;
            String formattedBalance = plugin.getEconomyManager().format(entry.getBalance());

            sender.sendMessage(plugin.getLanguageManager().getMessage("eco_baltop_entry",
                "rank", String.valueOf(rank),
                "player", entry.getName(),
                "amount", formattedBalance));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("1", "2", "3");
        }
        return Collections.emptyList();
    }
}
