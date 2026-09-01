package org.examplee.vampirest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Texts {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    public static final String PREFIX = colorize("&#FF0000&lV&#DA0606&la&#B50B0B&lm&#911111&lp&#6C1717&li&#471C1C&lr&#222222&le") + " §8| §7";
    public static final String BLOOD_WORD = colorize("&#FF0000&lК&#F10A0A&lР&#E21414&lО&#D41E1E&lВ&#C62828&lЬ");

    private Texts() {
    }

    public static String colorize(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = "§x"
                    + "§" + hex.charAt(0)
                    + "§" + hex.charAt(1)
                    + "§" + hex.charAt(2)
                    + "§" + hex.charAt(3)
                    + "§" + hex.charAt(4)
                    + "§" + hex.charAt(5);
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString().replace('&', '§');
    }

    public static String prefixed(String message) {
        return PREFIX + colorize(message);
    }
}