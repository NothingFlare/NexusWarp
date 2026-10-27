package me.nexuswarp.manager;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageManager {

    private final NexusWarpPlugin plugin;
    // Tracks the last conversational partner for each player
    private final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();
    // Special UUID representing Console
    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    public MessageManager(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    public void setReplyTarget(UUID player1, UUID player2) {
        if (player1 != null && player2 != null) {
            lastMessaged.put(player1, player2);
            lastMessaged.put(player2, player1);
        }
    }

    public UUID getReplyTarget(UUID playerId) {
        if (playerId == null) return null;
        return lastMessaged.get(playerId);
    }

    public void removePlayer(UUID playerId) {
        if (playerId == null) return;
        lastMessaged.remove(playerId);
        lastMessaged.values().removeIf(id -> id.equals(playerId));
    }

    public void sendPrivateMessage(CommandSender sender, Player recipient, String text) {
        if (sender == null || recipient == null || text == null) return;

        String senderName = sender.getName();
        String recipientName = recipient.getName();

        // Self-messaging check for players
        if (sender instanceof Player playerSender) {
            if (playerSender.getUniqueId().equals(recipient.getUniqueId())) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("cannot_msg_self"));
                return;
            }
            setReplyTarget(playerSender.getUniqueId(), recipient.getUniqueId());
        } else {
            // Console sender
            setReplyTarget(CONSOLE_UUID, recipient.getUniqueId());
        }

        // Format and send to sender
        String sendFormat = plugin.getLanguageManager().getMessage("msg_format_send",
                "receiver", recipientName,
                "message", text);
        sender.sendMessage(sendFormat);

        // Format and send to recipient
        String receiveFormat = plugin.getLanguageManager().getMessage("msg_format_receive",
                "sender", senderName,
                "message", text);
        recipient.sendMessage(receiveFormat);
    }

    public UUID getConsoleUUID() {
        return CONSOLE_UUID;
    }
}
