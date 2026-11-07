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

    // Incoming requests: target UUID -> (requester UUID -> TpaRequest)
    // Allows multiple players to send TPA requests to the same target in parallel
    private final Map<UUID, Map<UUID, TpaRequest>> incomingRequests = new ConcurrentHashMap<>();

    // Outgoing requests: requester UUID -> (target UUID -> TpaRequest)
    private final Map<UUID, Map<UUID, TpaRequest>> outgoingRequests = new ConcurrentHashMap<>();

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

        // 3. Register or refresh request (parallel multi-request support)
        TpaRequest request = new TpaRequest(
                sender.getUniqueId(),
                sender.getName(),
                target.getUniqueId(),
                target.getName()
        );

        incomingRequests.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(sender.getUniqueId(), request);
        outgoingRequests.computeIfAbsent(sender.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(target.getUniqueId(), request);

        // Send feedback messages
        sender.sendMessage(plugin.getLanguageManager().getMessage("request_sent", "player", target.getName()));
        target.sendMessage(plugin.getLanguageManager().getMessage("request_received", "player", sender.getName()));

        // Send clickable interactive buttons
        plugin.getLanguageManager().sendTpaButtons(target, sender.getName());
    }

    public void acceptRequest(Player receiver, String targetSenderName) {
        if (receiver == null || !receiver.isOnline()) return;

        Map<UUID, TpaRequest> incomingMap = incomingRequests.get(receiver.getUniqueId());
        int timeout = plugin.getConfigManager().getRequestTimeout();

        if (incomingMap == null || incomingMap.isEmpty()) {
            receiver.sendMessage(plugin.getLanguageManager().getMessage("no_active_requests"));
            return;
        }

        // Clean expired requests
        if (timeout > 0) {
            incomingMap.values().removeIf(req -> {
                if (req.isExpired(timeout)) {
                    Map<UUID, TpaRequest> out = outgoingRequests.get(req.getRequesterId());
                    if (out != null) {
                        out.remove(receiver.getUniqueId());
                        if (out.isEmpty()) outgoingRequests.remove(req.getRequesterId());
                    }
                    return true;
                }
                return false;
            });

            if (incomingMap.isEmpty()) {
                incomingRequests.remove(receiver.getUniqueId());
                receiver.sendMessage(plugin.getLanguageManager().getMessage("no_active_requests"));
                return;
            }
        }

        TpaRequest selected = null;

        if (targetSenderName != null && !targetSenderName.trim().isEmpty()) {
            String trimmed = targetSenderName.trim();
            for (TpaRequest req : incomingMap.values()) {
                if (req.getRequesterName().equalsIgnoreCase(trimmed)) {
                    selected = req;
                    break;
                }
            }
            if (selected == null) {
                receiver.sendMessage(plugin.getLanguageManager().getMessage("no_request_with_player"));
                return;
            }
        } else {
            // Select most recent request
            long latest = -1L;
            for (TpaRequest req : incomingMap.values()) {
                if (req.getCreationTime() > latest) {
                    latest = req.getCreationTime();
                    selected = req;
                }
            }
        }

        if (selected == null) {
            receiver.sendMessage(plugin.getLanguageManager().getMessage("no_active_requests"));
            return;
        }

        clearRequest(selected);

        Player sender = Bukkit.getPlayer(selected.getRequesterId());
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

    public void teleportWithWarmup(Player player, Location targetLocation, String successMessageKey) {
        teleportWithWarmup(player, targetLocation, plugin.getConfigManager().getTeleportDelay(), successMessageKey);
    }

    public void teleportWithWarmup(Player player, Location targetLocation, int customDelay, String successMessageKey) {
        if (player == null || !player.isOnline()) return;

        int delay = Math.max(0, customDelay);
        if (delay > 0 && !player.hasPermission("nexuswarp.bypass.delay")) {
            cancelWarmup(player.getUniqueId(), false);

            warmupLocations.put(player.getUniqueId(), player.getLocation().clone());
            player.sendMessage(plugin.getLanguageManager().getMessage("teleport_delay", "seconds", String.valueOf(delay)));

            UUID playerId = player.getUniqueId();
            ServerScheduler.TaskHandle task = ServerScheduler.runOnPlayerDelayed(plugin, player, () -> {
                activeWarmups.remove(playerId);
                warmupLocations.remove(playerId);

                if (!player.isOnline()) return;

                ServerScheduler.teleportAsync(player, targetLocation).thenAccept(success -> {
                    if (success) {
                        ServerScheduler.runOnPlayer(plugin, player, () -> {
                            if (successMessageKey != null && !successMessageKey.isEmpty()) {
                                player.sendMessage(plugin.getLanguageManager().getMessage(successMessageKey));
                            }
                        });
                    } else {
                        ServerScheduler.runOnPlayer(plugin, player, () -> {
                            player.sendMessage(plugin.getLanguageManager().getMessage("teleport_unsafe"));
                        });
                    }
                });
            }, delay * 20L);

            activeWarmups.put(playerId, task);
        } else {
            ServerScheduler.teleportAsync(player, targetLocation).thenAccept(success -> {
                if (success) {
                    ServerScheduler.runOnPlayer(plugin, player, () -> {
                        if (successMessageKey != null && !successMessageKey.isEmpty()) {
                            player.sendMessage(plugin.getLanguageManager().getMessage(successMessageKey));
                        }
                    });
                } else {
                    ServerScheduler.runOnPlayer(plugin, player, () -> {
                        player.sendMessage(plugin.getLanguageManager().getMessage("teleport_unsafe"));
                    });
                }
            });
        }
    }

    public void cancelRequest(Player player, String otherPlayerName) {
        if (player == null || !player.isOnline()) return;

        int timeout = plugin.getConfigManager().getRequestTimeout();
        String trimmed = (otherPlayerName != null) ? otherPlayerName.trim() : null;

        // Case 1: Player is receiver, cancelling an incoming request
        Map<UUID, TpaRequest> incomingMap = incomingRequests.get(player.getUniqueId());
        if (incomingMap != null && !incomingMap.isEmpty()) {
            TpaRequest toCancel = null;
            if (trimmed != null && !trimmed.isEmpty()) {
                for (TpaRequest req : incomingMap.values()) {
                    if (req.getRequesterName().equalsIgnoreCase(trimmed)) {
                        toCancel = req;
                        break;
                    }
                }
            } else {
                // Cancel most recent
                long latest = -1L;
                for (TpaRequest req : incomingMap.values()) {
                    if (req.getCreationTime() > latest) {
                        latest = req.getCreationTime();
                        toCancel = req;
                    }
                }
            }

            if (toCancel != null) {
                clearRequest(toCancel);
                player.sendMessage(plugin.getLanguageManager().getMessage("cancel_receiver_self", "player", toCancel.getRequesterName()));

                Player reqPlayer = Bukkit.getPlayer(toCancel.getRequesterId());
                if (reqPlayer != null && reqPlayer.isOnline()) {
                    reqPlayer.sendMessage(plugin.getLanguageManager().getMessage("cancel_receiver_other", "player", player.getName()));
                }
                return;
            }
        }

        // Case 2: Player is sender, cancelling an outgoing request
        Map<UUID, TpaRequest> outgoingMap = outgoingRequests.get(player.getUniqueId());
        if (outgoingMap != null && !outgoingMap.isEmpty()) {
            TpaRequest toCancel = null;
            if (trimmed != null && !trimmed.isEmpty()) {
                for (TpaRequest req : outgoingMap.values()) {
                    if (req.getTargetName().equalsIgnoreCase(trimmed)) {
                        toCancel = req;
                        break;
                    }
                }
            } else {
                // Cancel most recent
                long latest = -1L;
                for (TpaRequest req : outgoingMap.values()) {
                    if (req.getCreationTime() > latest) {
                        latest = req.getCreationTime();
                        toCancel = req;
                    }
                }
            }

            if (toCancel != null) {
                clearRequest(toCancel);
                player.sendMessage(plugin.getLanguageManager().getMessage("cancel_sender_self", "player", toCancel.getTargetName()));

                Player tgtPlayer = Bukkit.getPlayer(toCancel.getTargetId());
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

        Map<UUID, TpaRequest> inc = incomingRequests.get(request.getTargetId());
        if (inc != null) {
            inc.remove(request.getRequesterId());
            if (inc.isEmpty()) {
                incomingRequests.remove(request.getTargetId());
            }
        }

        Map<UUID, TpaRequest> out = outgoingRequests.get(request.getRequesterId());
        if (out != null) {
            out.remove(request.getTargetId());
            if (out.isEmpty()) {
                outgoingRequests.remove(request.getRequesterId());
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        cancelWarmup(uuid, false);

        // Clean all incoming requests to this player
        Map<UUID, TpaRequest> incoming = incomingRequests.remove(uuid);
        if (incoming != null) {
            for (TpaRequest req : incoming.values()) {
                Map<UUID, TpaRequest> out = outgoingRequests.get(req.getRequesterId());
                if (out != null) {
                    out.remove(uuid);
                    if (out.isEmpty()) {
                        outgoingRequests.remove(req.getRequesterId());
                    }
                }
            }
        }

        // Clean all outgoing requests from this player
        Map<UUID, TpaRequest> outgoing = outgoingRequests.remove(uuid);
        if (outgoing != null) {
            for (TpaRequest req : outgoing.values()) {
                Map<UUID, TpaRequest> inc = incomingRequests.get(req.getTargetId());
                if (inc != null) {
                    inc.remove(uuid);
                    if (inc.isEmpty()) {
                        incomingRequests.remove(req.getTargetId());
                    }
                }
            }
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

        Iterator<Map.Entry<UUID, Map<UUID, TpaRequest>>> targetIt = incomingRequests.entrySet().iterator();
        while (targetIt.hasNext()) {
            Map.Entry<UUID, Map<UUID, TpaRequest>> entry = targetIt.next();
            Map<UUID, TpaRequest> senderMap = entry.getValue();

            Iterator<Map.Entry<UUID, TpaRequest>> senderIt = senderMap.entrySet().iterator();
            while (senderIt.hasNext()) {
                Map.Entry<UUID, TpaRequest> reqEntry = senderIt.next();
                TpaRequest request = reqEntry.getValue();

                if (request.isExpired(timeout)) {
                    senderIt.remove();

                    Map<UUID, TpaRequest> out = outgoingRequests.get(request.getRequesterId());
                    if (out != null) {
                        out.remove(request.getTargetId());
                        if (out.isEmpty()) {
                            outgoingRequests.remove(request.getRequesterId());
                        }
                    }

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

            if (senderMap.isEmpty()) {
                targetIt.remove();
            }
        }
    }

    public List<String> getPendingSenderNamesForReceiver(UUID receiverId) {
        Map<UUID, TpaRequest> map = incomingRequests.get(receiverId);
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        int timeout = plugin.getConfigManager().getRequestTimeout();
        List<String> list = new ArrayList<>();
        for (TpaRequest req : map.values()) {
            if (timeout <= 0 || !req.isExpired(timeout)) {
                list.add(req.getRequesterName());
            }
        }
        return list;
    }

    public List<String> getRelevantNamesForCancel(Player player) {
        List<String> list = new ArrayList<>();
        int timeout = plugin.getConfigManager().getRequestTimeout();

        Map<UUID, TpaRequest> inc = incomingRequests.get(player.getUniqueId());
        if (inc != null) {
            for (TpaRequest req : inc.values()) {
                if (timeout <= 0 || !req.isExpired(timeout)) {
                    list.add(req.getRequesterName());
                }
            }
        }

        Map<UUID, TpaRequest> out = outgoingRequests.get(player.getUniqueId());
        if (out != null) {
            for (TpaRequest req : out.values()) {
                if (timeout <= 0 || !req.isExpired(timeout)) {
                    list.add(req.getTargetName());
                }
            }
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
