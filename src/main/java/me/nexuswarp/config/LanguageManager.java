package me.nexuswarp.config;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.ColorUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    public static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
            "en", "ru", "zh", "de", "fr", "es", "pt", "ja", "uk", "it", "pl", "tr", "ko"
    );

    private final NexusWarpPlugin plugin;
    private FileConfiguration langConfig;
    private String currentLanguage = "ru";

    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveDefaultLanguages();

        String lang = plugin.getConfigManager().getLanguage();
        if (!SUPPORTED_LANGUAGES.contains(lang)) {
            plugin.getLogger().warning("Unsupported language '" + lang + "' specified in config.yml! Defaulting to 'en'.");
            lang = "en";
        }
        this.currentLanguage = lang;

        File langFile = new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + langFile.getName() + " not found, defaulting to messages_en.yml.");
            langFile = new File(plugin.getDataFolder(), "lang/messages_en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream defaultStream = plugin.getResource("lang/messages_" + lang + ".yml");
        if (defaultStream == null) {
            defaultStream = plugin.getResource("lang/messages_en.yml");
        }
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultConfig);
        }

        rebuildCache();
    }

    private void rebuildCache() {
        messageCache.clear();
        String prefix = langConfig.getString("prefix", "");

        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                String raw = langConfig.getString(key, "");
                if (key.equals("prefix") || key.startsWith("btn_") || key.startsWith("usage_")) {
                    messageCache.put(key, ColorUtil.color(raw));
                } else {
                    messageCache.put(key, ColorUtil.color(prefix + raw));
                }
            }
        }
    }

    private void saveDefaultLanguages() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        for (String langCode : SUPPORTED_LANGUAGES) {
            String fileName = "lang/messages_" + langCode + ".yml";
            File targetFile = new File(plugin.getDataFolder(), fileName);
            if (!targetFile.exists()) {
                plugin.saveResource(fileName, false);
            }
        }
    }

    public String getMessage(String key) {
        String msg = messageCache.get(key);
        if (msg == null) {
            String fallback = langConfig.getString(key, "");
            msg = ColorUtil.color(fallback);
            messageCache.put(key, msg);
        }
        return msg;
    }

    public String getMessageRaw(String key) {
        return langConfig.getString(key, "");
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        if (msg.isEmpty()) return "";

        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            if (placeholder != null && value != null) {
                msg = msg.replace("{" + placeholder + "}", value);
            }
        }
        return msg;
    }

    public void sendTpaButtons(Player target, String requesterName) {
        if (target == null || !target.isOnline()) return;

        String acceptText = getMessage("btn_accept");
        String acceptHover = getMessage("btn_accept_hover");
        String cancelText = getMessage("btn_cancel");
        String cancelHover = getMessage("btn_cancel_hover");

        TextComponent acceptComponent = new TextComponent(acceptText);
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + requesterName));
        acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(TextComponent.fromLegacyText(acceptHover))));

        TextComponent space = new TextComponent("   ");

        TextComponent cancelComponent = new TextComponent(cancelText);
        cancelComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpacancel " + requesterName));
        cancelComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(TextComponent.fromLegacyText(cancelHover))));

        target.spigot().sendMessage(acceptComponent, space, cancelComponent);
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
