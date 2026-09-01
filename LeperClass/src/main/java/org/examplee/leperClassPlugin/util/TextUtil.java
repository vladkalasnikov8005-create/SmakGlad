package org.examplee.leperClassPlugin.util;

import org.bukkit.ChatColor;
import org.bukkit.Color;

public final class TextUtil {
    public static final Color C_GRAY = Color.fromRGB((int)138, (int)138, (int)138);
    public static final Color C_GREEN = Color.fromRGB((int)57, (int)255, (int)20);
    public static final Color C_RED = Color.fromRGB((int)255, (int)43, (int)43);
    public static final Color C_PURPLE = Color.fromRGB((int)176, (int)0, (int)255);
    public static final Color C_BLUE = Color.fromRGB((int)43, (int)229, (int)255);
    public static final String PREFIX = String.valueOf(ChatColor.DARK_GRAY) + "[" + String.valueOf(ChatColor.LIGHT_PURPLE) + "Leper" + String.valueOf(ChatColor.DARK_GRAY) + "] " + String.valueOf(ChatColor.GRAY);

    private TextUtil() {
    }

    public static String gGreenGray(String t) {
        return TextUtil.gradientMulti(t, C_GREEN, C_GRAY);
    }

    public static String gRedGray(String t) {
        return TextUtil.gradientMulti(t, C_RED, C_GRAY);
    }

    public static String gPurpleGray(String t) {
        return TextUtil.gradientMulti(t, C_PURPLE, C_GRAY);
    }

    public static String gPurpleGreen(String t) {
        return TextUtil.gradientMulti(t, C_PURPLE, C_GREEN);
    }

    public static String gBlueGray(String t) {
        return TextUtil.gradientMulti(t, C_BLUE, C_GRAY);
    }

    public static String ui(String msg) {
        return PREFIX + msg;
    }

    public static String loreHint(String msg) {
        return String.valueOf(ChatColor.DARK_GRAY) + "\u2022 " + String.valueOf(ChatColor.GRAY) + msg;
    }

    public static String loreWarn(String msg) {
        return String.valueOf(ChatColor.DARK_RED) + "\u2022 " + String.valueOf(ChatColor.RED) + msg;
    }

    public static String gradientMulti(String text, Color ... colors) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (colors == null || colors.length < 2) {
            return text;
        }
        int len = text.length();
        int segments = colors.length - 1;
        StringBuilder out = new StringBuilder(len * 14);
        for (int i = 0; i < len; ++i) {
            double p = len == 1 ? 0.0 : (double)i / (double)(len - 1);
            double scaled = p * (double)segments;
            int seg = Math.min(segments - 1, Math.max(0, (int)Math.floor(scaled)));
            double t = scaled - (double)seg;
            Color a = colors[seg];
            Color b = colors[seg + 1];
            int r = (int)Math.round((double)a.getRed() + t * (double)(b.getRed() - a.getRed()));
            int g = (int)Math.round((double)a.getGreen() + t * (double)(b.getGreen() - a.getGreen()));
            int bl = (int)Math.round((double)a.getBlue() + t * (double)(b.getBlue() - a.getBlue()));
            out.append(TextUtil.legacyHex(r, g, bl)).append(text.charAt(i));
        }
        return out.toString();
    }

    public static String legacyHex(int r, int g, int b) {
        String hex = String.format("%02x%02x%02x", TextUtil.clamp(r), TextUtil.clamp(g), TextUtil.clamp(b));
        StringBuilder sb = new StringBuilder("\u00a7x");
        for (char c : hex.toCharArray()) {
            sb.append('\u00a7').append(c);
        }
        return sb.toString();
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}

