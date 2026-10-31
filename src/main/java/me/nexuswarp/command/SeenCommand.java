package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.UserAccount;
import me.nexuswarp.util.DateUtil;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SeenCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public SeenCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.seen") && !sender.hasPermission("essentials.seen")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("usage_seen"));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getPlayer(targetName);

        if (target == null) {
            UUID uuid = plugin.getEconomyManager().getUuidByName(targetName);
            if (uuid != null) {
                target = Bukkit.getOfflinePlayer(uuid);
            } else {
                target = Bukkit.getOfflinePlayer(targetName);
            }
        }

        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("seen_never_online", "player", targetName));
            return true;
        }

        String lang = plugin.getLanguageManager().getCurrentLanguage();
        String resolvedName = target.getName() != null ? target.getName() : targetName;
        UserAccount account = plugin.getEconomyManager().getAccount(target.getUniqueId());

        boolean showIp = sender.hasPermission("nexuswarp.seen.ip") || sender.hasPermission("essentials.seen.ip");
        boolean showLocation = sender.hasPermission("nexuswarp.seen.location") || sender.hasPermission("essentials.seen.location");
        boolean showUuid = sender.hasPermission("nexuswarp.seen.uuid") || sender.hasPermission("essentials.seen.uuid");
        boolean showFirstLogin = sender.hasPermission("nexuswarp.seen.firstlogin") || sender.hasPermission("essentials.seen.firstlogin");
        boolean showBan = sender.hasPermission("nexuswarp.seen.banreason") || sender.hasPermission("essentials.seen.banreason");

        if (target.isOnline() && target.getPlayer() != null) {
            Player onlinePlayer = target.getPlayer();
            long loginTime = account != null && account.getLastLogin() > 0 ? account.getLastLogin() : System.currentTimeMillis();
            String timeDiff = DateUtil.formatDateDiff(loginTime, lang);

            sender.sendMessage(plugin.getLanguageManager().getMessage("seen_online",
                    "player", resolvedName,
                    "time", timeDiff));

            if (showUuid) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("seen_uuid",
                        "uuid", onlinePlayer.getUniqueId().toString()));
            }

            if (showFirstLogin) {
                String firstDate = DateUtil.formatDateTime(onlinePlayer.getFirstPlayed(), lang);
                sender.sendMessage(plugin.getLanguageManager().getMessage("seen_first_login",
                        "time", firstDate));
            }

            if (showLocation) {
                Location loc = onlinePlayer.getLocation();
                String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
                sender.sendMessage(plugin.getLanguageManager().getMessage("seen_location",
                        "world", worldName,
                        "x", String.valueOf(loc.getBlockX()),
                        "y", String.valueOf(loc.getBlockY()),
                        "z", String.valueOf(loc.getBlockZ())));
            }

            if (showIp && onlinePlayer.getAddress() != null && onlinePlayer.getAddress().getAddress() != null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("seen_ip",
                        "ip", onlinePlayer.getAddress().getAddress().getHostAddress()));
            }

        } else {
            // Offline player
            long logoutTime = account != null && account.getLastLogout() > 0 ? account.getLastLogout() : target.getLastPlayed();
            String timeDiff = DateUtil.formatDateDiff(logoutTime, lang);

            sender.sendMessage(plugin.getLanguageManager().getMessage("seen_offline",
                    "player", resolvedName,
                    "time", timeDiff));

            // Check bans
            BanEntry banEntry = null;
            try {
                banEntry = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(resolvedName);
            } catch (Throwable ignored) {}

            if (banEntry != null) {
                String reason = (showBan && banEntry.getReason() != null) ? banEntry.getReason() : "Banned";
                sender.sendMessage(plugin.getLanguageManager().getMessage("seen_banned", "reason", reason));
                if (banEntry.getExpiration() != null) {
                    long expiry = banEntry.getExpiration().getTime();
                    if (expiry > System.currentTimeMillis()) {
                        String expireDiff = DateUtil.formatDateDiff(expiry, lang);
                        sender.sendMessage(plugin.getLanguageManager().getMessage("seen_temp_banned", "time", expireDiff));
                    }
                }
            }

            if (showUuid) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("seen_uuid",
                        "uuid", target.getUniqueId().toString()));
            }

            if (showFirstLogin) {
                String firstDate = DateUtil.formatDateTime(target.getFirstPlayed(), lang);
                sender.sendMessage(plugin.getLanguageManager().getMessage("seen_first_login",
                        "time", firstDate));
            }

            if (showLocation && account != null && account.getLastLocation() != null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("seen_location_raw",
                        "location", account.getLastLocation()));
            }

            if (showIp && account != null && account.getLastIp() != null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("seen_ip",
                        "ip", account.getLastIp()));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.seen") && !sender.hasPermission("essentials.seen")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String query = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(query)) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }

        return Collections.emptyList();
    }
}
