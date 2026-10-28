package me.nexuswarp.economy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class EconomyUtilTest {

    @Test
    public void testParseStringToBigDecimal() {
        assertEquals(0, new BigDecimal("100").compareTo(EconomyUtil.parseStringToBigDecimal("100")));
        assertEquals(0, new BigDecimal("10.50").compareTo(EconomyUtil.parseStringToBigDecimal("10.50")));
        assertEquals(0, new BigDecimal("1000").compareTo(EconomyUtil.parseStringToBigDecimal("1k")));
        assertEquals(0, new BigDecimal("1000").compareTo(EconomyUtil.parseStringToBigDecimal("1K")));
        assertEquals(0, new BigDecimal("2500000").compareTo(EconomyUtil.parseStringToBigDecimal("2.5m")));
        assertEquals(0, new BigDecimal("1000000000").compareTo(EconomyUtil.parseStringToBigDecimal("1b")));
        assertEquals(0, new BigDecimal("2000000000000").compareTo(EconomyUtil.parseStringToBigDecimal("2t")));
        assertEquals(0, new BigDecimal("500").compareTo(EconomyUtil.parseStringToBigDecimal("$500")));
        assertEquals(0, new BigDecimal("500").compareTo(EconomyUtil.parseStringToBigDecimal("500$")));

        assertThrows(IllegalArgumentException.class, () -> EconomyUtil.parseStringToBigDecimal(""));
        assertThrows(IllegalArgumentException.class, () -> EconomyUtil.parseStringToBigDecimal("abc"));
        assertThrows(IllegalArgumentException.class, () -> EconomyUtil.parseStringToBigDecimal("100x"));
    }

    @Test
    public void testFormatCurrencyPrefix() {
        assertEquals("$1,000", EconomyUtil.formatCurrency(BigDecimal.valueOf(1000.0), "$", false));
        assertEquals("$10", EconomyUtil.formatCurrency(BigDecimal.valueOf(10.0), "$", false));
        assertEquals("$10.10", EconomyUtil.formatCurrency(BigDecimal.valueOf(10.10), "$", false));
        assertEquals("$10.10", EconomyUtil.formatCurrency(BigDecimal.valueOf(10.1000001), "$", false));
        assertEquals("$10.10", EconomyUtil.formatCurrency(BigDecimal.valueOf(10.1099999), "$", false));
        assertEquals("-$50", EconomyUtil.formatCurrency(BigDecimal.valueOf(-50.0), "$", false));
        assertEquals("$0", EconomyUtil.formatCurrency(BigDecimal.ZERO, "$", false));
    }

    @Test
    public void testFormatCurrencySuffix() {
        assertEquals("1,000$", EconomyUtil.formatCurrency(BigDecimal.valueOf(1000.0), "$", true));
        assertEquals("10$", EconomyUtil.formatCurrency(BigDecimal.valueOf(10.0), "$", true));
        assertEquals("10.10$", EconomyUtil.formatCurrency(BigDecimal.valueOf(10.10), "$", true));
        assertEquals("-50$", EconomyUtil.formatCurrency(BigDecimal.valueOf(-50.0), "$", true));
        assertEquals("500руб", EconomyUtil.formatCurrency(BigDecimal.valueOf(500.0), "руб", true));
    }
}
