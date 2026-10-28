package me.nexuswarp.economy;

import me.nexuswarp.NexusWarpPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class EconomyManager {

    private final NexusWarpPlugin plugin;
    private Economy vaultEconomy;
    private BigDecimal minimumPayAmount = new BigDecimal("0.01");

    public EconomyManager(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadSettings() {
        this.minimumPayAmount = new BigDecimal(plugin.getConfig().getString("economy.minimum-pay-amount", "0.01"));
        hookVault();
    }

    public boolean hookVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            this.vaultEconomy = null;
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            this.vaultEconomy = rsp.getProvider();
            return this.vaultEconomy != null;
        }
        this.vaultEconomy = null;
        return false;
    }

    public boolean isAvailable() {
        if (vaultEconomy == null) {
            hookVault();
        }
        return vaultEconomy != null && vaultEconomy.isEnabled();
    }

    public Economy getVault() {
        if (vaultEconomy == null) {
            hookVault();
        }
        return vaultEconomy;
    }

    public boolean hasAccount(UUID uuid) {
        if (!isAvailable()) return false;
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return getVault().hasAccount(op);
    }

    public boolean hasAccount(String name) {
        if (!isAvailable()) return false;
        return getVault().hasAccount(name);
    }

    public double getBalance(OfflinePlayer player) {
        if (!isAvailable() || player == null) return 0.0;
        return getVault().getBalance(player);
    }

    public BigDecimal getBalance(UUID uuid) {
        if (!isAvailable()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(getBalance(Bukkit.getOfflinePlayer(uuid)));
    }

    public BigDecimal getBalance(String name) {
        if (!isAvailable()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(getVault().getBalance(name));
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!isAvailable() || player == null) return false;
        return getVault().has(player, amount);
    }

    public boolean hasEnough(UUID uuid, BigDecimal amount) {
        if (!isAvailable() || amount == null) return false;
        return has(Bukkit.getOfflinePlayer(uuid), amount.doubleValue());
    }

    public EconomyResponse withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable() || player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Economy unavailable");
        }
        return getVault().withdrawPlayer(player, amount);
    }

    public void withdraw(UUID uuid, BigDecimal amount) throws NoLoanPermittedException, MaxMoneyException {
        if (!isAvailable()) return;
        EconomyResponse resp = withdraw(Bukkit.getOfflinePlayer(uuid), amount.doubleValue());
        if (!resp.transactionSuccess()) {
            throw new NoLoanPermittedException();
        }
    }

    public EconomyResponse deposit(OfflinePlayer player, double amount) {
        if (!isAvailable() || player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Economy unavailable");
        }
        return getVault().depositPlayer(player, amount);
    }

    public void deposit(UUID uuid, BigDecimal amount) throws MaxMoneyException {
        if (!isAvailable()) return;
        EconomyResponse resp = deposit(Bukkit.getOfflinePlayer(uuid), amount.doubleValue());
        if (!resp.transactionSuccess()) {
            throw new MaxMoneyException();
        }
    }

    public EconomyResponse setBalance(OfflinePlayer player, double targetBalance) {
        if (!isAvailable() || player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Economy unavailable");
        }
        double current = getBalance(player);
        if (targetBalance > current) {
            return deposit(player, targetBalance - current);
        } else if (targetBalance < current) {
            return withdraw(player, current - targetBalance);
        }
        return new EconomyResponse(0, current, EconomyResponse.ResponseType.SUCCESS, null);
    }

    public void setBalance(UUID uuid, BigDecimal amount) throws NoLoanPermittedException, MaxMoneyException {
        if (!isAvailable()) return;
        setBalance(Bukkit.getOfflinePlayer(uuid), amount.doubleValue());
    }

    public EconomyResponse resetBalance(OfflinePlayer player) {
        if (!isAvailable() || player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Economy unavailable");
        }
        double current = getBalance(player);
        if (current > 0) {
            return withdraw(player, current);
        }
        return new EconomyResponse(0, current, EconomyResponse.ResponseType.SUCCESS, null);
    }

    public String format(double amount) {
        if (isAvailable()) {
            return getVault().format(amount);
        }
        return String.format("%.2f", amount);
    }

    public String format(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        return format(amount.doubleValue());
    }

    public BigDecimal getMinimumPayAmount() {
        return minimumPayAmount;
    }

    public UUID getUuidByName(String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.hasPlayedBefore() || op.isOnline()) return op.getUniqueId();
        return null;
    }

    public String getNameByUuid(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (op.getName() != null) return op.getName();
        return uuid.toString().substring(0, 8);
    }

    public static class BalanceTopEntry {
        private final String name;
        private final double balance;

        public BalanceTopEntry(String name, double balance) {
            this.name = name;
            this.balance = balance;
        }

        public String getName() {
            return name;
        }

        public double getBalance() {
            return balance;
        }
    }

    public List<BalanceTopEntry> getBaltopOnline() {
        List<BalanceTopEntry> list = new ArrayList<>();
        if (!isAvailable()) return list;

        for (Player p : Bukkit.getOnlinePlayers()) {
            list.add(new BalanceTopEntry(p.getName(), getBalance(p)));
        }
        list.sort(Comparator.comparingDouble(BalanceTopEntry::getBalance).reversed());
        return list;
    }
}
