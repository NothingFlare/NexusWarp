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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class LanguageManager {

    public static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
            "en", "ru", "zh", "de", "fr", "es", "pt", "ja", "uk", "it", "pl", "tr", "ko"
    );

    private final NexusWarpPlugin plugin;
    private FileConfiguration langConfig;
    private FileConfiguration defaultConfig;
    private FileConfiguration fallbackEnConfig;
    private String currentLanguage = "ru";

    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        syncLanguageFiles();

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

        // Load reference defaultConfig from JAR
        InputStream defaultStream = plugin.getResource("lang/messages_" + lang + ".yml");
        if (defaultStream != null) {
            defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultConfig);
        } else {
            defaultConfig = null;
        }

        // Load master English fallback from JAR
        InputStream enStream = plugin.getResource("lang/messages_en.yml");
        if (enStream != null) {
            fallbackEnConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(enStream, StandardCharsets.UTF_8));
        } else {
            fallbackEnConfig = null;
        }

        rebuildCache();
    }

    /**
     * Synchronizes and remaps all language configuration files in plugins/NexusWarp/lang.
     * If a file matches reference (all keys present), nothing is done.
     * If a file is missing keys, it is updated on disk with default values and comments.
     */
    public void syncLanguageFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // 1. Ensure all supported languages exist on disk
        for (String langCode : SUPPORTED_LANGUAGES) {
            String fileName = "lang/messages_" + langCode + ".yml";
            File targetFile = new File(plugin.getDataFolder(), fileName);
            if (!targetFile.exists()) {
                plugin.saveResource(fileName, false);
            }
        }

        // 2. Remap and synchronize all language files on disk
        File[] langFiles = langDir.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (langFiles != null) {
            for (File diskFile : langFiles) {
                String name = diskFile.getName();
                String langCode = name.substring("messages_".length(), name.length() - ".yml".length());

                InputStream refStream = plugin.getResource("lang/messages_" + langCode + ".yml");
                if (refStream == null) {
                    refStream = plugin.getResource("lang/messages_en.yml");
                }
                InputStream fallbackStream = plugin.getResource("lang/messages_en.yml");

                int added = syncLanguageFile(diskFile, refStream, fallbackStream);
                if (added > 0) {
                    plugin.getLogger().info("Updated language file '" + diskFile.getName() + "': added " + added + " missing entries.");
                }
            }
        }
    }

    /**
     * Compares the given disk language configuration with reference stream(s).
     * If the configuration matches (no missing keys), does nothing and returns 0.
     * If the configuration is missing keys, updates the file on disk by appending
     * the missing keys (preserving existing user customizations and comments) and returns the number of added keys.
     *
     * @param diskFile the language file on disk
     * @param refStream the primary reference stream from JAR (e.g. messages_ru.yml)
     * @param fallbackStream the fallback reference stream from JAR (e.g. messages_en.yml)
     * @return the number of missing keys added to the file (0 if config already matched reference)
     */
    public static int syncLanguageFile(File diskFile, InputStream refStream, InputStream fallbackStream) {
        if (diskFile == null || !diskFile.exists() || diskFile.isDirectory()) {
            return 0;
        }

        try {
            byte[] refBytes = refStream != null ? refStream.readAllBytes() : null;
            byte[] fallbackBytes = fallbackStream != null ? fallbackStream.readAllBytes() : null;

            YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(diskFile);

            YamlConfiguration refConfig = refBytes != null
                    ? YamlConfiguration.loadConfiguration(new InputStreamReader(new ByteArrayInputStream(refBytes), StandardCharsets.UTF_8))
                    : null;

            YamlConfiguration fallbackConfig = fallbackBytes != null
                    ? YamlConfiguration.loadConfiguration(new InputStreamReader(new ByteArrayInputStream(fallbackBytes), StandardCharsets.UTF_8))
                    : null;

            Set<String> missingKeys = new LinkedHashSet<>();
            if (refConfig != null) {
                for (String key : refConfig.getKeys(true)) {
                    if (refConfig.isString(key) && (!diskConfig.contains(key, true) || diskConfig.get(key) == null)) {
                        missingKeys.add(key);
                    }
                }
            }

            if (fallbackConfig != null) {
                for (String key : fallbackConfig.getKeys(true)) {
                    if (fallbackConfig.isString(key) && (!diskConfig.contains(key, true) || diskConfig.get(key) == null)) {
                        missingKeys.add(key);
                    }
                }
            }

            // Если конфиг совпадает - ничего не делаем
            if (missingKeys.isEmpty()) {
                return 0;
            }

            // Если языковой конфиг не совпадает - обновляем его
            List<String> refLines = refBytes != null ? readLines(new ByteArrayInputStream(refBytes)) : Collections.emptyList();
            List<String> fallbackLines = fallbackBytes != null ? readLines(new ByteArrayInputStream(fallbackBytes)) : Collections.emptyList();

            Map<String, List<String>> refEntries = parseEntriesWithComments(refLines);
            Map<String, List<String>> fallbackEntries = parseEntriesWithComments(fallbackLines);

            String existingContent = Files.readString(diskFile.toPath(), StandardCharsets.UTF_8);
            StringBuilder appendText = new StringBuilder();

            if (!existingContent.isEmpty() && !existingContent.endsWith("\n") && !existingContent.endsWith("\r")) {
                appendText.append("\n");
            }

            appendText.append("\n# ========================================================\n");
            appendText.append("# Added automatically during update (missing entries)\n");
            appendText.append("# ========================================================\n");

            for (String key : missingKeys) {
                List<String> entryLines = refEntries.get(key);
                if (entryLines == null || entryLines.isEmpty()) {
                    entryLines = fallbackEntries.get(key);
                }

                if (entryLines != null && !entryLines.isEmpty()) {
                    for (String line : entryLines) {
                        appendText.append(line).append("\n");
                    }
                } else {
                    String val = refConfig != null ? refConfig.getString(key) : null;
                    if (val == null && fallbackConfig != null) {
                        val = fallbackConfig.getString(key);
                    }
                    if (val == null) {
                        val = "";
                    }
                    appendText.append(key).append(": \"").append(escapeYaml(val)).append("\"\n");
                }
            }

            Files.writeString(diskFile.toPath(), existingContent + appendText.toString(), StandardCharsets.UTF_8);
            return missingKeys.size();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static List<String> readLines(InputStream is) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static Map<String, List<String>> parseEntriesWithComments(List<String> lines) {
        Map<String, List<String>> entries = new LinkedHashMap<>();
        if (lines == null || lines.isEmpty()) {
            return entries;
        }

        List<String> pendingComments = new ArrayList<>();
        String currentKey = null;
        List<String> currentBlock = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                if (currentKey == null) {
                    pendingComments.add(line);
                } else {
                    currentKey = null;
                    pendingComments.add(line);
                }
            } else if (line.matches("^[a-zA-Z0-9_.-]+\\s*:.*")) {
                int colonIdx = line.indexOf(':');
                String key = line.substring(0, colonIdx).trim();
                currentKey = key;
                currentBlock = new ArrayList<>(pendingComments);
                while (!currentBlock.isEmpty() && currentBlock.get(0).trim().isEmpty()) {
                    currentBlock.remove(0);
                }
                currentBlock.add(line);
                pendingComments.clear();
                entries.put(key, currentBlock);
            } else if (currentKey != null && (line.startsWith(" ") || line.startsWith("\t"))) {
                currentBlock.add(line);
            } else {
                pendingComments.add(line);
            }
        }
        return entries;
    }

    private static String escapeYaml(String val) {
        if (val == null) return "";
        return val.replace("\"", "\\\"");
    }

    private String formatMessage(String prefix, String key, String raw) {
        if (raw == null) return "";
        if (key.equals("prefix") || key.startsWith("btn_") || key.startsWith("usage_")) {
            return ColorUtil.color(raw);
        } else {
            return ColorUtil.color(prefix + raw);
        }
    }

    private void rebuildCache() {
        messageCache.clear();
        String prefix = langConfig != null ? langConfig.getString("prefix", "") : "";

        // Layer 1: English master defaults
        if (fallbackEnConfig != null) {
            for (String key : fallbackEnConfig.getKeys(true)) {
                if (fallbackEnConfig.isString(key)) {
                    messageCache.put(key, formatMessage(prefix, key, fallbackEnConfig.getString(key, "")));
                }
            }
        }

        // Layer 2: Default language resource from JAR
        if (defaultConfig != null) {
            for (String key : defaultConfig.getKeys(true)) {
                if (defaultConfig.isString(key)) {
                    messageCache.put(key, formatMessage(prefix, key, defaultConfig.getString(key, "")));
                }
            }
        }

        // Layer 3: File on disk (user translations take priority)
        if (langConfig != null) {
            for (String key : langConfig.getKeys(true)) {
                if (langConfig.isString(key)) {
                    String raw = langConfig.getString(key, "");
                    if (key.equals("prefix") || !raw.trim().isEmpty()) {
                        messageCache.put(key, formatMessage(prefix, key, raw));
                    }
                }
            }
        }
    }

    public String getMessage(String key) {
        String msg = messageCache.get(key);
        if (msg == null || (msg.isEmpty() && !key.equals("prefix"))) {
            String raw = getMessageRaw(key);
            String prefix = langConfig != null ? langConfig.getString("prefix", "") : "";
            msg = formatMessage(prefix, key, raw);
            messageCache.put(key, msg);
        }
        return msg;
    }

    public String getMessageRaw(String key) {
        if (langConfig != null && langConfig.isString(key)) {
            String val = langConfig.getString(key);
            if (val != null && (!val.trim().isEmpty() || key.equals("prefix"))) {
                return val;
            }
        }
        if (defaultConfig != null && defaultConfig.isString(key)) {
            String val = defaultConfig.getString(key);
            if (val != null && (!val.trim().isEmpty() || key.equals("prefix"))) {
                return val;
            }
        }
        if (fallbackEnConfig != null && fallbackEnConfig.isString(key)) {
            String val = fallbackEnConfig.getString(key);
            if (val != null && (!val.trim().isEmpty() || key.equals("prefix"))) {
                return val;
            }
        }
        return "";
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        if (msg.isEmpty() && !key.equals("prefix")) return "";

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

    public FileConfiguration getLangConfig() {
        return langConfig;
    }
}
