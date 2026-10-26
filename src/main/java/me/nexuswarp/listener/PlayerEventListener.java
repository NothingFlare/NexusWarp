package me.nexuswarp.listener;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventListener implements Listener {

    private final NexusWarpPlugin plugin;

    public PlayerEventListener(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTpaManager().handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        plugin.getTpaManager().handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        plugin.getTpaManager().cancelWarmup(event.getEntity().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getConfigManager().isCancelOnMovement()) {
            return;
        }

        if (!plugin.getTpaManager().hasActiveWarmup(player.getUniqueId())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Check if player actually changed coordinates (ignore head rotation)
        if (from.getBlockX() != to.getBlockX() ||
            from.getBlockY() != to.getBlockY() ||
            from.getBlockZ() != to.getBlockZ()) {

            Location start = plugin.getTpaManager().getWarmupLocation(player.getUniqueId());
            if (start != null && start.getWorld().equals(to.getWorld())) {
                double distSq = start.distanceSquared(to);
                if (distSq > 0.05) {
                    plugin.getTpaManager().cancelWarmup(player.getUniqueId(), true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (plugin.getConfigManager().isCancelOnDamage() &&
                plugin.getTpaManager().hasActiveWarmup(player.getUniqueId())) {
                plugin.getTpaManager().cancelWarmupDamage(player.getUniqueId());
            }
        }
    }
}
