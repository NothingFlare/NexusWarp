package me.nexuswarp.economy.event;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.math.BigDecimal;

/**
 * Fired when a user's economy balance is updated.
 * Matches Essentials UserBalanceUpdateEvent.
 */
public class UserBalanceUpdateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum Cause {
        COMMAND_ECO,
        COMMAND_PAY,
        COMMAND_SELL,
        API,
        SPECIAL,
        UNKNOWN
    }

    private final OfflinePlayer player;
    private final BigDecimal oldBalance;
    private BigDecimal newBalance;
    private final Cause cause;
    private boolean cancelled = false;

    public UserBalanceUpdateEvent(OfflinePlayer player, BigDecimal oldBalance, BigDecimal newBalance, Cause cause) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.player = player;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.cause = cause != null ? cause : Cause.UNKNOWN;
    }

    public OfflinePlayer getOfflinePlayer() {
        return player;
    }

    public Player getPlayer() {
        return player != null ? player.getPlayer() : null;
    }

    public BigDecimal getOldBalance() {
        return oldBalance;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        if (newBalance == null) {
            throw new IllegalArgumentException("newBalance cannot be null");
        }
        this.newBalance = newBalance;
    }

    public Cause getCause() {
        return cause;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
