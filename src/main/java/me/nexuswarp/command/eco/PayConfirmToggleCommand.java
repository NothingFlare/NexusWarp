package me.nexuswarp.command.eco;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PayConfirmToggleCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public PayConfirmToggleCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
            return true;
        }

        if (!player.hasPermission("nexuswarp.payconfirmtoggle") && !player.hasPermission("essentials.payconfirmtoggle")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        boolean enable = !plugin.getEconomyManager().isPromptingPayConfirm(player.getUniqueId());

        String lowerLabel = label.toLowerCase(Locale.ROOT);
        if (lowerLabel.contains("payconfirmon")) {
            enable = true;
        } else if (lowerLabel.contains("payconfirmoff")) {
            enable = false;
        } else if (args.length > 0) {
            String arg = args[0].toLowerCase(Locale.ROOT);
            if (arg.equals("on") || arg.equals("enable") || arg.equals("true")) {
                enable = true;
            } else if (arg.equals("off") || arg.equals("disable") || arg.equals("false")) {
                enable = false;
            }
        }

        plugin.getEconomyManager().setPromptingPayConfirm(player.getUniqueId(), enable);
        plugin.getEconomyManager().clearPendingPayConfirm(player.getUniqueId());

        String msgKey = enable ? "eco_pay_confirm_toggle_on" : "eco_pay_confirm_toggle_off";
        player.sendMessage(plugin.getLanguageManager().getMessage(msgKey));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off");
        }
        return Collections.emptyList();
    }
}
