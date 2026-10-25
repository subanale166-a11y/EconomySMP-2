package com.example.economysmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Util {
    private Util() {}

    public static Component color(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    /** 1500 -> 1.5K, 2000000 -> 2.0M */
    public static String money(double v) {
        String[] suffix = {"", "K", "M", "B", "T", "Q"};
        int i = 0;
        double n = v;
        while (n >= 1000 && i < suffix.length - 1) {
            n /= 1000;
            i++;
        }
        if (i == 0) return String.format("%.2f", n);
        return String.format("%.1f%s", n, suffix[i]);
    }

    public static String time(long seconds) {
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }

    /** Parses 100, 1.5k, 2m, 3b. Returns -1 if invalid. */
    public static double parseAmount(String in) {
        try {
            String s = in.toLowerCase().trim();
            double mult = 1;
            if (s.endsWith("k")) { mult = 1_000; s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("m")) { mult = 1_000_000; s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("b")) { mult = 1_000_000_000; s = s.substring(0, s.length() - 1); }
            double v = Double.parseDouble(s) * mult;
            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) return -1;
            return v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
