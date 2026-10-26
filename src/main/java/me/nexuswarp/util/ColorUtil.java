package me.nexuswarp.util;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_PATTERN_3 = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
    private static final boolean SUPPORTS_HEX;

    static {
        boolean hex = false;
        try {
            ChatColor.class.getMethod("of", String.class);
            hex = true;
        } catch (Throwable ignored) {
            hex = false;
        }
        SUPPORTS_HEX = hex;
    }

    private ColorUtil() {}

    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        if (message.indexOf('#') != -1) {
            if (SUPPORTS_HEX) {
                if (message.contains("&#")) {
                    message = applyHex(message, HEX_PATTERN_1);
                }
                if (message.contains("<#")) {
                    message = applyHex(message, HEX_PATTERN_2);
                }
                if (message.contains("{#")) {
                    message = applyHex(message, HEX_PATTERN_3);
                }
            } else {
                if (message.contains("&#")) {
                    message = HEX_PATTERN_1.matcher(message).replaceAll("");
                }
                if (message.contains("<#")) {
                    message = HEX_PATTERN_2.matcher(message).replaceAll("");
                }
                if (message.contains("{#")) {
                    message = HEX_PATTERN_3.matcher(message).replaceAll("");
                }
            }
        }

        if (message.indexOf('&') != -1) {
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
        }

        return message;
    }

    public static List<String> color(List<String> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<String> colored = new ArrayList<>(list.size());
        for (String s : list) {
            colored.add(color(s));
        }
        return colored;
    }

    private static String applyHex(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexCode).toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
