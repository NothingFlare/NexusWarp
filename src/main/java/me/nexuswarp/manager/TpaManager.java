package me.nexuswarp.manager;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.ServerScheduler;
import me.nexuswarp.util.TeleportSafetyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    private final NexusWarpPlugin plugin;

    // Incoming requests: target UUID -> TpaRequest
    private final Map<UUID, TpaRequest> incomingRequests = new ConcurrentHashMap<>();
    // Outgoing requests: requester UUID -> TpaRequest
    private final Map<UUID, TpaRequest> outgoingRequests = new ConcurrentHashMap<>();

    // Active teleport warmup tasks and starting locations
    private final Map<UUID, ServerScheduler.TaskHandle> activeWarmups = new ConcurrentHashMap<>();
    private final Map<UUID, Location> warmupLocations = new ConcurrentHashMap<>();

    public TpaManager(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player target) {
        if (sender == null || target == null || !sender.isOnline() || !target.isOnline()) {
            return;
        }

        // 1. Cannot TP to self
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("cannot_tp_self"));
            return;
        }

        // 2. Disabled worlds check
        boolean senderBypass = !plugin.getConfigManager().isIgnoreWorldBypass() && sender.hasPermission("nexuswarp.bypass.world");
        if (!senderBypass) {
            if (plugin.getConfigManager().isWorldDisabled(sender.getWorld())) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("world_disabled"));
                return;
            }
            if (plugin.getConfigManager().isWorldDisabled(target.getWorld())) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("target_world_disabled"));
                return;
            }
        }

        // 3. Existing request check (as in original script)
        TpaRequest existingIncoming = incomingRequests.get(target.getUniqueId());
        int timeout = plugin.getConfigManager().getRequestTimeout();
        if (existingIncoming != null && (timeout <= 0 || !existingIncoming.isExpired(timeout))) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("target_already_has_request"));
            return;
        }

        // Register new request
        TpaRequest request = new TpaRequest(
                sender.getUniqueId(),
                sender.getName(),
                target.getUniqueId(),
                target.getName()
        );

        incomingRequests.put(target.getUniqueId(), request);
        outgoingRequests.put(sender.getUniqueId(), request);

        // Send feedback messages
        sender.sendMessage(plugin.getLanguageManager().getMessage("request_sent", "player", target.getName()));
        target.sendMessage(plugin.getLanguageManager().getMessage("request_received", "player", sender.getName()));

        // Send clickable interactive buttons
        plugin.getLanguageManager().sendTpaButtons(target, sender.getName());
    }

    public void acceptRequest(Player receiver, String targetSenderName) {
        if (receiver == null || !receiver.isOnline()) return;

        TpaRequest request = incomingRequests.get(receiver.getUniqueId());
        int timeout = plugin.getConfigManager().getRequestTimeout();
        if (request == null || (timeout > 0 && request.isExpired(timeout))) {
            incomingRequests.remove(receiver.getUniqueId());
            receiver.sendMessage(plugin.getLanguageManager().getMessage("no_active_requests"));
            return;
        }

        if (targetSenderName != null && !targetSenderName.trim().isEmpty()) {
            if (!request.getRequesterName().equalsIgnoreCase(targetSenderName.trim())) {
                receiver.sendMessage(plugin.getLanguageManager().getMessage("no_request_with_player"));
                return;
            }
        }

        clearRequest(request);

        Player sender = Bukkit.getPlayer(request.getRequesterId());
        if (sender == null || !sender.isOnline()) {
            receiver.sendMessage(plugin.getLanguageManager().getMessage("player_not_online"));
            return;
        }

        // World disabled re-check on accept
        boolean receiverBypass = !plugin.getConfigManager().isIgnoreWorldBypass() && receiver.hasPermission("nexuswarp.bypass.world");
        if (!receiverBypass) {
            if (plugin.getConfigManager().isWorldDisabled(receiver.getWorld()) ||
                    plugin.getConfigManager().isWorldDisabled(sender.getWorld())) {
                String worldMsg = plugin.getLanguageManager().getMessage("world_disabled");
                receiver.sendMessage(worldMsg);
                sender.sendMessage(worldMsg);
                return;
            }
        }

        // Safety check
        Location destLoc = receiver.getLocation();
        if (plugin.getConfigManager().isSafeTeleportEnabled()) {
            Location safeLoc = TeleportSafetyUtil.getSafeLocation(
                    destLoc,
                    plugin.getConfigManager().isPreventLava(),
                    plugin.getConfigManager().isPreventVoid(),
                    plugin.getConfigManager().isPreventSuffocation()
            );

            if (safeLoc == null) {
                String unsafeMsg = plugin.getLanguageManager().getMessage("teleport_unsafe");
                receiver.sendMessage(unsafeMsg);
                sender.sendMessage(unsafeMsg);
                return;
            }
            destLoc = safeLoc;
        }

        // Warmup delay handling
        int delay = plugin.getConfigManager().getTeleportDelay();
        if (delay > 0 && !sender.hasPermission("nexuswarp.bypass.delay")) {
            startWarmup(sender, receiver, destLoc, delay);
        } else {
            executeTeleport(sender, receiver, destLoc);
        }
    }

    private void startWarmup(Player sender, Player receiver, Location targetLocation, int seconds) {
        cancelWarmup(sender.getUniqueId(), false);

        warmupLocations.put(sender.getUniqueId(), sender.getLocation().clone());
        sender.sendMessage(plugin.getLanguageManager().getMessage("teleport_delay", "seconds", String.valueOf(seconds)));

        UUID senderId = sender.getUniqueId();
        UUID receiverId = receiver.getUniqueId();

        ServerScheduler.TaskHandle task = ServerScheduler.runOnPlayerDelayed(plugin, sender, () -> {
            activeWarmups.remove(senderId);
            warmupLocations.remove(senderId);

            if (!sender.isOnline()) return;

            Player rec = Bukkit.getPlayer(receiverId);
            if (rec == null || !rec.isOnline()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_online"));
                return;
            }

            executeTeleport(sender, rec, targetLocation);
        }, seconds * 20L);

        activeWarmups.put(senderId, task);
    }

    private void executeTeleport(Player sender, Player receiver, Location targetLocation) {
        ServerScheduler.teleportAsync(sender, targetLocation).thenAccept(success -> {
            ServerScheduler.runOnPlayer(plugin, sender, () -> {
                if (success) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("teleport_accepted_sender"));
                    if (receiver.isOnline()) {
                        receiver.sendMessage(plugin.getLanguageManager().getMessage("teleport_accepted_receiver", "player", sender.getName()));
                    }
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("teleport_unsafe"));
                }
            });
        });
    }

    public void cancelRequest(Player player, String otherPlayerName) {
        if (player == null || !player.isOnline()) return;

        // Case 1: Player is receiver, cancels request from target
        TpaRequest incoming = incomingRequests.get(player.getUniqueId());
        if (incoming != null) {
            if (otherPlayerName == null || otherPlayerName.trim().isEmpty() ||
                    incoming.getRequesterName().equalsIgnoreCase(otherPlayerName.trim())) {

                clearRequest(incoming);
                player.sendMessage(plugin.getLanguageManager().getMessage("cancel_receiver_self", "player", incoming.getRequesterName()));

                Player reqPlayer = Bukkit.getPlayer(incoming.getRequesterId());
                if (reqPlayer != null && reqPlayer.isOnline()) {
                    reqPlayer.sendMessage(plugin.getLanguageManager().getMessage("cancel_receiver_other", "player", player.getName()));
                }
                return;
            }
        }

        // Case 2: Player is sender, cancels request to target
        TpaRequest outgoing = outgoingRequests.get(player.getUniqueId());
        if (outgoing != null) {
            if (otherPlayerName == null || otherPlayerName.trim().isEmpty() ||
                    outgoing.getTargetName().equalsIgnoreCase(otherPlayerName.trim())) {

                clearRequest(outgoing);
                player.sendMessage(plugin.getLanguageManager().getMessage("cancel_sender_self", "player", outgoing.getTargetName()));

                Player tgtPlayer = Bukkit.getPlayer(outgoing.getTargetId());
                if (tgtPlayer != null && tgtPlayer.isOnline()) {
                    tgtPlayer.sendMessage(plugin.getLanguageManager().getMessage("cancel_sender_other", "player", player.getName()));
                }
                return;
            }
        }

        player.sendMessage(plugin.getLanguageManager().getMessage("no_request_with_player"));
    }

    public void clearRequest(TpaRequest request) {
        if (request == null) return;
        incomingRequests.remove(request.getTargetId());
        outgoingRequests.remove(request.getRequesterId());
    }

    public void handlePlayerQuit(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        cancelWarmup(uuid, false);

        TpaRequest incoming = incomingRequests.remove(uuid);
        if (incoming != null) {
            outgoingRequests.remove(incoming.getRequesterId());
        }

        TpaRequest outgoing = outgoingRequests.remove(uuid);
        if (outgoing != null) {
            incomingRequests.remove(outgoing.getTargetId());
        }
    }

    public void cancelWarmup(UUID playerId, boolean notify) {
        ServerScheduler.TaskHandle task = activeWarmups.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        warmupLocations.remove(playerId);

        if (notify) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(plugin.getLanguageManager().getMessage("teleport_cancelled_movement"));
            }
        }
    }

    public void cancelWarmupDamage(UUID playerId) {
        ServerScheduler.TaskHandle task = activeWarmups.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        warmupLocations.remove(playerId);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.getLanguageManager().getMessage("teleport_cancelled_damage"));
        }
    }

    public boolean hasActiveWarmup(UUID playerId) {
        return activeWarmups.containsKey(playerId);
    }

    public Location getWarmupLocation(UUID playerId) {
        return warmupLocations.get(playerId);
    }

    public void tickExpire() {
        int timeout = plugin.getConfigManager().getRequestTimeout();
        if (timeout <= 0) return;

        Iterator<Map.Entry<UUID, TpaRequest>> it = incomingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TpaRequest> entry = it.next();
            TpaRequest request = entry.getValue();

            if (request.isExpired(timeout)) {
                it.remove();
                outgoingRequests.remove(request.getRequesterId());

                Player sender = Bukkit.getPlayer(request.getRequesterId());
                if (sender != null && sender.isOnline()) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("request_expired_sender", "player", request.getTargetName()));
                }

                Player target = Bukkit.getPlayer(request.getTargetId());
                if (target != null && target.isOnline()) {
                    target.sendMessage(plugin.getLanguageManager().getMessage("request_expired_target", "player", request.getRequesterName()));
                }
            }
        }
    }

    public List<String> getPendingSenderNamesForReceiver(UUID receiverId) {
        TpaRequest req = incomingRequests.get(receiverId);
        int timeout = plugin.getConfigManager().getRequestTimeout();
        if (req != null && (timeout <= 0 || !req.isExpired(timeout))) {
            return Collections.singletonList(req.getRequesterName());
        }
        return Collections.emptyList();
    }

    public List<String> getRelevantNamesForCancel(Player player) {
        List<String> list = new ArrayList<>();
        int timeout = plugin.getConfigManager().getRequestTimeout();

        TpaRequest inc = incomingRequests.get(player.getUniqueId());
        if (inc != null && (timeout <= 0 || !inc.isExpired(timeout))) {
            list.add(inc.getRequesterName());
        }
        TpaRequest out = outgoingRequests.get(player.getUniqueId());
        if (out != null && (timeout <= 0 || !out.isExpired(timeout))) {
            list.add(out.getTargetName());
        }
        return list;
    }

    public void clearAll() {
        for (ServerScheduler.TaskHandle handle : activeWarmups.values()) {
            if (handle != null) {
                handle.cancel();
            }
        }
        activeWarmups.clear();
        warmupLocations.clear();
        incomingRequests.clear();
        outgoingRequests.clear();
    }
}
