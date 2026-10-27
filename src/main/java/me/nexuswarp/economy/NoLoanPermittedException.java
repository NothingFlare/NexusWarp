package me.nexuswarp.economy;

public class NoLoanPermittedException extends Exception {
    public NoLoanPermittedException() {
        super("Negative balances / loans are not permitted.");
    }
}
