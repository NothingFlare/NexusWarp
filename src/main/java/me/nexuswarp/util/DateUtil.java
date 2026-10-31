package me.nexuswarp.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtil {

    private static final SimpleDateFormat DATE_FORMAT_RU = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.ROOT);
    private static final SimpleDateFormat DATE_FORMAT_EN = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private DateUtil() {}

    /**
     * Formats duration in milliseconds into human-readable string (e.g. "3 дн. 5 ч. 12 мин." or "3d 5h 12m").
     */
    public static String formatDuration(long millis, String lang) {
        if (millis <= 0) {
            return isRussian(lang) ? "0 сек." : "0s";
        }

        long seconds = millis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        seconds %= 60L;
        minutes %= 60L;
        hours %= 24L;

        StringBuilder sb = new StringBuilder();
        boolean ru = isRussian(lang);

        if (days > 0) {
            sb.append(days).append(ru ? " дн. " : "d ");
        }
        if (hours > 0) {
            sb.append(hours).append(ru ? " ч. " : "h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(ru ? " мин. " : "m ");
        }
        if (days == 0 && hours == 0 && (seconds > 0 || sb.length() == 0)) {
            sb.append(seconds).append(ru ? " сек." : "s");
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? (ru ? "0 сек." : "0s") : result;
    }

    /**
     * Formats time difference between now and target timestamp (in the past or future).
     */
    public static String formatDateDiff(long targetEpochMs, String lang) {
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - targetEpochMs);
        boolean ru = isRussian(lang);

        if (diff < 1000L) {
            return ru ? "только что" : "just now";
        }

        return formatDuration(diff, lang);
    }

    /**
     * Formats absolute date and time into string.
     */
    public static String formatDateTime(long epochMs, String lang) {
        if (epochMs <= 0) {
            return isRussian(lang) ? "Никогда" : "Never";
        }
        Date date = new Date(epochMs);
        SimpleDateFormat sdf = isRussian(lang) ? DATE_FORMAT_RU : DATE_FORMAT_EN;
        synchronized (sdf) {
            return sdf.format(date);
        }
    }

    private static boolean isRussian(String lang) {
        return lang != null && (lang.equalsIgnoreCase("ru") || lang.equalsIgnoreCase("uk"));
    }
}
