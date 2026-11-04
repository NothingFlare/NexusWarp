package me.nexuswarp.hook.papi;

import me.clip.placeholderapi.PlaceholderAPI;
import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class PlaceholderHookManager {

    private final NexusWarpPlugin plugin;
    private boolean papiHooked = false;
    private NexusWarpPlaceholderExpansion nexusExpansion;
    private NexusWarpPlaceholderExpansion essentialsExpansion;

    public PlaceholderHookManager(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            plugin.getLogger().info("[NexusWarp] PlaceholderAPI not found. PAPI placeholders are disabled.");
            return;
        }

        try {
            this.nexusExpansion = new NexusWarpPlaceholderExpansion(plugin, "nexuswarp");
            this.nexusExpansion.register();
            this.papiHooked = true;
            plugin.getLogger().info("[NexusWarp] Successfully registered %nexuswarp_*% PlaceholderAPI expansion!");

            // Register %essentials_*% bridge if Essentials is not installed
            if (!Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
                this.essentialsExpansion = new NexusWarpPlaceholderExpansion(plugin, "essentials");
                this.essentialsExpansion.register();
                plugin.getLogger().info("[NexusWarp] Registered %essentials_*% PlaceholderAPI bridge for backwards compatibility!");
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "[NexusWarp] Failed to register PlaceholderAPI expansion: " + t.getMessage(), t);
            this.papiHooked = false;
        }
    }

    public void unregister() {
        if (nexusExpansion != null) {
            try {
                nexusExpansion.unregister();
            } catch (Throwable ignored) {}
            nexusExpansion = null;
        }
        if (essentialsExpansion != null) {
            try {
                essentialsExpansion.unregister();
            } catch (Throwable ignored) {}
            essentialsExpansion = null;
        }
        this.papiHooked = false;
    }

    public boolean isPapiHooked() {
        return papiHooked;
    }

    public String setPlaceholders(Player player, String text) {
        if (!papiHooked || text == null || text.indexOf('%') == -1) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable t) {
            return text;
        }
    }

    public String setPlaceholders(OfflinePlayer player, String text) {
        if (!papiHooked || text == null || text.indexOf('%') == -1) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable t) {
            return text;
        }
    }
}
