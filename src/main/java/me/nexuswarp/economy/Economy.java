package me.nexuswarp.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Static Economy API matching the design of Essentials Economy.
 */
public final class Economy {

    private static EconomyManager manager;

    private Economy() {}

    public static void setManager(EconomyManager economyManager) {
        manager = economyManager;
    }

    public static EconomyManager getManager() {
        return manager;
    }

    public static boolean playerExists(UUID uuid) {
        return manager != null && manager.hasAccount(uuid);
    }

    public static boolean playerExists(String name) {
        return manager != null && manager.hasAccount(name);
    }

    public static BigDecimal getMoneyExact(UUID uuid) throws UserDoesNotExistException {
        if (manager == null) return BigDecimal.ZERO;
        if (!manager.hasAccount(uuid)) {
            throw new UserDoesNotExistException(uuid.toString());
        }
        return manager.getBalance(uuid);
    }

    public static BigDecimal getMoneyExact(String name) throws UserDoesNotExistException {
        if (manager == null) return BigDecimal.ZERO;
        if (!manager.hasAccount(name)) {
            throw new UserDoesNotExistException(name);
        }
        return manager.getBalance(name);
    }

    public static double getMoney(UUID uuid) throws UserDoesNotExistException {
        return getMoneyExact(uuid).doubleValue();
    }

    public static double getMoney(String name) throws UserDoesNotExistException {
        return getMoneyExact(name).doubleValue();
    }

    public static boolean hasEnough(UUID uuid, BigDecimal amount) throws UserDoesNotExistException {
        if (manager == null) return false;
        if (!manager.hasAccount(uuid)) {
            throw new UserDoesNotExistException(uuid.toString());
        }
        return manager.hasEnough(uuid, amount);
    }

    public static boolean hasEnough(String name, BigDecimal amount) throws UserDoesNotExistException {
        if (manager == null) return false;
        if (!manager.hasAccount(name)) {
            throw new UserDoesNotExistException(name);
        }
        UUID uuid = manager.getUuidByName(name);
        return uuid != null && manager.hasEnough(uuid, amount);
    }

    public static boolean hasEnough(String name, double amount) throws UserDoesNotExistException {
        return hasEnough(name, BigDecimal.valueOf(amount));
    }

    public static void setMoney(UUID uuid, BigDecimal amount) throws NoLoanPermittedException, MaxMoneyException {
        if (manager != null) {
            manager.setBalance(uuid, amount);
        }
    }

    public static void setMoney(String name, BigDecimal amount) throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        if (manager == null) return;
        UUID uuid = manager.getUuidByName(name);
        if (uuid == null) throw new UserDoesNotExistException(name);
        manager.setBalance(uuid, amount);
    }

    public static void add(UUID uuid, BigDecimal amount) throws MaxMoneyException {
        if (manager != null) {
            manager.deposit(uuid, amount);
        }
    }

    public static void add(String name, BigDecimal amount) throws UserDoesNotExistException, MaxMoneyException {
        if (manager == null) return;
        UUID uuid = manager.getUuidByName(name);
        if (uuid == null) throw new UserDoesNotExistException(name);
        manager.deposit(uuid, amount);
    }

    public static void add(String name, double amount) throws UserDoesNotExistException, MaxMoneyException {
        add(name, BigDecimal.valueOf(amount));
    }

    public static void subtract(UUID uuid, BigDecimal amount) throws NoLoanPermittedException, MaxMoneyException {
        if (manager != null) {
            manager.withdraw(uuid, amount);
        }
    }

    public static void subtract(String name, BigDecimal amount) throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        if (manager == null) return;
        UUID uuid = manager.getUuidByName(name);
        if (uuid == null) throw new UserDoesNotExistException(name);
        manager.withdraw(uuid, amount);
    }

    public static void subtract(String name, double amount) throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        subtract(name, BigDecimal.valueOf(amount));
    }

    public static String format(BigDecimal amount) {
        if (manager == null) return amount.toPlainString();
        return manager.format(amount);
    }

    public static String format(double amount) {
        if (manager == null) return String.valueOf(amount);
        return manager.format(amount);
    }
}
