package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class KillCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public KillCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.kill") && !sender.hasPermission("essentials.kill")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("usage_kill"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found"));
            return true;
        }

        // Exemption check
        boolean isExempt = target.hasPermission("nexuswarp.kill.exempt") || target.hasPermission("essentials.kill.exempt");
        boolean canForce = sender.hasPermission("nexuswarp.kill.force") || sender.hasPermission("essentials.kill.force");
        if (isExempt && !canForce && sender instanceof Player) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("kill_exempt", "player", target.getName()));
            return true;
        }

        @SuppressWarnings("deprecation")
        EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.SUICIDE, Short.MAX_VALUE);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            target.setLastDamageCause(event);
            target.setHealth(0.0);
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("kill_success", "player", target.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && (sender.hasPermission("nexuswarp.kill") || sender.hasPermission("essentials.kill"))) {
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    list.add(p.getName());
                }
            }
            return list;
        }
        return Collections.emptyList();
    }
}
