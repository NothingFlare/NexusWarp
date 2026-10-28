package me.nexuswarp.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class EconomyUtil {

    public static final BigDecimal THOUSAND = new BigDecimal("1000");
    public static final BigDecimal MILLION = new BigDecimal("1000000");
    public static final BigDecimal BILLION = new BigDecimal("1000000000");
    public static final BigDecimal TRILLION = new BigDecimal("1000000000000");

    private static final NumberFormat PRETTY_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
        PRETTY_FORMAT.setRoundingMode(RoundingMode.FLOOR);
        PRETTY_FORMAT.setGroupingUsed(true);
        PRETTY_FORMAT.setMinimumFractionDigits(2);
        PRETTY_FORMAT.setMaximumFractionDigits(2);
    }

    private EconomyUtil() {}

    /**
     * Parses a string into a BigDecimal, supporting standard number formats
     * and short-hand suffixes: k, m, b, t (case-insensitive).
     *
     * Example: "10k" -> 10000, "2.5m" -> 2500000
     */
    public static BigDecimal parseStringToBigDecimal(String input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be empty");
        }

        // Sanitize string keeping numbers, minus, and dot/comma
        String sanitized = trimmed.replaceAll("[^0-9.,\\-]", "");
        if (sanitized.isEmpty() || sanitized.equals("-")) {
            throw new IllegalArgumentException("Invalid amount format: " + input);
        }

        // Extract any multiplier letters (e.g. k, m, b, t)
        String suffix = trimmed.replaceAll("[^a-zA-Z]", "").toUpperCase(Locale.ROOT);
        BigDecimal multiplier = null;

        switch (suffix) {
            case "":
                break;
            case "K":
                multiplier = THOUSAND;
                break;
            case "M":
                multiplier = MILLION;
                break;
            case "B":
                multiplier = BILLION;
                break;
            case "T":
                multiplier = TRILLION;
                break;
            default:
                throw new IllegalArgumentException("Unknown multiplier suffix: " + suffix);
        }

        NumberFormat format = NumberFormat.getInstance(Locale.US);
        Number parsed;
        try {
            parsed = format.parse(sanitized.replace(",", ""));
        } catch (ParseException e) {
            try {
                parsed = Double.parseDouble(sanitized.replace(",", ""));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid numeric value: " + input);
            }
        }

        BigDecimal amount = new BigDecimal(parsed.toString());
        if (multiplier != null) {
            amount = amount.multiply(multiplier);
        }
        return amount;
    }

    /**
     * Formats currency matching Essentials display rules:
     * - Thousands separator
     * - Two decimal places
     * - Strips trailing .00 if whole number
     * - Configurable currency symbol position (prefix or suffix)
     */
    public static String formatCurrency(BigDecimal value, String currencySymbol, boolean symbolBehind) {
        if (value == null) {
            value = BigDecimal.ZERO;
        }

        String sign = "";
        if (value.signum() < 0) {
            sign = "-";
            value = value.abs();
        }

        synchronized (PRETTY_FORMAT) {
            String str = PRETTY_FORMAT.format(value);
            if (str.endsWith(".00")) {
                str = str.substring(0, str.length() - 3);
            }
            if (symbolBehind) {
                return sign + str + currencySymbol;
            } else {
                return sign + currencySymbol + str;
            }
        }
    }

    public static String formatCurrency(double value, String currencySymbol, boolean symbolBehind) {
        return formatCurrency(BigDecimal.valueOf(value), currencySymbol, symbolBehind);
    }
}
