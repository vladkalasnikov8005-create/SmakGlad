package org.examplee.palePlugin.util;

import org.bukkit.ChatColor;

public final class Msg {
    private Msg() {
    }

    private static String rgb(String hex) {
        try {
            return net.md_5.bungee.api.ChatColor.of((String)hex).toString();
        }
        catch (Throwable t) {
            return ChatColor.GREEN.toString();
        }
    }

    public static String g(String s) {
        return Msg.rgb("#39FF14") + s + String.valueOf(ChatColor.RESET);
    }
}

