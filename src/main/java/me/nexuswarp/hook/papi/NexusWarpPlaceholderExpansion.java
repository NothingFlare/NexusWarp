package me.nexuswarp.hook.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.EconomyManager;
import me.nexuswarp.economy.UserAccount;
import me.nexuswarp.util.DateUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

public class NexusWarpPlaceholderExpansion extends PlaceholderExpansion {

    private final NexusWarpPlugin plugin;
    private final String identifier;
    private final Statistic playTimeStat;

    public NexusWarpPlaceholderExpansion(NexusWarpPlugin plugin, String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;

        Statistic stat;
        try {
            stat = Statistic.valueOf("PLAY_ONE_MINUTE");
        } catch (IllegalArgumentException e) {
            stat = Statistic.valueOf("PLAY_ONE_TICK");
        }
        this.playTimeStat = stat;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "NothingFlare";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @Nullable
    public String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase(Locale.ROOT);
        String lang = plugin.getLanguageManager().getCurrentLanguage();

        // 1. Server-wide placeholders (independent of player)
        switch (lower) {
            case "uptime":
                return DateUtil.formatDuration(ManagementFactory.getRuntimeMXBean().getUptime(), lang);
            case "tps":
            case "tps_1m":
                return String.format(Locale.US, "%.2f", Math.min(20.0, Bukkit.getTPS()[0]));
            case "tps_5m":
                return String.format(Locale.US, "%.2f", Math.min(20.0, Bukkit.getTPS()[1]));
            case "tps_15m":
                return String.format(Locale.US, "%.2f", Math.min(20.0, Bukkit.getTPS()[2]));
            case "total_money":
                return plugin.getEconomyManager().format(plugin.getEconomyManager().getTotalServerMoney());
            case "unique_joins":
                return String.valueOf(plugin.getEconomyManager().getAllAccounts().size());
            case "online":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max_players":
                return String.valueOf(Bukkit.getMaxPlayers());
        }

        // 2. Baltop rank placeholders (e.g. baltop_player_1, baltop_balance_1, baltop_balance_formatted_1)
        if (lower.startsWith("baltop_player_")) {
            int rank = parseRank(lower.substring("baltop_player_".length()));
            return getBaltopPlayer(rank);
        }
        if (lower.startsWith("baltop_balance_formatted_")) {
            int rank = parseRank(lower.substring("baltop_balance_formatted_".length()));
            return getBaltopBalanceFormatted(rank);
        }
        if (lower.startsWith("baltop_balance_")) {
            int rank = parseRank(lower.substring("baltop_balance_".length()));
            return getBaltopBalance(rank);
        }

        // 3. Player-specific placeholders
        if (player == null) {
            return "";
        }

        UserAccount account = plugin.getEconomyManager().getAccount(player.getUniqueId());

        switch (lower) {
            case "balance":
                return account != null ? account.getBalance().toPlainString() : "0.00";
            case "balance_formatted":
                return account != null ? plugin.getEconomyManager().format(account.getBalance()) : "$0.00";
            case "balance_fixed":
                return account != null ? account.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
            case "is_accepting_pay":
                return (account != null && account.isAcceptingPay()) ? "yes" : "no";
            case "is_confirm_pay":
                return (account != null && account.isPromptPayConfirm()) ? "yes" : "no";
            case "is_muted":
                return (account != null && account.isMuted()) ? "yes" : "no";
            case "muted":
                return String.valueOf(account != null && account.isMuted());
            case "mute_reason":
                return (account != null && account.getMuteReason() != null) ? account.getMuteReason() : "";
            case "mute_time_left":
                if (account == null || !account.isMuted()) return "0s";
                if (account.getMutedUntil() <= 0L) return "permanent";
                long leftMs = Math.max(0L, account.getMutedUntil() - System.currentTimeMillis());
                return DateUtil.formatDuration(leftMs, lang);
            case "fly":
            case "is_flying": {
                Player online = player.getPlayer();
                return (online != null && online.getAllowFlight()) ? "yes" : "no";
            }
            case "fly_status": {
                Player online = player.getPlayer();
                return (online != null && online.getAllowFlight()) ? "enabled" : "disabled";
            }
            case "seen":
                return account != null ? DateUtil.formatDateTime(account.getLastLogout(), lang) : "Never";
            case "seen_time":
                if (account == null || account.getLastLogout() <= 0L) return "never";
                long diff = Math.max(0L, System.currentTimeMillis() - account.getLastLogout());
                return DateUtil.formatDuration(diff, lang);
            case "playtime": {
                long millis = getPlaytimeMillis(player);
                return DateUtil.formatDuration(millis, lang);
            }
            case "playtime_hours": {
                long hours = getPlaytimeMillis(player) / 3600000L;
                return String.valueOf(hours);
            }
            case "playtime_minutes": {
                long minutes = getPlaytimeMillis(player) / 60000L;
                return String.valueOf(minutes);
            }
            case "playtime_seconds": {
                long seconds = getPlaytimeMillis(player) / 1000L;
                return String.valueOf(seconds);
            }
        }

        return null;
    }

    private long getPlaytimeMillis(OfflinePlayer player) {
        try {
            long ticks = player.getStatistic(playTimeStat);
            return ticks * 50L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private int parseRank(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String getBaltopPlayer(int rank) {
        if (rank < 1) return "";
        List<EconomyManager.BalanceTopEntry> baltop = plugin.getEconomyManager().getBaltop();
        int index = rank - 1;
        if (index < baltop.size()) {
            return baltop.get(index).getName();
        }
        return "---";
    }

    private String getBaltopBalance(int rank) {
        if (rank < 1) return "0.00";
        List<EconomyManager.BalanceTopEntry> baltop = plugin.getEconomyManager().getBaltop();
        int index = rank - 1;
        if (index < baltop.size()) {
            return baltop.get(index).getBalance().toPlainString();
        }
        return "0.00";
    }

    private String getBaltopBalanceFormatted(int rank) {
        if (rank < 1) return "$0.00";
        List<EconomyManager.BalanceTopEntry> baltop = plugin.getEconomyManager().getBaltop();
        int index = rank - 1;
        if (index < baltop.size()) {
            return plugin.getEconomyManager().format(baltop.get(index).getBalance());
        }
        return "$0.00";
    }
}
