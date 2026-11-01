package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.ColorUtil;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.*;

public class ListCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public ListCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.list") && !sender.hasPermission("essentials.list")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        boolean canSeeHidden = sender.hasPermission("nexuswarp.list.hidden") || sender.hasPermission("essentials.list.hidden");

        int onlineVisible = 0;
        int hiddenCount = 0;
        int maxPlayers = Bukkit.getMaxPlayers();

        Map<String, List<String>> groupMap = new LinkedHashMap<>();
        List<String> allVisiblePlayers = new ArrayList<>();

        String afkTag = plugin.getLanguageManager().getMessageRaw("list_afk_tag");
        if (afkTag.isEmpty()) afkTag = "&7[AFK] ";
        String hiddenTag = plugin.getLanguageManager().getMessageRaw("list_hidden_tag");
        if (hiddenTag.isEmpty()) hiddenTag = "&7[HIDDEN] ";

        for (Player target : Bukkit.getOnlinePlayers()) {
            boolean hidden = isPlayerHidden(sender, target);
            if (hidden) {
                if (canSeeHidden) {
                    hiddenCount++;
                } else {
                    // Omit player completely from non-authorized view
                    continue;
                }
            }

            onlineVisible++;

            boolean afk = isPlayerAfk(target);
            String displayName = target.getDisplayName();
            String formattedName = (afk ? afkTag : "") + (hidden ? hiddenTag : "") + displayName + "&r";

            allVisiblePlayers.add(formattedName);

            String group = getPlayerGroup(target);
            groupMap.computeIfAbsent(group, k -> new ArrayList<>()).add(formattedName);
        }

        // Summary message
        if (hiddenCount > 0) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("list_summary_hidden",
                    "online", String.valueOf(onlineVisible),
                    "hidden", String.valueOf(hiddenCount),
                    "max", String.valueOf(maxPlayers)));
        } else {
            sender.sendMessage(plugin.getLanguageManager().getMessage("list_summary",
                    "online", String.valueOf(onlineVisible),
                    "max", String.valueOf(maxPlayers)));
        }

        if (onlineVisible == 0) {
            return true;
        }

        // If specific group requested: /list <group>
        if (args.length > 0) {
            String requestedGroup = args[0].toLowerCase(Locale.ROOT);
            Map.Entry<String, List<String>> found = null;
            for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
                if (entry.getKey().toLowerCase(Locale.ROOT).equals(requestedGroup)) {
                    found = entry;
                    break;
                }
            }

            if (found != null && !found.getValue().isEmpty()) {
                String joined = String.join("&7, &f", found.getValue());
                sender.sendMessage(plugin.getLanguageManager().getMessage("list_group",
                        "group", found.getKey(),
                        "players", ColorUtil.color(joined)));
            } else {
                sender.sendMessage(plugin.getLanguageManager().getMessage("list_group_not_found",
                        "group", args[0]));
            }
            return true;
        }

        // Normal output: either grouped or single list
        boolean hasMultipleGroups = groupMap.size() > 1 || (groupMap.size() == 1 && !groupMap.containsKey("Default") && !groupMap.containsKey("Players"));
        if (hasMultipleGroups) {
            for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
                String joined = String.join("&7, &f", entry.getValue());
                sender.sendMessage(plugin.getLanguageManager().getMessage("list_group",
                        "group", entry.getKey(),
                        "players", ColorUtil.color(joined)));
            }
        } else {
            String joined = String.join("&7, &f", allVisiblePlayers);
            sender.sendMessage(plugin.getLanguageManager().getMessage("list_players",
                    "players", ColorUtil.color(joined)));
        }

        return true;
    }

    private boolean isPlayerHidden(CommandSender viewer, Player target) {
        if (viewer instanceof Player p && !p.canSee(target)) {
            return true;
        }
        if (target.hasMetadata("vanished")) {
            for (MetadataValue meta : target.getMetadata("vanished")) {
                if (meta.asBoolean()) return true;
            }
        }
        return false;
    }

    private boolean isPlayerAfk(Player target) {
        if (target.hasMetadata("afk")) {
            for (MetadataValue meta : target.getMetadata("afk")) {
                if (meta.asBoolean()) return true;
            }
        }
        try {
            Method m = target.getClass().getMethod("isAfk");
            return (boolean) m.invoke(target);
        } catch (Throwable ignored) {}
        return false;
    }

    private String getPlayerGroup(Player player) {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                RegisteredServiceProvider<Permission> rsp =
                        Bukkit.getServicesManager().getRegistration(Permission.class);
                if (rsp != null && rsp.getProvider() != null && rsp.getProvider().hasGroupSupport()) {
                    String group = rsp.getProvider().getPrimaryGroup(player);
                    if (group != null && !group.isEmpty()) {
                        return capitalize(group);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return "Players";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.list") && !sender.hasPermission("essentials.list")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            Set<String> groups = new LinkedHashSet<>();
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player p && !p.canSee(target)
                        && !sender.hasPermission("nexuswarp.list.hidden")
                        && !sender.hasPermission("essentials.list.hidden")) {
                    continue;
                }
                groups.add(getPlayerGroup(target));
            }
            List<String> list = new ArrayList<>();
            String query = args[0].toLowerCase(Locale.ROOT);
            for (String g : groups) {
                if (g.toLowerCase(Locale.ROOT).startsWith(query)) {
                    list.add(g);
                }
            }
            return list;
        }

        return Collections.emptyList();
    }
}
