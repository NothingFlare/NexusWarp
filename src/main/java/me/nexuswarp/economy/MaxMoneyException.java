package me.nexuswarp.economy;

public class MaxMoneyException extends Exception {
    public MaxMoneyException() {
        super("Transaction exceeds maximum allowed balance limit.");
    }
}
