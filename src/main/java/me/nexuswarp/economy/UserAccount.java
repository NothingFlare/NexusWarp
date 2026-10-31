package me.nexuswarp.economy;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a cached user account (economy, session, and player info).
 */
public class UserAccount {

    private final UUID uuid;
    private volatile String lastAccountName;
    private volatile BigDecimal balance;
    private volatile boolean npc;
    private volatile String npcName;
    private volatile boolean acceptingPay = true;
    private volatile boolean promptPayConfirm = false;

    // Session and history tracking for /seen
    private volatile long lastLogin = 0L;
    private volatile long lastLogout = 0L;
    private volatile String lastIp = null;
    private volatile String lastLocation = null;

    private volatile boolean dirty = false;

    public UserAccount(UUID uuid, String lastAccountName, BigDecimal balance) {
        this.uuid = Objects.requireNonNull(uuid, "uuid cannot be null");
        this.lastAccountName = lastAccountName != null ? lastAccountName : uuid.toString().substring(0, 8);
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastAccountName() {
        return lastAccountName;
    }

    public void setLastAccountName(String lastAccountName) {
        this.lastAccountName = lastAccountName;
        this.dirty = true;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = Objects.requireNonNull(balance, "balance cannot be null");
        this.dirty = true;
    }

    public boolean isNpc() {
        return npc;
    }

    public void setNpc(boolean npc) {
        this.npc = npc;
        this.dirty = true;
    }

    public String getNpcName() {
        return npcName;
    }

    public void setNpcName(String npcName) {
        this.npcName = npcName;
        this.dirty = true;
    }

    public boolean isAcceptingPay() {
        return acceptingPay;
    }

    public void setAcceptingPay(boolean acceptingPay) {
        this.acceptingPay = acceptingPay;
        this.dirty = true;
    }

    public boolean isPromptPayConfirm() {
        return promptPayConfirm;
    }

    public void setPromptPayConfirm(boolean promptPayConfirm) {
        this.promptPayConfirm = promptPayConfirm;
        this.dirty = true;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
        this.dirty = true;
    }

    public long getLastLogout() {
        return lastLogout;
    }

    public void setLastLogout(long lastLogout) {
        this.lastLogout = lastLogout;
        this.dirty = true;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
        this.dirty = true;
    }

    public String getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(String lastLocation) {
        this.lastLocation = lastLocation;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
