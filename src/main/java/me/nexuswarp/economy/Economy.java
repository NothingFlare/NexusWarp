package me.nexuswarp.economy;

import me.nexuswarp.economy.event.UserBalanceUpdateEvent;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.UUID;

/**
 * Static Economy API matching Essentials Economy API.
 */
public final class Economy {

    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    private static EconomyManager manager;

    private Economy() {}

    public static void setManager(EconomyManager economyManager) {
        manager = economyManager;
    }

    public static EconomyManager getManager() {
        return manager;
    }

    private static void checkManager() {
        if (manager == null) {
            throw new IllegalStateException("Economy API is called before NexusWarp is loaded.");
        }
    }

    private static UUID requireUuid(String name) throws UserDoesNotExistException {
        checkManager();
        if (name == null || name.isEmpty()) {
            throw new UserDoesNotExistException("null");
        }
        UUID uuid = manager.getUuidByName(name);
        if (uuid == null || !manager.hasAccount(uuid)) {
            throw new UserDoesNotExistException(name);
        }
        return uuid;
    }

    private static UUID requireUuid(UUID uuid) throws UserDoesNotExistException {
        checkManager();
        if (uuid == null || !manager.hasAccount(uuid)) {
            throw new UserDoesNotExistException(uuid != null ? uuid.toString() : "null");
        }
        return uuid;
    }

    // -------------------------------------------------------------
    // Player Existence
    // -------------------------------------------------------------

    public static boolean playerExists(String name) {
        if (manager == null || name == null) return false;
        return manager.hasAccount(name);
    }

    public static boolean playerExists(UUID uuid) {
        if (manager == null || uuid == null) return false;
        return manager.hasAccount(uuid);
    }

    // -------------------------------------------------------------
    // Get Money
    // -------------------------------------------------------------

    public static double getMoney(String name) throws UserDoesNotExistException {
        return getMoneyExact(name).doubleValue();
    }

    public static double getMoney(UUID uuid) throws UserDoesNotExistException {
        return getMoneyExact(uuid).doubleValue();
    }

    public static BigDecimal getMoneyExact(String name) throws UserDoesNotExistException {
        UUID uuid = requireUuid(name);
        return manager.getBalance(uuid);
    }

    public static BigDecimal getMoneyExact(UUID uuid) throws UserDoesNotExistException {
        requireUuid(uuid);
        return manager.getBalance(uuid);
    }

    // -------------------------------------------------------------
    // Set Money
    // -------------------------------------------------------------

