package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.UserAccount;
import me.nexuswarp.util.DateUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public MuteCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isUnmute = label.equalsIgnoreCase("unmute");

        if (!sender.hasPermission("nexuswarp.mute") && !sender.hasPermission("essentials.mute")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            String usageKey = isUnmute ? "usage_unmute" : "usage_mute";
            sender.sendMessage(plugin.getLanguageManager().getMessage(usageKey));
            return true;
        }

        String targetName = args[0];
        Player onlineTarget = Bukkit.getPlayer(targetName);
        UUID targetUuid;
        String resolvedName;

        if (onlineTarget != null) {
            targetUuid = onlineTarget.getUniqueId();
            resolvedName = onlineTarget.getName();
        } else {
            UUID cachedUuid = plugin.getEconomyManager().getUuidByName(targetName);
            if (cachedUuid != null) {
                targetUuid = cachedUuid;
                resolvedName = targetName;
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                targetUuid = offline.getUniqueId();
                resolvedName = offline.getName() != null ? offline.getName() : targetName;
            }
        }

        UserAccount account = plugin.getEconomyManager().getAccount(targetUuid);
        if (account == null) {
            account = plugin.getEconomyManager().createAccount(targetUuid, resolvedName);
        }

        // Handle /unmute
        if (isUnmute) {
            if (!account.isMuted()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("mute_not_muted", "player", resolvedName));
                return true;
            }
            unmutePlayer(sender, resolvedName, account, onlineTarget);
            return true;
        }

        // Handle /mute <player> when already muted and no additional args -> toggle unmute
        if (account.isMuted() && args.length == 1) {
            unmutePlayer(sender, resolvedName, account, onlineTarget);
            return true;
        }

        // Exemption check
        if (onlineTarget != null && (onlineTarget.hasPermission("nexuswarp.mute.exempt") || onlineTarget.hasPermission("essentials.mute.exempt"))
                && sender instanceof Player) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("mute_exempt", "player", resolvedName));
            return true;
        }

        // Parse optional time and reason
        long mutedUntil = 0L;
        String reason = null;
        int reasonStartIndex = 1;

        if (args.length >= 2) {
            try {
                mutedUntil = DateUtil.parseDateDiff(args[1], true);
                reasonStartIndex = 2;
            } catch (IllegalArgumentException ignored) {
                // Not a time string, treat as start of reason
                reasonStartIndex = 1;
            }
        }

        if (args.length > reasonStartIndex) {
            StringBuilder sb = new StringBuilder();
            for (int i = reasonStartIndex; i < args.length; i++) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(args[i]);
            }
            reason = sb.toString();
        }

        account.setMuted(true);
        account.setMutedUntil(mutedUntil);
        account.setMuteReason(reason);
        plugin.getEconomyManager().saveAccountAsync(account);

        String lang = plugin.getLanguageManager().getCurrentLanguage();
        String reasonDisplay = reason != null ? reason : plugin.getLanguageManager().getMessage("mute_no_reason");

        if (mutedUntil > 0L) {
            long remainingMs = Math.max(0L, mutedUntil - System.currentTimeMillis());
            String durationStr = DateUtil.formatDuration(remainingMs, lang);

            sender.sendMessage(plugin.getLanguageManager().getMessage("mute_success_temp",
                    "player", resolvedName,
                    "time", durationStr,
                    "reason", reasonDisplay));

            if (onlineTarget != null) {
                onlineTarget.sendMessage(plugin.getLanguageManager().getMessage("mute_notify_temp",
                        "sender", sender.getName(),
                        "time", durationStr,
                        "reason", reasonDisplay));
            }
        } else {
            sender.sendMessage(plugin.getLanguageManager().getMessage("mute_success_permanent",
                    "player", resolvedName,
                    "reason", reasonDisplay));

            if (onlineTarget != null) {
                onlineTarget.sendMessage(plugin.getLanguageManager().getMessage("mute_notify_permanent",
                        "sender", sender.getName(),
                        "reason", reasonDisplay));
            }
        }

        return true;
    }

    private void unmutePlayer(CommandSender sender, String playerName, UserAccount account, Player onlineTarget) {
        account.setMuted(false);
        account.setMutedUntil(0L);
        account.setMuteReason(null);
        plugin.getEconomyManager().saveAccountAsync(account);

        sender.sendMessage(plugin.getLanguageManager().getMessage("unmute_success", "player", playerName));
        if (onlineTarget != null) {
            onlineTarget.sendMessage(plugin.getLanguageManager().getMessage("unmute_notify", "sender", sender.getName()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.mute") && !sender.hasPermission("essentials.mute")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    list.add(p.getName());
                }
            }
            return list;
        }

        if (args.length == 2 && !alias.equalsIgnoreCase("unmute")) {
            List<String> suggestions = Arrays.asList("10m", "30m", "1h", "1d", "7d", "30d");
            List<String> list = new ArrayList<>();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (String s : suggestions) {
                if (s.startsWith(prefix)) {
                    list.add(s);
                }
            }
            return list;
        }

        return Collections.emptyList();
    }
}
