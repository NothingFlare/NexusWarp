package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.DateUtil;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HealCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public HealCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /heal (self)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                return true;
            }

            if (!sender.hasPermission("nexuswarp.heal") && !sender.hasPermission("essentials.heal")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }

            // Cooldown check
            if (!sender.hasPermission("nexuswarp.heal.cooldown.bypass") && !sender.hasPermission("essentials.heal.cooldown.bypass")) {
                int cooldownSeconds = plugin.getConfig().getInt("heal-cooldown", 0);
                if (cooldownSeconds > 0) {
                    long now = System.currentTimeMillis();
                    long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
                    long diff = now - lastUsed;
                    long waitMs = (cooldownSeconds * 1000L) - diff;
                    if (waitMs > 0) {
                        String timeStr = DateUtil.formatDuration(waitMs, plugin.getLanguageManager().getCurrentLanguage());
                        player.sendMessage(plugin.getLanguageManager().getMessage("heal_cooldown", "time", timeStr));
                        return true;
                    }
                }
            }

            performHeal(player);
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage(plugin.getLanguageManager().getMessage("heal_success_self"));
            return true;
        }

        // /heal <player|*>
        if (!sender.hasPermission("nexuswarp.heal.others") && !sender.hasPermission("essentials.heal.others")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        String targetArg = args[0];

        // Heal all players
        if (targetArg.equals("*") || targetArg.equalsIgnoreCase("all")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                performHeal(p);
                p.sendMessage(plugin.getLanguageManager().getMessage("heal_target_healed", "sender", sender.getName()));
            }
            sender.sendMessage(plugin.getLanguageManager().getMessage("heal_all_success"));
            return true;
        }

        Player target = Bukkit.getPlayer(targetArg);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found"));
            return true;
        }

        performHeal(target);
        sender.sendMessage(plugin.getLanguageManager().getMessage("heal_success_other", "player", target.getName()));
        target.sendMessage(plugin.getLanguageManager().getMessage("heal_target_healed", "sender", sender.getName()));
        return true;
    }

    private void performHeal(Player player) {
        double maxHealth = 20.0;
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            maxHealth = attr.getValue();
        }

        double amountToHeal = Math.max(0.0, maxHealth - player.getHealth());
        if (amountToHeal > 0.0) {
            EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, amountToHeal, EntityRegainHealthEvent.RegainReason.CUSTOM);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                player.setHealth(maxHealth);
            }
        } else {
            player.setHealth(maxHealth);
        }

        player.setFoodLevel(20);
        player.setSaturation(10.0f);
        player.setFireTicks(0);
        player.setRemainingAir(player.getMaximumAir());

        // Remove active potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && (sender.hasPermission("nexuswarp.heal.others") || sender.hasPermission("essentials.heal.others"))) {
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("*".startsWith(prefix)) list.add("*");
            if ("all".startsWith(prefix)) list.add("all");
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
