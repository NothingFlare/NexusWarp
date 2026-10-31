package me.nexuswarp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DateUtilTest {

    @Test
    public void testFormatDurationRussian() {
        assertEquals("0 сек.", DateUtil.formatDuration(0, "ru"));
        assertEquals("5 сек.", DateUtil.formatDuration(5000, "ru"));
        assertEquals("2 мин. 15 сек.", DateUtil.formatDuration((2 * 60 + 15) * 1000L, "ru"));
        assertEquals("1 ч. 30 мин.", DateUtil.formatDuration((90 * 60) * 1000L, "ru"));
        assertEquals("2 дн. 4 ч. 10 мин.", DateUtil.formatDuration((2 * 24 * 3600 + 4 * 3600 + 10 * 60) * 1000L, "ru"));
    }

    @Test
    public void testFormatDurationEnglish() {
        assertEquals("0s", DateUtil.formatDuration(0, "en"));
        assertEquals("5s", DateUtil.formatDuration(5000, "en"));
        assertEquals("2m 15s", DateUtil.formatDuration((2 * 60 + 15) * 1000L, "en"));
        assertEquals("1h 30m", DateUtil.formatDuration((90 * 60) * 1000L, "en"));
        assertEquals("2d 4h 10m", DateUtil.formatDuration((2 * 24 * 3600 + 4 * 3600 + 10 * 60) * 1000L, "en"));
    }

    @Test
    public void testFormatDateDiff() {
        long now = System.currentTimeMillis();
        assertEquals("только что", DateUtil.formatDateDiff(now - 100, "ru"));
        assertEquals("just now", DateUtil.formatDateDiff(now - 100, "en"));

        long fiveMinutesAgo = now - 5 * 60 * 1000L;
        assertTrue(DateUtil.formatDateDiff(fiveMinutesAgo, "ru").contains("мин."));
        assertTrue(DateUtil.formatDateDiff(fiveMinutesAgo, "en").contains("m"));
    }

    @Test
    public void testFormatDateTime() {
        assertEquals("Никогда", DateUtil.formatDateTime(0, "ru"));
        assertEquals("Never", DateUtil.formatDateTime(0, "en"));
        assertNotNull(DateUtil.formatDateTime(System.currentTimeMillis(), "ru"));
        assertNotNull(DateUtil.formatDateTime(System.currentTimeMillis(), "en"));
    }
}
