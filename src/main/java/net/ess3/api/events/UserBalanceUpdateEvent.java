package net.ess3.api.events;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public class UserBalanceUpdateEvent extends me.nexuswarp.economy.event.UserBalanceUpdateEvent {

    public UserBalanceUpdateEvent(OfflinePlayer player, BigDecimal originalBalance, BigDecimal balance, Cause cause) {
        super(player, originalBalance, balance, cause);
    }

    public UserBalanceUpdateEvent(Player player, BigDecimal originalBalance, BigDecimal balance, Cause cause) {
        super(player, originalBalance, balance, cause);
    }
}
