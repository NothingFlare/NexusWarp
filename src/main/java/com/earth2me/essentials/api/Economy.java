package com.earth2me.essentials.api;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.UUID;

/**
 * Compatibility bridge for plugins directly calling Essentials Economy API.
 */
public class Economy {

    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    protected Economy() {}

    public static boolean playerExists(String name) {
        return me.nexuswarp.economy.Economy.playerExists(name);
    }

    public static boolean playerExists(UUID uuid) {
        return me.nexuswarp.economy.Economy.playerExists(uuid);
    }

    public static double getMoney(String name) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.getMoney(name);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static double getMoney(UUID uuid) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.getMoney(uuid);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        }
    }

    public static BigDecimal getMoneyExact(String name) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.getMoneyExact(name);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static BigDecimal getMoneyExact(UUID uuid) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.getMoneyExact(uuid);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        }
    }

    public static void setMoney(String name, double balance)
            throws UserDoesNotExistException, NoLoanPermittedException, net.ess3.api.MaxMoneyException {
        setMoney(name, BigDecimal.valueOf(balance));
    }

    public static void setMoney(String name, BigDecimal balance)
            throws UserDoesNotExistException, NoLoanPermittedException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.setMoney(name, balance);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void setMoney(UUID uuid, BigDecimal balance)
            throws NoLoanPermittedException, UserDoesNotExistException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.setMoney(uuid, balance);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void add(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, net.ess3.api.MaxMoneyException {
        add(name, BigDecimal.valueOf(amount));
    }

    public static void add(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.add(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void add(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.add(uuid, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void subtract(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, net.ess3.api.MaxMoneyException {
        subtract(name, BigDecimal.valueOf(amount));
    }

    public static void subtract(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.subtract(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void subtract(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.subtract(uuid, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void substract(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, net.ess3.api.MaxMoneyException {
        subtract(name, amount);
    }

    public static void substract(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, net.ess3.api.MaxMoneyException {
        subtract(name, amount);
    }

    public static void substract(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, net.ess3.api.MaxMoneyException {
        subtract(uuid, amount);
    }

    public static void multiply(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, net.ess3.api.MaxMoneyException {
        multiply(name, BigDecimal.valueOf(amount));
    }

    public static void multiply(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.multiply(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void multiply(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.multiply(uuid, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void divide(String name, double amount)
            throws UserDoesNotExistException, NoLoanPermittedException, net.ess3.api.MaxMoneyException {
        divide(name, BigDecimal.valueOf(amount));
    }

    public static void divide(String name, BigDecimal amount)
            throws UserDoesNotExistException, NoLoanPermittedException, ArithmeticException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.divide(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void divide(UUID uuid, BigDecimal amount)
            throws NoLoanPermittedException, ArithmeticException, UserDoesNotExistException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.divide(uuid, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void resetBalance(String name)
            throws UserDoesNotExistException, NoLoanPermittedException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.resetBalance(name);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static void resetBalance(UUID uuid)
            throws NoLoanPermittedException, UserDoesNotExistException, net.ess3.api.MaxMoneyException {
        try {
            me.nexuswarp.economy.Economy.resetBalance(uuid);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        } catch (me.nexuswarp.economy.NoLoanPermittedException e) {
            throw new NoLoanPermittedException();
        } catch (me.nexuswarp.economy.MaxMoneyException e) {
            throw new net.ess3.api.MaxMoneyException();
        }
    }

    public static boolean hasEnough(String name, double amount) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.hasEnough(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static boolean hasEnough(String name, BigDecimal amount)
            throws UserDoesNotExistException, ArithmeticException {
        try {
            return me.nexuswarp.economy.Economy.hasEnough(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static boolean hasEnough(UUID uuid, BigDecimal amount)
            throws ArithmeticException, UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.hasEnough(uuid, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        }
    }

    public static boolean hasMore(String name, double amount) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.hasMore(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static boolean hasMore(String name, BigDecimal amount)
            throws UserDoesNotExistException, ArithmeticException {
        try {
            return me.nexuswarp.economy.Economy.hasMore(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static boolean hasMore(UUID uuid, BigDecimal amount)
            throws ArithmeticException, UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.hasMore(uuid, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        }
    }

    public static boolean hasLess(String name, double amount) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.hasLess(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static boolean hasLess(String name, BigDecimal amount)
            throws UserDoesNotExistException, ArithmeticException {
        try {
            return me.nexuswarp.economy.Economy.hasLess(name, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static boolean hasLess(UUID uuid, BigDecimal amount)
            throws ArithmeticException, UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.hasLess(uuid, amount);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        }
    }

    public static boolean isNegative(String name) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.isNegative(name);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static boolean isNegative(UUID uuid) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.isNegative(uuid);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(uuid);
        }
    }

    public static String format(double amount) {
        return me.nexuswarp.economy.Economy.format(amount);
    }

    public static String format(BigDecimal amount) {
        return me.nexuswarp.economy.Economy.format(amount);
    }

    public static boolean isNPC(String name) throws UserDoesNotExistException {
        try {
            return me.nexuswarp.economy.Economy.isNPC(name);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }

    public static boolean createNPC(String name) {
        return me.nexuswarp.economy.Economy.createNPC(name);
    }

    public static void removeNPC(String name) throws UserDoesNotExistException {
        try {
            me.nexuswarp.economy.Economy.removeNPC(name);
        } catch (me.nexuswarp.economy.UserDoesNotExistException e) {
            throw new UserDoesNotExistException(name);
        }
    }
}