    public static void setMoney(String name, double balance)
            throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        setMoney(name, BigDecimal.valueOf(balance));
    }

    public static void setMoney(String name, BigDecimal balance)
            throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        UUID uuid = requireUuid(name);
        setMoney(uuid, balance);
    }

    public static void setMoney(UUID uuid, BigDecimal balance)
            throws NoLoanPermittedException, UserDoesNotExistException, MaxMoneyException {
        requireUuid(uuid);
        manager.setBalance(uuid, balance, UserBalanceUpdateEvent.Cause.API);
    }

    // -------------------------------------------------------------
    // Add Money
    // -------------------------------------------------------------

    public static void add(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        add(name, BigDecimal.valueOf(amount));
    }

    public static void add(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, MaxMoneyException {
        UUID uuid = requireUuid(name);
        add(uuid, amount);
    }

    public static void add(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, MaxMoneyException {
        requireUuid(uuid);
        manager.deposit(uuid, amount, UserBalanceUpdateEvent.Cause.API);
    }

    // -------------------------------------------------------------
    // Subtract Money
    // -------------------------------------------------------------

    public static void subtract(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        subtract(name, BigDecimal.valueOf(amount));
    }

    public static void subtract(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, MaxMoneyException {
        UUID uuid = requireUuid(name);
        subtract(uuid, amount);
    }

    public static void subtract(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, MaxMoneyException {
        requireUuid(uuid);
        manager.withdraw(uuid, amount, true, UserBalanceUpdateEvent.Cause.API);
    }

    // Essentials typo backward compatibility
    public static void substract(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        subtract(name, amount);
    }

    public static void substract(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, MaxMoneyException {
        subtract(name, amount);
    }

    public static void substract(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, MaxMoneyException {
        subtract(uuid, amount);
    }

    // -------------------------------------------------------------
    // Multiply & Divide
    // -------------------------------------------------------------

    public static void multiply(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        multiply(name, BigDecimal.valueOf(amount));
    }

    public static void multiply(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, MaxMoneyException {
        UUID uuid = requireUuid(name);
        multiply(uuid, amount);
    }

    public static void multiply(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, MaxMoneyException {
        requireUuid(uuid);
        manager.multiply(uuid, amount, UserBalanceUpdateEvent.Cause.API);
    }

    public static void divide(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        divide(name, BigDecimal.valueOf(amount));
    }

    public static void divide(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, MaxMoneyException {
        UUID uuid = requireUuid(name);
        divide(uuid, amount);
    }

    public static void divide(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, MaxMoneyException {
        requireUuid(uuid);
        manager.divide(uuid, amount, UserBalanceUpdateEvent.Cause.API);
    }

    // -------------------------------------------------------------
    // Reset Balance
    // -------------------------------------------------------------

    public static void resetBalance(String name)
            throws UserDoesNotExistException, NoLoanPermittedException, MaxMoneyException {
        UUID uuid = requireUuid(name);
        resetBalance(uuid);
    }

    public static void resetBalance(UUID uuid)
            throws NoLoanPermittedException, UserDoesNotExistException, MaxMoneyException {
        requireUuid(uuid);
        manager.resetBalance(uuid, UserBalanceUpdateEvent.Cause.API);
    }

    // -------------------------------------------------------------
    // Comparison Methods
    // -------------------------------------------------------------

    public static boolean hasEnough(String name, double amount) throws UserDoesNotExistException {
        return hasEnough(name, BigDecimal.valueOf(amount));
    }

    public static boolean hasEnough(String name, BigDecimal amount)
            throws UserDoesNotExistException, ArithmeticException {
        UUID uuid = requireUuid(name);
        return hasEnough(uuid, amount);
    }

    public static boolean hasEnough(UUID uuid, BigDecimal amount)
            throws ArithmeticException, UserDoesNotExistException {
        requireUuid(uuid);
        return manager.hasEnough(uuid, amount);
    }

    public static boolean hasMore(String name, double amount) throws UserDoesNotExistException {
        return hasMore(name, BigDecimal.valueOf(amount));
    }

    public static boolean hasMore(String name, BigDecimal amount)
            throws UserDoesNotExistException, ArithmeticException {
        UUID uuid = requireUuid(name);
        return hasMore(uuid, amount);
    }

    public static boolean hasMore(UUID uuid, BigDecimal amount)
            throws ArithmeticException, UserDoesNotExistException {
        requireUuid(uuid);
        return manager.getBalance(uuid).compareTo(amount) > 0;
    }

    public static boolean hasLess(String name, double amount) throws UserDoesNotExistException {
        return hasLess(name, BigDecimal.valueOf(amount));
    }

    public static boolean hasLess(String name, BigDecimal amount)
            throws UserDoesNotExistException, ArithmeticException {
        UUID uuid = requireUuid(name);
        return hasLess(uuid, amount);
    }

    public static boolean hasLess(UUID uuid, BigDecimal amount)
            throws ArithmeticException, UserDoesNotExistException {
        requireUuid(uuid);
        return manager.getBalance(uuid).compareTo(amount) < 0;
    }

    public static boolean isNegative(String name) throws UserDoesNotExistException {
        UUID uuid = requireUuid(name);
        return isNegative(uuid);
    }

    public static boolean isNegative(UUID uuid) throws UserDoesNotExistException {
        requireUuid(uuid);
        return manager.getBalance(uuid).signum() < 0;
    }

    // -------------------------------------------------------------
    // Formatting
    // -------------------------------------------------------------

    public static String format(double amount) {
        checkManager();
        return manager.format(amount);
    }

    public static String format(BigDecimal amount) {
        checkManager();
        return manager.format(amount);
    }

    // -------------------------------------------------------------
    // NPC Methods
    // -------------------------------------------------------------

    public static boolean isNPC(String name) throws UserDoesNotExistException {
        UUID uuid = requireUuid(name);
        return manager.isNPC(uuid);
    }

    public static boolean createNPC(String name) {
        checkManager();
        return manager.createNPC(name);
    }

    public static void removeNPC(String name) throws UserDoesNotExistException {
        checkManager();
        manager.removeNPC(name);
    }
}
