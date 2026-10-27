package me.nexuswarp.economy.vault;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.Economy;
import me.nexuswarp.economy.MaxMoneyException;
import me.nexuswarp.economy.NoLoanPermittedException;
import me.nexuswarp.economy.UserDoesNotExistException;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class VaultEconomyProvider extends AbstractEconomy {

    private final NexusWarpPlugin plugin;

    public VaultEconomyProvider(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "NexusWarp Economy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return plugin.getEconomyManager().format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getEconomyManager().getCurrencySymbol();
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getEconomyManager().getCurrencySymbol();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return plugin.getEconomyManager().hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null && plugin.getEconomyManager().hasAccount(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        return plugin.getEconomyManager().getBalance(playerName).doubleValue();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null) return 0.0;
        return plugin.getEconomyManager().getBalance(player.getUniqueId()).doubleValue();
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        try {
            return Economy.hasEnough(playerName, amount);
        } catch (UserDoesNotExistException e) {
            return false;
        }
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (player == null) return false;
        return plugin.getEconomyManager().hasEnough(player.getUniqueId(), BigDecimal.valueOf(amount));
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        if (playerName == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player name cannot be null!");
        }
        if (amount < 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds!");
        }

        try {
            Economy.subtract(playerName, amount);
            return new EconomyResponse(amount, getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, null);
        } catch (UserDoesNotExistException e) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "User does not exist!");
        } catch (NoLoanPermittedException e) {
            return new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.FAILURE, "Loan was not permitted!");
        } catch (MaxMoneyException e) {
            return new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.FAILURE, "Exceeds maximum limit!");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player cannot be null!");
        }
        if (amount < 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds!");
        }

        try {
            plugin.getEconomyManager().withdraw(player.getUniqueId(), BigDecimal.valueOf(amount));
            return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        } catch (NoLoanPermittedException e) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Loan was not permitted!");
        } catch (MaxMoneyException e) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Exceeds maximum limit!");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        if (playerName == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player name cannot be null!");
        }
        if (amount < 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds!");
        }

        try {
            Economy.add(playerName, amount);
            return new EconomyResponse(amount, getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, null);
        } catch (UserDoesNotExistException e) {
            // Auto create account
            createPlayerAccount(playerName);
            try {
                Economy.add(playerName, amount);
                return new EconomyResponse(amount, getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, null);
            } catch (Exception ex) {
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, ex.getMessage());
            }
        } catch (MaxMoneyException e) {
            return new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.FAILURE, "Exceeds maximum balance!");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player cannot be null!");
        }
        if (amount < 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds!");
        }

        try {
            plugin.getEconomyManager().deposit(player.getUniqueId(), BigDecimal.valueOf(amount));
            return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        } catch (MaxMoneyException e) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Exceeds maximum balance!");
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        plugin.getEconomyManager().createAccount(plugin.getEconomyManager().getUuidByName(playerName), playerName);
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (player == null) return false;
        plugin.getEconomyManager().createAccount(player.getUniqueId(), player.getName());
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    // Bank methods (not supported, standard Vault stubs)
    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }
}
