package org.examplee.leperClassPlugin.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.examplee.leperClassPlugin.util.TextUtil;

public final class MessageService {
    public void info(CommandSender s, String msg) {
        if (s != null) {
            s.sendMessage(TextUtil.ui(String.valueOf(ChatColor.GRAY) + msg));
        }
    }

    public void ok(CommandSender s, String msg) {
        if (s != null) {
            s.sendMessage(TextUtil.ui(String.valueOf(ChatColor.GREEN) + msg));
        }
    }

    public void warn(CommandSender s, String msg) {
        if (s != null) {
            s.sendMessage(TextUtil.ui(String.valueOf(ChatColor.YELLOW) + msg));
        }
    }

    public void error(CommandSender s, String msg) {
        if (s != null) {
            s.sendMessage(TextUtil.ui(String.valueOf(ChatColor.RED) + msg));
        }
    }
}

