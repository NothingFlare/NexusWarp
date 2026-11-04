package me.nexuswarp.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*(?:s[a-z]*)?)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final SimpleDateFormat DATE_FORMAT_RU = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.ROOT);
    private static final SimpleDateFormat DATE_FORMAT_EN = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private DateUtil() {}

    /**
     * Parses duration string (e.g. "1d", "2h30m", "10s", "1mo", "1y") into future/past epoch timestamp in milliseconds.
     */
    public static long parseDateDiff(String time, boolean future) throws IllegalArgumentException {
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException("Time string is empty");
        }
        Matcher m = TIME_PATTERN.matcher(time.trim());
        int years = 0;
        int months = 0;
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        boolean found = false;

        while (m.find()) {
            if (m.group() == null || m.group().isEmpty()) {
                continue;
            }
            for (int i = 1; i <= m.groupCount(); i++) {
                if (m.group(i) != null && !m.group(i).isEmpty()) {
                    found = true;
                    break;
                }
            }
            if (found) {
                if (m.group(1) != null && !m.group(1).isEmpty()) years = Integer.parseInt(m.group(1));
                if (m.group(2) != null && !m.group(2).isEmpty()) months = Integer.parseInt(m.group(2));
                if (m.group(3) != null && !m.group(3).isEmpty()) weeks = Integer.parseInt(m.group(3));
                if (m.group(4) != null && !m.group(4).isEmpty()) days = Integer.parseInt(m.group(4));
                if (m.group(5) != null && !m.group(5).isEmpty()) hours = Integer.parseInt(m.group(5));
                if (m.group(6) != null && !m.group(6).isEmpty()) minutes = Integer.parseInt(m.group(6));
                if (m.group(7) != null && !m.group(7).isEmpty()) seconds = Integer.parseInt(m.group(7));
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Illegal date format: " + time);
        }

        Calendar c = new GregorianCalendar();
        int sign = future ? 1 : -1;
        if (years > 0) c.add(Calendar.YEAR, Math.min(years, 100) * sign);
        if (months > 0) c.add(Calendar.MONTH, months * sign);
        if (weeks > 0) c.add(Calendar.WEEK_OF_YEAR, weeks * sign);
        if (days > 0) c.add(Calendar.DAY_OF_MONTH, days * sign);
        if (hours > 0) c.add(Calendar.HOUR_OF_DAY, hours * sign);
        if (minutes > 0) c.add(Calendar.MINUTE, minutes * sign);
        if (seconds > 0) c.add(Calendar.SECOND, seconds * sign);

        return c.getTimeInMillis();
    }

    /**
     * Parses duration string (e.g. "1d", "2h30m") into duration in milliseconds.
     */
    public static long parseDuration(String time) throws IllegalArgumentException {
        long target = parseDateDiff(time, true);
        return Math.max(0L, target - System.currentTimeMillis());
    }

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
